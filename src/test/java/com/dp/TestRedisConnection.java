package com.dp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

@SpringBootTest
class TestRedisConnection {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void test(){
        String gxh = stringRedisTemplate.opsForValue().get("gxh");
        System.out.println(gxh);
    }


    /**
     * 测试生成UUID的api
     */
    @Test
    void test_JDK_UUID(){
        System.out.println("jdkUUID： " + UUID.randomUUID().toString());
        System.out.println("hutoolUUID： " + cn.hutool.core.lang.UUID.randomUUID().toString());
        System.out.println("hutoolUUID： " + cn.hutool.core.lang.UUID.randomUUID().toString(true));
    }

}
