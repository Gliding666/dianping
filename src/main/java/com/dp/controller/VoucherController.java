package com.dp.controller;

import com.dp.dto.Result;
import com.dp.entity.Voucher;
import com.dp.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增优惠券
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable Long shopId){
        return voucherService.queryVoucherOfShop(shopId);
    }

}
