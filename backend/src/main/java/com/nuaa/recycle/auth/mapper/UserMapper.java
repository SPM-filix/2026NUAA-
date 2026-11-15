package com.nuaa.recycle.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE `user` SET points_balance = points_balance + #{points} WHERE id = #{id}")
    int addPoints(@Param("id") Long id, @Param("points") int points);

    @Update("UPDATE `user` SET points_balance = points_balance - #{points} "
            + "WHERE id = #{id} AND points_balance >= #{points}")
    int deductPoints(@Param("id") Long id, @Param("points") int points);

    @Update("UPDATE `user` SET points_balance = GREATEST(points_balance - #{points}, 0) WHERE id = #{id}")
    int deductPointsAllowZero(@Param("id") Long id, @Param("points") int points);

    @Update("UPDATE `user` SET token_version = token_version + 1 WHERE id = #{id}")
    int bumpTokenVersion(@Param("id") Long id);

    @Update("UPDATE `user` SET password_hash = #{passwordHash}, token_version = token_version + 1, "
            + "must_change_password = 0 WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE `user` SET must_change_password = 1, token_version = token_version + 1 WHERE id = #{id}")
    int markMustChangePassword(@Param("id") Long id);
}
