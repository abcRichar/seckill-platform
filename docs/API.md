# API 速查

- Base URL：`http://localhost:9090`
- 统一响应：`{ "code": 200, "message": "ok", "data": ..., "timestamp": ... }`（`code` 与 HTTP 状态码一致，`bizCode` 为可选业务细分码）
- 鉴权头：`Authorization: Bearer <token>`
- Swagger：`http://localhost:9090/swagger-ui.html`

## HTTP 状态码约定

| HTTP | 场景 | body code | bizCode |
|---|---|---|---|
| 200 | 成功 | 200 | - |
| 400 | 参数校验失败 / 业务校验失败 | 400 | - |
| 401 | 未登录 / token 失效 | 401 | - |
| 403 | 无权限 | 403 | - |
| 404 | 资源不存在 | 404 | - |
| 405 | 请求方法不支持 | 405 | - |
| 409 | 售罄 / 重复购买 / 活动结束 | 409 | 1001 / 1002 / 1003 |
| 409 | 唯一约束冲突 | 409 | - |
| 413 | 上传文件过大 | 413 | - |
| 429 | 触发限流 | 429 | - |
| 500 | 服务端未预期异常 | 500 | - |

```json
// 成功
{ "code": 200, "message": "ok", "data": { }, "timestamp": 1795245187419 }
// 重复购买
{ "code": 409, "bizCode": 1002, "message": "您已参与过该活动", "data": null, "timestamp": 1795245187419 }
```

前端 axios 对非 2xx 统一进入错误分支：读取 body 的 `message` 提示，
`code=401` 清理 token 并跳登录页，其余 4xx/5xx 只提示不跳转。

## 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/auth/admin/login` | 管理端登录（仅 ADMIN） |
| POST | `/api/auth/login` | 用户端登录 |
| POST | `/api/auth/register` | 用户注册（自动登录） |

## 用户端公开接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/mall/categories` | 分类列表 |
| GET | `/api/mall/products` | 商品分页（categoryId/keyword/sort） |
| GET | `/api/mall/products/{id}` | 商品详情（缓存） |
| GET | `/api/mall/seckill/list` | 秒杀活动列表 |
| GET | `/api/mall/seckill/{id}` | 秒杀详情（含隐藏 path） |

## 用户端登录接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/mall/seckill/{id}/token?path=` | 获取一次性令牌（限流） |
| POST | `/api/mall/seckill/order` | 秒杀下单（限流） |
| GET | `/api/mall/seckill/result?orderNo=` | 轮询异步下单结果 |
| GET | `/api/mall/seckill/{id}/mine` | 当前用户是否已参与 |
| GET | `/api/mall/orders` | 我的订单（status/page/size） |
| GET | `/api/mall/orders/{id}` | 订单详情 |
| POST | `/api/mall/orders/{id}/pay` | 模拟支付 |
| POST | `/api/mall/orders/{id}/cancel` | 取消订单 |
| POST | `/api/mall/orders/{id}/refund` | 申请退款（已支付/已完成） |
| GET | `/api/mall/orders/{id}/refund` | 查看该订单退款单 |
| GET | `/api/mall/user/me` | 当前用户信息 |
| PUT | `/api/mall/user/profile` | 修改资料 |
| PUT | `/api/mall/user/password` | 修改密码 |

下单请求体：

```json
{
  "activityId": 1,
  "token": "获取令牌接口返回的 token",
  "path": "秒杀详情返回的 seckillPath",
  "quantity": 1
}
```

轮询结果：

```json
{ "orderNo": "SK...", "status": "QUEUED|SUCCESS|FAILED", "orderId": 1, "orderStatus": 0, "message": "" }
```

申请退款：

```json
POST /api/mall/orders/1/refund
{ "reason": "临时不需要了", "description": "行程有变" }
```

审核退款：

```json
POST /api/admin/refunds/1/approve   { "remark": "同意退款，原路退回" }
POST /api/admin/refunds/1/reject    { "remark": "未满足退款条件" }
POST /api/admin/orders/1/refund     { "reason": "客服补偿退款", "description": "物流异常" }
```

退款状态：

- 订单 `refund_status`：0 无退款 / 1 审核中 / 2 已退款 / 3 被拒
- 订单 `status=4`：已退款
- 退款单 `status`：0 待审核 / 1 已通过 / 2 已拒绝

## 管理端 `/api/admin`（需 ADMIN + 对应权限）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/admin/auth/me` | - | 当前管理员 + 权限码 |
| GET | `/api/admin/dashboard` | dashboard:view | 工作台统计 |
| GET | `/api/admin/products` | product:list | 商品分页 |
| POST | `/api/admin/products` | product:edit | 新增商品 |
| PUT | `/api/admin/products/{id}` | product:edit | 修改商品 |
| PUT | `/api/admin/products/{id}/status` | product:edit | 上下架 |
| DELETE | `/api/admin/products/{id}` | product:edit | 删除商品 |
| POST | `/api/admin/upload` | product:edit | 上传图片 |
| GET | `/api/admin/categories` | category:manage | 分类列表 |
| POST/PUT/DELETE | `/api/admin/categories...` | category:manage | 分类维护 |
| GET | `/api/admin/seckill` | seckill:list | 活动分页 |
| POST/PUT/DELETE | `/api/admin/seckill...` | seckill:edit | 活动维护 |
| PUT | `/api/admin/seckill/{id}/publish` | seckill:edit | 上架 + 初始化 Redis |
| PUT | `/api/admin/seckill/{id}/stop` | seckill:edit | 下架 + 清理 Redis |
| GET | `/api/admin/orders` | order:list | 订单分页 |
| POST | `/api/admin/orders/{id}/cancel` | order:edit | 取消订单 |
| POST | `/api/admin/orders/{id}/refund` | refund:audit | 管理员直接退款 |
| GET | `/api/admin/orders/export` | order:export | 导出 Excel |
| GET | `/api/admin/refunds` | refund:list | 退款单分页 |
| GET | `/api/admin/refunds/summary` | refund:list | 退款统计 |
| GET | `/api/admin/refunds/{id}` | refund:list | 退款单详情 |
| POST | `/api/admin/refunds/{id}/approve` | refund:audit | 审核通过 |
| POST | `/api/admin/refunds/{id}/reject` | refund:audit | 审核拒绝 |
| GET | `/api/admin/users` | user:list | 用户分页 |
| PUT | `/api/admin/users/{id}/status` | user:edit | 启用/禁用 |
| PUT | `/api/admin/users/{id}/reset-password` | user:edit | 重置密码 |
| PUT | `/api/admin/users/{id}/roles` | user:edit | 分配角色 |
| GET | `/api/admin/roles` | role:manage | 角色列表 |
| GET | `/api/admin/roles/menus` | role:manage | 权限点 |
| PUT | `/api/admin/roles/{id}/menus` | role:manage | 分配权限 |
| GET | `/api/admin/logs` | log:list | 操作日志 |

## 业务细分码 bizCode

以下值只在 `HTTP 409 + code 409` 时出现：

| bizCode | 含义 |
|---|---|
| 1001 | 秒杀售罄 |
| 1002 | 重复购买 |
| 1003 | 活动未开始/已结束 |
