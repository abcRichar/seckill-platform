# 秒杀链路设计与取舍

## 1. 为什么不能直接同步下单

朴素实现：

```java
@Transactional
public void seckill(Long activityId, Long userId) {
    Activity a = activityMapper.selectById(activityId);
    if (a.getStock() <= 0) throw new RuntimeException("售罄");
    activityMapper.updateStock(activityId, a.getStock() - 1);
    orderMapper.insert(order);
}
```

问题：

1. **超卖**：先查后改，两个线程可能读到同一份库存。
2. **数据库被打爆**：秒杀 QPS 通常是日常的几十上百倍，事务、行锁、连接池都扛不住。
3. **用户等待**：同步写订单、扣库存，接口 RT 高。

本项目的处理顺序是：**Redis 挡在前面，数据库只处理真正成功的请求**。

---

## 2. 完整时序

```
Client                Backend                         Redis                    MySQL                Consumer
  │  POST /token        │                              │                        │                     │
  ├────────────────────►│ 校验活动/隐藏path/已购        │                        │                     │
  │                     ├─────────────────────────────►│ SET token 60s          │                     │
  │◄────────────────────┤ token                        │                        │                     │
  │                     │                              │                        │                     │
  │  POST /order        │                              │                        │                     │
  ├────────────────────►│ 校验 token（删）              │                        │                     │
  │                     ├─────────────────────────────►│ EVAL seckill.lua       │                     │
  │                     │                              │ SISMEMBER / GET /      │                     │
  │                     │                              │ DECRBY / SADD          │                     │
  │                     │◄─────────────────────────────┤ 剩余库存               │                     │
  │                     ├─────────────────────────────►│ XADD stream            │                     │
  │◄────────────────────┤ { orderNo, QUEUED }          │                        │                     │
  │                     │                              │                        │                     │
  │                     │                              │        XREADGROUP      │                     │
  │                     │                              │◄───────────────────────┤                     │
  │                     │                              │                        │ UPDATE stock-1      │
  │                     │                              │                        │ INSERT order        │
  │                     │                              │                        │◄────────────────────┤
  │                     │                              │◄───────────────────────┤ XACK                │
  │                     │                              │ ZADD close_queue       │                     │
  │  GET /result        │                              │                        │                     │
  ├────────────────────►│ SELECT order by no           │                        │                     │
  │◄────────────────────┤ SUCCESS + orderId            │                        │                     │
```

---

## 3. Lua 脚本逐行解释

`lua/seckill.lua`：

```lua
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2                -- 已购买：一人一单
end

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -3                -- 库存 key 未初始化
end

stock = tonumber(stock)
if stock < tonumber(ARGV[2]) then
    return -1                -- 售罄
end

redis.call('DECRBY', KEYS[1], ARGV[2])
redis.call('SADD', KEYS[2], ARGV[1])
return stock - tonumber(ARGV[2])
```

要点：

- Redis 单线程执行 Lua，整个脚本是原子的，不会出现两个请求同时通过库存判断。
- **不要**用 `DECR` 后再判断 `< 0` 再 `INCR` 回补，那样在并发下依然会短暂多卖，且回补逻辑复杂。
- 已购用户集合和库存扣减在同一个原子操作里，避免"扣了库存但没记录用户"。
- 返回剩余库存可用于前端展示或调试。

`lua/rollback.lua`：

```lua
if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('INCRBY', KEYS[1], ARGV[2])
end
redis.call('SREM', KEYS[2], ARGV[1])
```

- 只在库存 key 存在时回补，避免活动已结束、key 被清理后又"凭空"创建库存。
- 同时移除用户，允许其重新获取令牌（如果业务允许）。

---

## 4. 为什么 Redis 扣了，数据库还要再扣

Redis 是并发入口，数据库是最终事实源。两者必须都扣：

