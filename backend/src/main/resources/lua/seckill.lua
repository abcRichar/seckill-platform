-- 秒杀预扣库存（原子操作）
-- KEYS[1] = 库存 key
-- KEYS[2] = 已购买用户 Set
-- ARGV[1] = userId
-- ARGV[2] = 购买数量
-- 返回：>=0 剩余库存；-1 售罄；-2 重复购买；-3 库存未初始化

if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -2
end

local stock = redis.call('GET', KEYS[1])
if stock == false then
    return -3
end

stock = tonumber(stock)
local qty = tonumber(ARGV[2])
if stock < qty then
    return -1
end

redis.call('DECRBY', KEYS[1], qty)
redis.call('SADD', KEYS[2], ARGV[1])
return stock - qty
