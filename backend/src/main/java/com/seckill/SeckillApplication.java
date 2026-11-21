package com.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 秒杀平台启动类
 *
 * 学习要点：
 * - @EnableScheduling：订单超时关闭、活动状态流转
 * - @EnableAsync：操作日志异步落库，不阻塞业务请求
 */
@SpringBootApplication
@MapperScan({"com.seckill.system.mapper", "com.seckill.product.mapper", "com.seckill.seckill.mapper"})
@EnableScheduling
@EnableAsync
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
