---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by gxh.
--- DateTime: 2023/4/5 21:31
---

--- 首先判断库存
--- 再判断是否下过单
--- 都ok的话，直接扣库存，保存下单用户

local voucherId = ARGV[1]
local userId = ARGV[2]

local socketKey = "seckill:stock:" .. voucherId
local orderKey = "seckill:order" .. voucherId

if( tonumber(redis.call('get',socketKey)) <= 0 ) then
    return 1
end

if(redis.call('sismember',orderKey, userId) == 1) then
    return 2
end

--- 在有库存和未下单后，就可以减库存和保存订单了
redis.call('incrby', socketKey, -1)
redis.call('sadd', orderKey, userId)

return 0