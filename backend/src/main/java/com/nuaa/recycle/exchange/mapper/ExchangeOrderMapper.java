package com.nuaa.recycle.exchange.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExchangeOrderMapper extends BaseMapper<ExchangeOrder> {

    @Select("SELECT * FROM exchange_order WHERE user_id = #{userId} AND idempotency_key = #{key} LIMIT 1")
    ExchangeOrder findByIdempotencyKey(@Param("userId") Long userId, @Param("key") String key);

    @Select("SELECT * FROM exchange_order WHERE redeem_code = #{code} LIMIT 1")
    ExchangeOrder findByRedeemCode(@Param("code") String code);

    @Update("UPDATE exchange_order SET status = 'COMPLETED', verified_by = #{recyclerId}, "
            + "verified_site_id = #{siteId}, verified_at = NOW() "
            + "WHERE id = #{orderId} AND status = 'PENDING' AND redeem_expire_at > NOW()")
    int complete(@Param("orderId") Long orderId,
                 @Param("recyclerId") Long recyclerId,
                 @Param("siteId") Long siteId);

    @Update("UPDATE exchange_order SET status = 'CANCELLED', cancel_reason = #{reason}, "
            + "cancelled_by = #{adminId} WHERE id = #{orderId} AND status = 'PENDING'")
    int cancel(@Param("orderId") Long orderId,
               @Param("adminId") Long adminId,
               @Param("reason") String reason);
}
