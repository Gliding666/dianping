package com.dp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dp.dto.Result;
import com.dp.entity.Shop;

public interface IShopService extends IService<Shop> {
    Result queryShopByType(Integer typeId, Integer current);

    Result queryShopById(Long id);
}
