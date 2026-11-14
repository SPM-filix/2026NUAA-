package com.nuaa.recycle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.entity.Recycler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RecyclerMapper extends BaseMapper<Recycler> {
    @Update("UPDATE recycler SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(Long id);
}
