# 从项目里学什么、怎么学

建议顺序：先跑通 → 再按模块读代码 → 最后动手改造。每读一个点，问自己三件事：

1. **为什么这么设计？** 不用它会怎样？
2. **会出什么问题？** 并发、异常、网络抖动、重复请求。
3. **线上怎么排查？** 看哪个日志、哪个 Redis key、哪张表。

---

## 阶段一：工程化 CRUD 闭环

### 1. 统一响应 + 全局异常

- 代码：`common/result/R.java`、`ResultCode.java`、`common/exception/*`
- 要能讲：
  - 为什么用 HTTP 状态码表达语义（2xx/4xx/5xx），body code 保留业务含义？
  - 409（售罄/重复购买）、429（限流）分别对应什么业务场景？
  - 前端如何统一处理 401 跳登录、403 只提示、Blob 下载错误？
  - 参数校验异常、业务异常、系统异常怎么分流？
  - 为什么不能把堆栈直接返回给前端？
- 动手：
  - 增加一个 `@ExceptionHandler` 处理 `AccessDeniedException`；
  - 给 `R` 增加 `traceId` 字段，前端报错时带上。

### 2. JWT 鉴权 + 拦截器

- 代码：`common/util/JwtUtil.java`、`common/interceptor/AuthInterceptor.java`、`auth/*`
- 要能讲：
  - JWT 三段结构、签名与过期时间；
  - 为什么 token 里不放权限列表？
  - 拦截器与过滤器的区别；
  - 401 / 403 的语义区别。
- 坑：
  - 密钥太短会抛 `WeakKeyException`；
  - ThreadLocal 用完必须 `remove()`，否则线程池串号；
  - 无状态 token 无法主动失效。
- 动手：
  - 增加 Redis 黑名单，实现退出登录立即失效；
  - 增加 token 版本号，改密码后旧 token 全部失效。

### 3. RBAC 五表

- 代码：`schema.sql`、`SysUserService.getPermissions`、`AdminRoleController`
- 要能讲：
  - 用户-角色-权限为什么拆五张表？
  - 一条 SQL 查用户所有权限码；
  - 权限缓存 10 分钟 + 主动失效；
  - 前端菜单/按钮如何根据权限码显隐（只做展示，后端必须再校验）。
- 动手：
  - 新建一个「运营专员」角色，只给 `product:list` 和 `seckill:list`，验证写接口返回 403；
  - 给 `@RequirePerm` 增加「或」语义（满足任意一个即可）。

### 4. 分页 / 参数校验 / 文件上传

- 分页：`PageResult`、MyBatis-Plus `PaginationInnerInterceptor`（物理分页）。
- 校验：`@Valid` + DTO 注解；分组校验（Create/Update）可以再扩展。
- 上传：扩展名白名单、随机文件名、按日期分目录、大小限制。
- 坑：
  - 深分页 `LIMIT 100000, 10` 会越翻越慢；
  - 上传不要拼接原始文件名，避免路径穿越；
  - 事务方法里不要做耗时 IO（上传、发消息）。

### 5. AOP 操作日志

- 代码：`@OpLog`、`common/aspect/OpLogAspect.java`
- 要能讲：
  - 切点、通知类型（Around）；
  - 为什么日志异步落库？
  - 参数序列化要过滤 `MultipartFile`、`Request/Response`；
  - 日志脱敏（手机号、密码）该怎么做。
- 动手：
  - 给日志加 traceId；
  - 对密码字段做脱敏后再序列化。

---

## 阶段二：秒杀与高并发

### 6. Redis 缓存三件套

- 代码：`product/service/ProductService.java`
- 重点：穿透（空值缓存）、击穿（互斥锁 + double check）、雪崩（随机 TTL）。
- 要能讲：
  - 为什么用 `SETNX`/Redisson 而不是 `synchronized`？（多实例）
  - 空值缓存 TTL 为什么不能太长？
  - 删除缓存还是更新缓存？先删缓存还是先更 DB？
- 动手：
  - 用 JMeter 压商品详情，观察缓存命中前后 DB 查询量；
  - 加一个布隆过滤器挡不存在的 id。

### 7. Lua 预扣库存

- 代码：`resources/lua/seckill.lua`、`seckill/service/SeckillStockService.java`
- 要能讲：
  - 为什么必须用 Lua？`DECR` + 判断为什么不行？
  - Redis 单线程模型与 Lua 原子性；
  - 返回码设计（售罄 / 重复 / 未初始化）。
