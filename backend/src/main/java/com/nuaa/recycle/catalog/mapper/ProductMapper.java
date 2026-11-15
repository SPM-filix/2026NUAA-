package com.nuaa.recycle.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.catalog.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 当前读（加行锁），用于乐观锁冲突后的重读：REPEATABLE READ 下普通 SELECT 会读事务快照，
     * 看不到其他事务已提交的新 version，会导致重试循环反复失败并误报 3002。
     */
    @Select("SELECT * FROM product WHERE id = #{id} FOR UPDATE")
    Product selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE product SET stock = stock - #{quantity}, sold_count = sold_count + #{quantity}, "
            + "version = version + 1 WHERE id = #{id} AND status = 'ON' "
            + "AND stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("id") Long id,
                    @Param("quantity") int quantity,
                    @Param("version") int version);

    @Update("UPDATE product SET stock = stock + #{quantity}, version = version + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
