package com.seckill.seckill.service;

/**
 * 消费者内部使用：表示用户已经有过该活动订单，需要回滚本次 Redis 预扣。
 */
public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String message) {
        super(message);
    }
}