- **Redis**：挡住 99% 的无效流量，并做一人一单。
- **DB**：`UPDATE seckill_activity SET stock = stock - ? WHERE id = ? AND stock >= ?`
  条件更新天然防超卖，即使 Redis 和 DB 短暂不一致，也不会卖成负数。
- **唯一索引** `uk_user_activity (user_id, activity_id)`：并发重复下单的最后一道防线。

当 DB 扣减失败（例如管理员手工改过库存）时，消费者回滚 Redis 并记录失败原因，前端轮询会得到 `FAILED`。

---

## 5. Stream 消费可靠性

- 消费组：`sk:seckill:order:group`，消费者 `consumer-1`。
- 正常：处理成功 → `XACK`。
- 可预期失败（库存不足、重复购买）：回滚 Redis → 写失败标记 → `XACK`，避免无限重试。
- 不可预期失败（数据库抖动）：**不 ACK**，消息留在 pending。
- `retryPending()` 每 60 秒读取本消费者的 pending 消息重试；单条消息最多重试 5 次，超过后回滚并 ACK，防止毒消息阻塞队列。

生产建议：

- 多实例消费（不同 consumer name）
- 死信队列 / 人工补偿
- 监控 `XPENDING` 数量与消费者 lag

---

## 6. 订单超时关闭

两条路径并行：

1. **ZSET 延迟队列**：下单事务提交后 `ZADD sk:seckill:order:close score=到期时间 member=orderNo`，每 10 秒 `ZRANGEBYSCORE` 取出到期订单。
2. **DB 兜底**：每 90 秒扫描 `status=0 AND create_time < NOW()-15min`，防止 Redis 丢消息/Down 机。

关闭逻辑必须是**条件更新**：

```sql
UPDATE seckill_order SET status=2, cancel_time=NOW()
WHERE order_no=? AND status=0
```

只有影响行数为 1 才回补库存，避免"用户刚支付成功，定时任务又取消"的并发问题。回补包含：

- `UPDATE seckill_activity SET stock=stock+?`
- Redis `INCRBY` 库存 + `SREM` 用户

Redis 回补放在事务提交之后执行，避免 DB 回滚但 Redis 已加库存。

---

## 7. 支付幂等

```sql
UPDATE seckill_order SET status=1, pay_time=NOW() WHERE id=? AND status=0
```

- 第一次：影响 1 行 → 增加商品销量。
- 重复回调：影响 0 行 → 查询当前状态，已是已支付则直接返回成功。
- 已取消订单：拒绝支付。

这就是支付回调最常见的幂等写法，不依赖分布式锁。

---

## 8. 缓存三大问题

| 问题 | 现象 | 本项目方案 | 代码 |
|---|---|---|---|
| 穿透 | 查询不存在的 id，请求全打 DB | 缓存空值 60 秒 | `ProductService.loadFromDb` |
| 击穿 | 热点 key 过期瞬间大量请求回源 | Redisson 分布式锁 + double check，只放一个线程回源 | `ProductService.detailMall` |
| 雪崩 | 同一批 key 同时过期 | TTL 加 0~300 秒随机值 | `ProductService.detailMall` |

注意：缓存空值要设置较短的 TTL，并在数据新增/修改时主动删除对应 key。

---

## 9. 限流

`@RateLimit(key = "seckill:order", count = 2, seconds = 5)`

- Redis `INCR` + `EXPIRE`，按用户维度（未登录按 IP）。
- 固定窗口实现简单，但窗口边界可能放过 2 倍流量。
- 生产可换成滑动窗口（ZSET）或 Redisson `RRateLimiter`（令牌桶）。

秒杀令牌接口和下单接口都有限流，防止脚本刷接口。

---

## 10. 隐藏链接 + 令牌

- 活动上架时生成随机 `seckill_path`，详情页在活动进行中才返回。
- 下单前先换一次性 token（60 秒有效），下单接口校验并立刻删除 token。
- 这样即使有人拿到接口地址，也不能绕过页面直接刷单；令牌过期/重放会失败。
- 注意：这只能提高刷单成本，不能替代风控（设备指纹、验证码、黑名单）。

