package com.nuaa.recycle.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Update("UPDATE admin_user SET token_version = token_version + 1 WHERE id = #{id}")
    int bumpTokenVersion(@Param("id") Long id);

    @Update("UPDATE admin_user SET password_hash = #{passwordHash}, token_version = token_version + 1 "
            + "WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
