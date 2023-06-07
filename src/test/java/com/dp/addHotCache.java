package com.dp;

import cn.hutool.json.JSONUtil;
import com.dp.service.impl.ShopServiceImpl;
import com.dp.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;

import static com.dp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
public class addHotCache {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopServiceImpl shopService;

    @Test
    public void add(){
        RedisData redisData = new RedisData();
        redisData.setData(shopService.getById(1));
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(10));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + 1, JSONUtil.toJsonStr(redisData));
    }

}
