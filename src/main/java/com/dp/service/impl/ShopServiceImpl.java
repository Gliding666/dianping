package com.dp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.Shop;
import com.dp.mapper.ShopMapper;
import com.dp.service.IShopService;
import com.dp.utils.RedisData;
import com.dp.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopByType(Integer typeId, Integer current) {

        System.out.println(typeId + "   " + current);

        Page<Shop> page = query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));

        return Result.ok(page.getRecords());
    }

    @Override
    public Result queryShopById(Long id) {

        //Shop shop =  queryWithPassThrough(id);

        //Shop shop = queryWithMutex(id);
        Shop shop = queryWithLogicExpire(id);

        if(shop == null) Result.fail("店铺不存在！");

        return Result.ok(shop);
    }

    /**
     * 缓存穿透实现商铺查询
     * @param id
     * @return
     */
    private Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        Shop shop;
        if(StrUtil.isNotBlank(json)){
            shop = JSONUtil.toBean(json, Shop.class);
            return shop;
        }
        if(json != null) { //说明缓存的value是 “”
            return null;
        }
        //shop = query().eq("id", id).one();
        shop = getById(id);
        if(shop == null){
            stringRedisTemplate.opsForValue().set(key, "");
            return null;
        }
        String value = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key, value);
        stringRedisTemplate.expire(key, CACHE_SHOP_TTL, TimeUnit.DAYS);

        return shop;
    }

    /**
     * 增加互斥锁，解决热点key失效问题
     * @param id
     * @return
     */
    private Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        Shop shop = null;
        if(StrUtil.isNotBlank(json)){ //查询到缓存，直接返回数据
            shop = JSONUtil.toBean(json, Shop.class);
            return shop;
        }
        if(json != null) { //说明缓存的value是 “”
            return null;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        try {
            boolean isLock = getLock(lockKey);
            if(! isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 获取到锁后需要二次查询缓存
            String json2 = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(json)){ //查询到缓存，直接返回数据
                shop = JSONUtil.toBean(json2, Shop.class);
                return shop;
            }
            if(json2 != null) { //说明缓存的value是 “”
                return null;
            }

            //查询数据库 实现缓存重建
            // 模拟重建的延时
            Thread.sleep(200);
            //shop = query().eq("id", id).one();
            shop = getById(id);
            if(shop == null){
                stringRedisTemplate.opsForValue().set(key, "");
                return null;
            }
            String value = JSONUtil.toJsonStr(shop);
            stringRedisTemplate.opsForValue().set(key, value);
            stringRedisTemplate.expire(key, CACHE_SHOP_TTL, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            delLock(lockKey);
        }
        return shop;
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 利用逻辑过期解决缓存击穿
     * @param id
     * @return
     */
    private Shop queryWithLogicExpire(Long id){
        String key = CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json)){
            return null;
        }

        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        if(redisData.getExpireTime().isAfter(LocalDateTime.now())){ // 过期时间在现在时间后面未过期
            return shop;
        }
        // 确定缓存过期，查询数据库重建缓存

        String lockKey = LOCK_SHOP_KEY + id;
        boolean lock = getLock(lockKey);
        if(lock){ // 获取到锁，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    Shop newShop = getById(id);
                    try {
                        Thread.sleep(200); //模拟缓存重建延迟
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    RedisData newRedisData = new RedisData();
                    newRedisData.setData(newShop);
                    // 先设置个10s,让缓存过期,便于我们观察缓存重建是否会出现线程安全问题
                    newRedisData.setExpireTime(LocalDateTime.now().plusSeconds(10));

                    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(newRedisData));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    delLock(lockKey);
                }
            });
        }
        // 没获取到锁，暂时返回过期数据
        return shop;
    }



    private boolean getLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1",LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void delLock(String key) {
        stringRedisTemplate.delete(key);
    }


}
