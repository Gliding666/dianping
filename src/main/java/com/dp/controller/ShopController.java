package com.dp.controller;

import com.dp.dto.Result;
import com.dp.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("shop")
public class ShopController {
    @Autowired
    private IShopService shopService;

    @GetMapping("of/type")
    public Result queryShopByType(
            Integer typeId,
            Integer current
    ){
        return shopService.queryShopByType(typeId, current);
    }

    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryShopById(id);
    }


}
