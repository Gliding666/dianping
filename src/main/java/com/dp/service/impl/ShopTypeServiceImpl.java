package com.dp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.ShopType;
import com.dp.mapper.ShopTypeMapper;
import com.dp.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.dp.utils.RedisConstants.CACHE_SHOP_TTL;

@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /*
    将首页的商铺类型缓存
    key:    cache:shop:type
    value:  shopType数据
     */
    @Override
    public Result shopTypeList() {
        String key = CACHE_SHOP_KEY + "type";
        String json = stringRedisTemplate.opsForValue().get(key);
        System.out.println(json);
        if(StrUtil.isNotBlank(json)){ // 命中缓存
            List<ShopType> shopTypeList = JSONUtil.toList(json, ShopType.class);
            return Result.ok(shopTypeList);
        }

        List<ShopType> shopTypelist = list();

        if(shopTypelist == null) return Result.fail("获取商户类别失败");

        String value = JSONUtil.toJsonStr(shopTypelist);
        System.out.println(value);
        stringRedisTemplate.opsForValue().set(key, value);
        stringRedisTemplate.expire(key, CACHE_SHOP_TTL, TimeUnit.DAYS);

        return Result.ok(shopTypelist);
    }
}
