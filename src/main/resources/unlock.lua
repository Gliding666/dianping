---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by gxh.
--- DateTime: 2023/4/5 10:43
---

if(redis.call('get', KEYS[1]) == ARGV[1]) then
    return redis.call('del', KEYS[1])
end
return 0