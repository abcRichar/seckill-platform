package com.seckill.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.product.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE product SET sales = sales + #{qty} WHERE id = #{id}")
    int addSales(@Param("id") Long id, @Param("qty") int qty);
}
