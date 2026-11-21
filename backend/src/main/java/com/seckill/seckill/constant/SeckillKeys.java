package com.seckill.seckill.constant;

/**
 * Redis key / Stream 常量集中管理，避免拼错 key 排查半天
 */
public final class SeckillKeys {

    private SeckillKeys() {
    }

    /** 活动剩余库存（String 整数） */
    public static final String STOCK = "sk:seckill:stock:";
    /** 已购买用户集合（Set），用于一人一单 */
    public static final String USERS = "sk:seckill:users:";
    /** 秒杀令牌 */
    public static final String TOKEN = "sk:seckill:token:";
    /** 活动静态信息缓存 */
    public static final String ACTIVITY_DETAIL = "sk:cache:seckill:detail:";
    /** 活动列表缓存 */
    public static final String ACTIVITY_LIST = "sk:cache:seckill:list";
    /** 隐藏链接 */
    public static final String PATH = "sk:seckill:path:";
    /** 订单延迟关闭队列（ZSET） */
    public static final String CLOSE_QUEUE = "sk:seckill:order:close";
    /** 下单请求 Stream */
    public static final String ORDER_STREAM = "sk:seckill:order:stream";
    /** 消费组 */
    public static final String ORDER_GROUP = "sk:seckill:order:group";
    /** 消费者名（单实例） */
    public static final String ORDER_CONSUMER = "consumer-1";
    /** 异步下单失败原因 */
    public static final String ORDER_FAIL = "sk:seckill:result:fail:";
    /** orderNo -> userId，用于异步结果归属校验 */
    public static final String ORDER_OWNER = "sk:seckill:order:owner:";
}
