package com.dp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.LoginFormDTO;
import com.dp.dto.Result;
import com.dp.dto.UserDTO;
import com.dp.entity.User;
import com.dp.mapper.UserMapper;
import com.dp.service.IUserService;
import com.dp.utils.RegexUtils;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.*;
import static com.dp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        boolean invalid = RegexUtils.isPhoneInvalid(phone);
        if(invalid){ //手机号码不合法
            return Result.fail("手机号码格式错误");
        }
        // 手机号码合法，生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 存入redis ： key: login:code:'电话号码' value :code
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code);
        log.info("登录验证码：" + code);
        return Result.ok("ok");
    }

    @Override
    public Result login(LoginFormDTO loginFormDTO) {
        String phone = loginFormDTO.getPhone();
        String code = loginFormDTO.getCode();
        String password = loginFormDTO.getPassword();

        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        if(cacheCode == null || !cacheCode.equals(cacheCode)){
            // 不一致，返回验证码错误
            return Result.fail("验证码错误");
        }

        User user = lambdaQuery().eq(User::getPhone, phone).one();

        if(user == null) user = createUserWithPhone(phone);

        // 生成token

        String token =UUID.randomUUID().toString();

        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((key, value) -> value.toString())
        );

        //将token存入redis
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, map);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.HOURS);
        return Result.ok(token);
    }

    @Override
    public Result logout(HttpServletRequest request) {
        String token = request.getHeader("authorization");
        Boolean isSuccess = stringRedisTemplate.delete(LOGIN_USER_KEY + token);
        return Result.ok(isSuccess);
    }

    @Override
    public Result sign() {
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        String key = "sign:" + userId.toString() + keySuffix;
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 写入 Redis SETBIT key offset 1
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        Long userId = UserHolder.getUser().getId();
        // 获取日期
        LocalDateTime now = LocalDateTime.now();

        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));

        String key = "sign:" + userId.toString() + keySuffix;
        // 获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 获取本月截止今天为止的所有签到记录，返回的是一个十进制的数字
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key, BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                        .valueAt(0)
        );

        if(result == null || result.isEmpty()) {
            return Result.ok();
        }
        Long num = result.get(0);
        if(num == null || num == 0) {
            return Result.ok();
        }
        int ans = 0;
        while(num >  0) {
            if((num & 1) == 1){
                ans ++ ;
                num >>>= 1;
            }
            else break;
        }

        return Result.ok(ans);
    }

    public Result me(){
        return Result.ok();
    }

    public User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
