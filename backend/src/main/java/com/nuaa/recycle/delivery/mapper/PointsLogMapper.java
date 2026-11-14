package com.nuaa.recycle.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.delivery.entity.PointsLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsLogMapper extends BaseMapper<PointsLog> {

    @Select("SELECT COALESCE(SUM(`change`), 0) FROM points_log WHERE user_id = #{userId} AND type = 'INCOME'")
    long sumIncome(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(-`change`), 0) FROM points_log WHERE user_id = #{userId} AND type = 'EXPENSE'")
    long sumExpense(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(-`change`), 0) FROM points_log WHERE user_id = #{userId} AND type = 'REVOKE'")
    long sumRevoked(@Param("userId") Long userId);
}
