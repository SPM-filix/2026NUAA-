package com.nuaa.recycle.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.catalog.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Update("UPDATE product SET stock = stock - #{quantity}, sold_count = sold_count + #{quantity}, "
            + "version = version + 1 WHERE id = #{id} AND status = 'ON' "
            + "AND stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("id") Long id,
                    @Param("quantity") int quantity,
                    @Param("version") int version);

    @Update("UPDATE product SET stock = stock + #{quantity}, version = version + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
