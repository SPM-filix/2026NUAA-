package com.nuaa.recycle.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nuaa.recycle.delivery.entity.DeliveryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeliveryRecordMapper extends BaseMapper<DeliveryRecord> {

    @Update("UPDATE delivery_record SET status = 'REVOKED', revoke_reason = #{reason}, "
            + "revoked_by_type = 'RECYCLER', revoked_by_id = #{recyclerId}, revoked_at = NOW() "
            + "WHERE id = #{recordId} AND status = 'NORMAL' AND recycler_id = #{recyclerId}")
    int revokeByRecycler(@Param("recordId") Long recordId,
                         @Param("recyclerId") Long recyclerId,
                         @Param("reason") String reason);

    @Update("UPDATE delivery_record SET status = 'REVOKED', revoke_reason = #{reason}, "
            + "revoked_by_type = 'ADMIN', revoked_by_id = #{adminId}, revoked_at = NOW() "
            + "WHERE id = #{recordId} AND status = 'NORMAL'")
    int revokeByAdmin(@Param("recordId") Long recordId,
                      @Param("adminId") Long adminId,
                      @Param("reason") String reason);
}
