package com.seckill.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 扣 DB 库存：stock >= qty 作为条件，天然防超卖。
     * 返回受影响行数，0 表示库存不足。
     */
    @Update("UPDATE seckill_activity SET stock = stock - #{qty}, update_time = NOW() " +
            "WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    @Update("UPDATE seckill_activity SET stock = stock + #{qty}, update_time = NOW() WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    @Select("SELECT COUNT(*) FROM seckill_activity WHERE status = 1")
    long countActive();

    @Update("UPDATE seckill_activity SET status = 2, update_time = NOW() " +
            "WHERE status = 1 AND end_time < NOW()")
    int closeExpired();
}
