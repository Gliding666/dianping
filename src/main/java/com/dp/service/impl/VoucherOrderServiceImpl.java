package com.dp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dp.dto.Result;
import com.dp.entity.SeckillVoucher;
import com.dp.entity.VoucherOrder;
import com.dp.mapper.VoucherOrderMapper;
import com.dp.service.ISeckillVoucherService;
import com.dp.service.IVoucherOrderService;
import com.dp.utils.MyRedisLock;
import com.dp.utils.RedisIdWorker;
import com.dp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

//    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            // 1.不断从队列中的订单信息
            while(true) {
                try {
                    VoucherOrder voucherOrder = orderTasks.take();
                    // 2. 创建订单
                    save(voucherOrder);
                    // 扣减库存
                    seckillVoucherService.update()
                            .setSql("stock = stock - 1")
                            .eq("voucher_id", voucherOrder.getVoucherId())
                            .ge("stock", 1).update();
                } catch (Exception e) {
                    log.error("处理订单异常" + e);
                }
            }
        }
    }

    @RabbitListener(queues = "orderQueue")
    public void receiver(VoucherOrder voucherOrder){
//        System.out.println("消息队列获取消息成功");
        // 2. 创建订单
        save(voucherOrder);
        // 扣减库存
        seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .ge("stock", 1).update();
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // 1.执行lua脚本
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 2.不为0没有购买资格
        if(res != 0){
            return Result.fail(res == 1 ? "库存不足" : "不能重复下单");
        }
        // 3.为0,有购买资格，把下单信息保存到阻塞队列

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 设置订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 设置用户id
        voucherOrder.setUserId(userId);
        // 设置代金券id
        voucherOrder.setVoucherId(voucherId);

//        orderTasks.add(voucherOrder); // 添加到阻塞队列

        //添加到RabbitMQ消息队列
        rabbitTemplate.convertAndSend("amq.direct", "my-yyds", voucherOrder);

        // 返回订单id
        return Result.ok(orderId);
    }


    //自己实现的redis分布式锁
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        if(voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("秒杀未开始");
//        }
//        if(voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("秒杀以结束");
//        }
//        if (voucher.getStock() < 1) {
//            return Result.fail("库存不足");
//        }
//
//        Long userId = UserHolder.getUser().getId();
//        // 创建锁对象
////        MyRedisLock lock = new MyRedisLock(userId + "", stringRedisTemplate);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//
//        boolean success = lock.tryLock();
//        if(! success) {
//            return Result.fail("不能重复下单");

//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            lock.unlock();
//        }
//    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if(count > 0) {
            // 用户已经购买过一次了
            return Result.fail("用户已经购买过一次了");
        }
        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .ge("stock", 1).update();

        if(!success) {
            return Result.fail("库存不足");
        }
        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 设置订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 设置用户id
        voucherOrder.setUserId(userId);
        // 设置代金券id
        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        return Result.ok(orderId);
    }
}
