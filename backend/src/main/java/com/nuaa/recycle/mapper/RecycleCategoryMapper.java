package com.nuaa.recycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.entity.RecycleCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecycleCategoryMapper extends BaseMapper<RecycleCategory> {
    @Select("SELECT c.*, r.points_per_kg as pointsPerKg, r.effective_from as ruleEffectiveFrom " +
            "FROM recycle_category c " +
            "LEFT JOIN points_rule r ON r.category_id = c.id " +
            "AND r.effective_from = (SELECT MAX(effective_from) FROM points_rule WHERE category_id = c.id AND effective_from <= NOW()) " +
            "WHERE c.status = 'ENABLED' ORDER BY c.sort, c.id")
    List<RecycleCategory> selectListWithCurrentRule();
}
