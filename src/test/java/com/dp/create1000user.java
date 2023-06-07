package com.dp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.dp.utils.RedisConstants.LOGIN_USER_TTL;

@SpringBootTest
class create1000user {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void main() throws IOException {

        PrintWriter pw =  new PrintWriter(  new FileWriter(  "D:\\token.txt"  ) );
        String phone = "13812345678";

        for(int i = 0; i < 1000; i ++ ) {
            phone = String.valueOf(Long.parseLong(phone) + i);
            // 创建新用户并保存
            User user = userService.query().eq("phone", phone).one();
            // 7.1 随机生成token，作为登录令牌
            String token = UUID.randomUUID().toString(true);
            pw.println(token);
            // 7.2 将User对象转为HashMap存储
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class); //过滤一些属性
            // TODO 学习bean变成map时，转换属性的数据类型
            Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create()
                            .setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldNmae, fieldValue) -> fieldValue.toString()));

            // 7.3 存储
            String tokenKey  = LOGIN_USER_KEY + token ;
            stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
            stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
    }

    @Test
    void test() throws IOException {
        PrintWriter pw =  new PrintWriter( new FileWriter(  "D:\\token.txt") );
        String phone = "13812345678";
        for(int i=0;i<1000;i++){
            System.out.println(String.valueOf(Long.parseLong(phone) + i));
            pw.println(String.valueOf(Long.parseLong(phone) + i));
        }

    }
}
