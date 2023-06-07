package com.dp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

//    @Bean("directDlExchange")
//    public Exchange dlExchange(){
//        //创建一个新的死信交换机
//        return ExchangeBuilder.directExchange("dlx.direct").build();
//    }
//
//    @Bean("yydsDlQueue")   //创建一个新的死信队列
//    public Queue dlQueue(){
//        return QueueBuilder
//                .nonDurable("dl-yyds")
//                .build();
//    }
//
//    @Bean("dlBinding")   //死信交换机和死信队列进绑定
//    public Binding dlBinding(@Qualifier("directDlExchange") Exchange exchange,
//                             @Qualifier("yydsDlQueue") Queue queue){
//        return BindingBuilder
//                .bind(queue)
//                .to(exchange)
//                .with("dl-yyds")
//                .noargs();
//    }

    @Bean("directExchange")  //定义交换机Bean，可以很多个
    public Exchange exchange(){
        return ExchangeBuilder.directExchange("amq.direct").build();
    }

    @Bean("orderQueueBean")     //定义消息队列
    public Queue queue(){
        return QueueBuilder
                .nonDurable("orderQueue")   //非持久化类型
//                .deadLetterExchange("dlx.direct")   //指定死信交换机
//                .deadLetterRoutingKey("dl-yyds")   //指定死信RoutingKey
                .build();
    }

    @Bean("binding")
    public Binding binding(@Qualifier("directExchange") Exchange exchange,
                           @Qualifier("orderQueueBean") Queue queue){
      	//将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with("my-yyds")   //使用自定义的routingKey
                .noargs();
    }

    @Bean("jacksonConverter")   //直接创建一个用于JSON转换的Bean
    public Jackson2JsonMessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }

}