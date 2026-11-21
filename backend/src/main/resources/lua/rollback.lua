-- 回滚 Redis 预扣库存（下单失败 / 订单取消）
-- KEYS[1] = 库存 key
-- KEYS[2] = 已购买用户 Set
-- ARGV[1] = userId
-- ARGV[2] = 数量

if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('INCRBY', KEYS[1], ARGV[2])
end
redis.call('SREM', KEYS[2], ARGV[1])
return 1
