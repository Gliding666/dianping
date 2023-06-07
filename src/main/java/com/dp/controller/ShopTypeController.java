package com.dp.controller;

import com.dp.dto.Result;
import com.dp.entity.ShopType;
import com.dp.service.IShopTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Autowired
    private IShopTypeService shopTypeService;

    @GetMapping("list")
    public Result list(){
        return shopTypeService.shopTypeList();
    }
}
