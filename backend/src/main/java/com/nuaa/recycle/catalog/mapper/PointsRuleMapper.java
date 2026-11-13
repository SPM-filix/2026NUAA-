package com.nuaa.recycle.catalog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.catalog.entity.PointsRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PointsRuleMapper extends BaseMapper<PointsRule> {

    @Select("SELECT * FROM points_rule WHERE category_id = #{categoryId} "
            + "AND effective_from <= NOW() ORDER BY effective_from DESC LIMIT 1")
    PointsRule findCurrent(@Param("categoryId") Long categoryId);
}