- 动手：
  - 去掉 `SISMEMBER`，观察一人多单是否被 DB 唯一索引拦住；
  - 把库存 key 删掉，观察 `-3` 分支。

### 8. Redis Stream 异步下单

- 代码：`SeckillService.sendOrderMessage`、`OrderStreamConsumer`
- 要能讲：
  - 为什么异步？接口 RT 与削峰；
  - 消费组、ACK、pending 的含义；
  - 消息重复消费如何幂等（orderNo 查询 + 唯一索引）；
  - 失败重试与毒消息处理。
- 坑：
  - Stream 没有消费组时 `XREADGROUP` 会报 NOGROUP；
  - 不 ACK 会一直留在 pending；
  - Stream 无限增长，需要 MAXLEN 或定期清理。
- 动手：
  - 把消费者停掉，再下单，观察 pending；
  - 重启消费者，验证订单补落库；
  - 换成 RabbitMQ/RocketMQ 对比实现。

### 9. 订单状态机与幂等

- 代码：`constant/OrderStatus.java`、`SeckillOrderService`
- 要能讲：
  - 为什么用 `UPDATE ... WHERE status=旧状态` 而不是先查再改？
  - 支付回调重复到达怎么幂等？
  - 取消与支付并发时谁赢？
- 动手：
  - 并发调用两次支付接口，验证销量只加一次；
  - 取消一笔刚支付的订单，验证被拒绝。

### 10. 超时关单

- 代码：`job/ScheduledTasks.java`、`SeckillOrderService.cancelIfUnpaid`
- 要能讲：
  - 延迟队列（ZSET）与定时扫描的取舍；
  - 为什么还要 DB 兜底？
  - 关闭订单时如何避免和支付并发？
  - 库存回补为什么放在事务提交后？
- 动手：
  - 把超时时间改成 1 分钟，观察自动取消与库存回补；
  - 模拟 Redis 丢消息，验证 DB 兜底。

### 11. 限流 / 隐藏链接 / 令牌

- 代码：`@RateLimit`、`RateLimitAspect`、`SeckillActivityService.publish`、`SeckillService.getToken`
- 要能讲：
  - 固定窗口 vs 滑动窗口 vs 令牌桶；
  - 令牌为什么要一次性、短有效期；
  - 隐藏链接能挡住什么、挡不住什么。
- 动手：
  - 10 秒内连续请求 token，观察 429；
  - 用过期 token 下单，观察失败。

### 12. Excel 导出

- 代码：`OrderExportVO`、`AdminOrderController.export`
- 要能讲：
  - EasyExcel 与 POI 的区别（流式、内存占用）；
  - 导出大数据量为什么要异步 + 分片；
  - 中文文件名编码。
- 动手：
  - 导出 5000 条订单，观察内存；
  - 改成异步导出，前端轮询下载。

---

## 推荐练习顺序

1. 跑通项目，用演示账号走一遍完整抢购。
2. 按「阶段一」清单逐个读代码，画一张请求链路图。
3. 用 `scripts/seckill-stress.py` 压测，记录 QPS、RT、库存。
4. 按「阶段二」清单挑 3 个点做改造，写成博客：
   - 现象 → 定位 → 根因 → 方案 → 验证数据。
5. 准备面试表达：
   - 3 分钟讲清项目架构与秒杀链路；
   - 5 分钟讲清一个你踩过的坑（附日志/监控截图）。

### 13. 退款审核状态机

- 代码：`seckill/service/RefundService.java`、`mapper/SeckillRefundMapper.java`、`mapper/SeckillOrderMapper.java`、`seed/SchemaUpgradeRunner.java`
- 要能讲：
  - 退款和取消订单的区别（是否回补库存、允许的订单状态）；
  - 为什么退款单要单独建表，而不是只在订单上加字段；
  - 双条件更新（退款单 status=0、订单 refund_status=1）如何保证并发幂等；
  - 拒绝后为什么允许复用退款单重新申请；
  - 旧库字段升级怎么做（information_schema 检查 + ALTER）。
- 动手：
  - 并发调用两次审核通过，观察只有一次成功；
  - 把退款改成"退款并回补库存"，思考需要哪些库存流水/幂等保护；
  - 增加"退款原因分类统计"报表。
