package com.nuaa.recycle.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.delivery.entity.DeliveryRecord;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import com.nuaa.recycle.user.dto.AppealCreateRequest;
import com.nuaa.recycle.user.dto.AppealCreateResponse;
import com.nuaa.recycle.user.dto.AppealResponse;
import com.nuaa.recycle.user.entity.Appeal;
import com.nuaa.recycle.user.mapper.AppealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API-USER-09 提交申诉、API-USER-10 本人申诉列表。
 * 只能对属于自己的投递流水或兑换订单提交申诉（BR-02）。
 */
@Service
@RequiredArgsConstructor
public class AppealService {

    private static final String TYPE_DELIVERY = "DELIVERY";
    private static final String TYPE_EXCHANGE = "EXCHANGE";

    private final AppealMapper appealMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final ExchangeOrderMapper exchangeOrderMapper;

    @Transactional
    public AppealCreateResponse create(AppealCreateRequest request) {
        Long userId = UserContext.id();
        validateTarget(request.targetType(), request.targetId(), userId);

        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setTargetType(request.targetType());
        appeal.setTargetId(request.targetId());
        appeal.setDescription(request.description());
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(LocalDateTime.now());
        appealMapper.insert(appeal);

        return new AppealCreateResponse(appeal.getId(), appeal.getStatus(), appeal.getCreatedAt());
    }

    public PageResult<AppealResponse> myAppeals(long page, long size) {
        Long userId = UserContext.id();
        Page<Appeal> result = appealMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Appeal>()
                        .eq(Appeal::getUserId, userId)
                        .orderByDesc(Appeal::getCreatedAt));
        List<AppealResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(result, list);
    }

    private void validateTarget(String targetType, Long targetId, Long userId) {
        if (TYPE_DELIVERY.equals(targetType)) {
            DeliveryRecord record = deliveryRecordMapper.selectById(targetId);
            if (record == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "投递流水不存在");
            }
            if (!record.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.FORBIDDEN, "只能对本人的投递流水提交申诉");
            }
            return;
        }
        ExchangeOrder order = exchangeOrderMapper.selectById(targetId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "兑换订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能对本人的兑换订单提交申诉");
        }
    }

    private AppealResponse toResponse(Appeal appeal) {
        return new AppealResponse(appeal.getId(), appeal.getTargetType(), appeal.getTargetId(),
                targetNo(appeal.getTargetType(), appeal.getTargetId()), appeal.getDescription(),
                appeal.getStatus(), appeal.getConclusion(), appeal.getHandledAt(), appeal.getCreatedAt());
    }

    private String targetNo(String targetType, Long targetId) {
        if (TYPE_DELIVERY.equals(targetType)) {
            DeliveryRecord record = deliveryRecordMapper.selectById(targetId);
            return record == null ? null : record.getRecordNo();
        }
        ExchangeOrder order = exchangeOrderMapper.selectById(targetId);
        return order == null ? null : order.getOrderNo();
    }
}
