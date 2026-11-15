package com.nuaa.recycle.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.auth.entity.Recycler;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RecyclerMapper extends BaseMapper<Recycler> {

    @Update("UPDATE recycler SET token_version = token_version + 1 WHERE id = #{id}")
    int bumpTokenVersion(@Param("id") Long id);

    @Update("UPDATE recycler SET password_hash = #{passwordHash}, token_version = token_version + 1, "
            + "must_change_password = 0 WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE recycler SET must_change_password = 1, token_version = token_version + 1 WHERE id = #{id}")
    int markMustChangePassword(@Param("id") Long id);
}
