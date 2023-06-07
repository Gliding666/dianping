package com.dp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.dp.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        System.out.println(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // 缓存穿透
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFailback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;
        // 1. 在redis里查询
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 有数据,直接返回
        if( StrUtil.isNotBlank(json) ){
            return JSONUtil.toBean(json, type);
        }
        // shopJson != null 说明 shopJson 为 “” ，即说明数据库不存在数据，直接返回
        if (json != null) {
            return null;
        }
        // 3. 无数据，在数据库中查询
        R r = dbFailback.apply(id);
        // 3.1 数据库中无数据
        if(r == null){
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 3.2 数据库中有数据，存入redis中，返回
        this.set(key, JSONUtil.toJsonStr(r), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return r;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    // 逻辑过期解决缓存击穿 不用考虑缓存穿透的情况，因为所有数据都放到redis预热了，一旦缓存查询出null的，说明数据库没有这个数据
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFailback, long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1. 在redis里查询
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2. 判断是否存在
        if( StrUtil.isBlank(json) ){
            // 3. 不存在,直接返回
            return null;
        }

        // 4. 命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);

        // 5. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1 未过期，直接放回店铺信息
            System.out.println(expireTime);
            System.out.println(LocalDateTime.now());
            return r;
        }

        // 5.2 已过期，需要缓存重建
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        // 6.1 判断是否成功获取到锁
        if(isLock) {
            // 6.2 获取到锁，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                //重建缓存
                try {
                    // 先查数据库
                    R r1 = dbFailback.apply(id);
                    // 再写入 redis
                    setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unLock(lockKey);
                }
            });
        }

        // 6. 没获取到锁， 锁被别的线程用了， 那么就返回过期数据，
        return r;
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

}