---

## 11. 一致性与故障矩阵

| 故障点 | 后果 | 兜底 |
|---|---|---|
| Redis 预扣成功，XADD 失败 | 库存少了但没订单 | 立即执行 rollback.lua |
| 消费者落库失败（DB 抖动） | 消息 pending | 60s 后重试，最多 5 次 |
| DB 扣库存失败 | 订单未创建 | 回滚 Redis，写失败标记 |
| 订单取消后 Redis 回补失败 | Redis 库存偏少 | 记录 error 日志；可加对账任务 |
| 应用重启 | Redis 库存/Stream 不丢 | 启动预热 + 消费组 pending 重试 |
| Redis 重启丢数据 | 库存 key 丢失 | 启动预热从 DB 重建；已购集合丢失时由 DB 唯一索引兜底 |

---

## 12. 可以继续优化的方向

1. 库存对账定时任务：对比 DB 剩余库存与 Redis 库存，差异告警。
2. 活动开始前预热的定时任务（而不是仅启动时）。
3. 秒杀预约、开抢提醒。
4. 多消费者 + 死信队列。
5. Redis 集群 / 主从，避免单点。
6. 网关层限流（Sentinel / Nginx）。
7. 压测与火焰图调优记录。

---

## 13. 退款流程设计

退款和"取消未支付订单"是两条不同的链路：

- 取消：仅 `status=0`，回补 DB + Redis 库存。
- 退款：仅 `status IN (1,3)`，只退金额，不回补库存（商品已售出）。

订单新增两个字段：

- `refund_status`：0 无退款 / 1 退款审核中 / 2 已退款 / 3 退款被拒
- `refund_time`：退款完成时间

退款单 `seckill_refund` 独立存审核信息（原因、说明、审核人、审核时间、审核备注、退款时间），一个订单一条退款单（`UNIQUE(order_id)`）。

```
用户申请
  │ 订单 status IN (1,3) 且 refund_status IN (0,3)
  ▼
UPDATE seckill_order SET refund_status=1 WHERE ...（条件更新，防并发重复申请）
  │
  ├─ 无退款单 → INSERT status=0 待审核
  └─ 有被拒退款单 → 复用并清空上次审核信息，status=0
  ▼
管理员审核
  ├─ 通过：
  │   UPDATE seckill_refund SET status=1, audit_* WHERE id=? AND status=0
  │   UPDATE seckill_order SET status=4, refund_status=2, refund_time=NOW()
  │       WHERE id=? AND status IN (1,3) AND refund_status=1
  │   任一步影响行数为 0 → 事务回滚
  └─ 拒绝：
      UPDATE seckill_refund SET status=2, audit_* WHERE id=? AND status=0
      UPDATE seckill_order SET refund_status=3 WHERE id=? AND refund_status=1
      订单保持已支付，用户可修改原因重新申请

管理员直接退款（订单页）：
  UPDATE seckill_order SET status=4, refund_status=2, refund_time=NOW()
  WHERE id=? AND status IN (1,3) AND refund_status IN (0,3)
  同时写入一条 status=1 的退款单，审核人记为当前管理员
```

关键点：

1. **并发幂等**：退款单和订单都使用"条件更新 + 判断影响行数"，两个管理员同时点通过只有一个能成功。
2. **不让状态倒退**：订单状态 4（已退款）不是合法退款起点，也不能再支付/申请退款。
3. **审核留痕**：审核人、时间、备注保存在退款单，用户端可见拒绝原因。
4. **库存策略**：退款不调用 `restoreStock`/Lua 回补，避免已售商品重新进入秒杀池；真实退货入库应另建库存流水。
5. **兼容旧库**：`SchemaUpgradeRunner` 在启动时检查字段/权限点并补齐，不需要手动删库。
