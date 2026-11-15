package com.nuaa.recycle.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.auth.entity.Recycler;
import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.entity.Product;
import com.nuaa.recycle.catalog.entity.RecycleCategory;
import com.nuaa.recycle.catalog.entity.Site;
import com.nuaa.recycle.catalog.mapper.ProductMapper;
import com.nuaa.recycle.catalog.mapper.RecycleCategoryMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.JwtService;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.delivery.entity.DeliveryRecord;
import com.nuaa.recycle.delivery.entity.PointsLog;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import com.nuaa.recycle.user.dto.PointsLogResponse;
import com.nuaa.recycle.user.dto.PointsSummaryResponse;
import com.nuaa.recycle.user.dto.QrCodeResponse;
import com.nuaa.recycle.user.dto.UserDeliveryResponse;
import com.nuaa.recycle.user.dto.UserExchangeResponse;
import com.nuaa.recycle.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int QR_EXPIRE_MINUTES = 10;

    private final UserMapper userMapper;
    private final RecyclerMapper recyclerMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final PointsLogMapper pointsLogMapper;
    private final ExchangeOrderMapper exchangeOrderMapper;
    private final RecycleCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final SiteMapper siteMapper;
    private final JwtService jwtService;

    public UserProfileResponse profile() {
        User user = requireUser();
        return new UserProfileResponse(user.getId(), user.getUserCode(), user.getPhone(),
                user.getNickname(), user.getAvatarUrl(), user.getAddress(),
                user.getPointsBalance(), user.getStatus(), user.getCreatedAt());
    }

    public QrCodeResponse qrCode() {
        User user = requireUser();
        String token = jwtService.generateQrToken(user.getId(), user.getTokenVersion());
        return new QrCodeResponse(user.getUserCode(), "NUAA-RECYCLE:U:" + token,
                QR_EXPIRE_MINUTES * 60, LocalDateTime.now().plusMinutes(QR_EXPIRE_MINUTES));
    }

    public PointsSummaryResponse pointsSummary() {
        User user = requireUser();
        return new PointsSummaryResponse(user.getPointsBalance(),
                pointsLogMapper.sumIncome(user.getId()),
                pointsLogMapper.sumExpense(user.getId()),
                pointsLogMapper.sumRevoked(user.getId()));
    }

    public PageResult<PointsLogResponse> pointsLogs(long page, long size, String type) {
        User user = requireUser();
        LambdaQueryWrapper<PointsLog> wrapper = new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, user.getId())
                .eq(StringUtils.hasText(type), PointsLog::getType, type)
                .orderByDesc(PointsLog::getCreatedAt);
        Page<PointsLog> result = pointsLogMapper.selectPage(new Page<>(page, size), wrapper);
        List<PointsLogResponse> list = result.getRecords().stream()
                .map(log -> new PointsLogResponse(log.getId(), log.getType(), log.getChange(),
                        log.getBalanceAfter(), log.getBizType(), log.getBizNo(), log.getRemark(),
                        log.getCreatedAt()))
                .toList();
        return PageResult.of(result, list);
    }

    public PageResult<UserDeliveryResponse> deliveries(long page, long size) {
        User user = requireUser();
        Page<DeliveryRecord> result = deliveryRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<DeliveryRecord>()
                        .eq(DeliveryRecord::getUserId, user.getId())
                        .orderByDesc(DeliveryRecord::getCreatedAt));
        List<UserDeliveryResponse> list = result.getRecords().stream()
                .map(this::toUserDelivery)
                .toList();
        return PageResult.of(result, list);
    }

    public PageResult<UserExchangeResponse> exchanges(long page, long size, String status) {
        User user = requireUser();
        Page<ExchangeOrder> result = exchangeOrderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ExchangeOrder>()
                        .eq(ExchangeOrder::getUserId, user.getId())
                        .eq(StringUtils.hasText(status), ExchangeOrder::getStatus, status)
                        .orderByDesc(ExchangeOrder::getCreatedAt));
        List<UserExchangeResponse> list = result.getRecords().stream()
                .map(this::toUserExchange)
                .toList();
        return PageResult.of(result, list);
    }

    public UserExchangeResponse exchangeDetail(Long orderId) {
        User user = requireUser();
        ExchangeOrder order = exchangeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(user.getId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toUserExchange(order);
    }

    public User requireUser() {
        User user = userMapper.selectById(UserContext.id());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserDeliveryResponse toUserDelivery(DeliveryRecord record) {
        RecycleCategory category = record.getCategoryId() == null ? null
                : categoryMapper.selectById(record.getCategoryId());
        Recycler recycler = record.getRecyclerId() == null ? null
                : recyclerMapper.selectById(record.getRecyclerId());
        Site site = record.getSiteId() == null ? null : siteMapper.selectById(record.getSiteId());
        return new UserDeliveryResponse(record.getId(), record.getRecordNo(),
                record.getCategoryId(), category == null ? null : category.getName(),
                record.getWeight(), record.getPoints(),
                recycler == null ? null : recycler.getName(),
                site == null ? null : site.getName(),
                record.getStatus(), record.getRevokeReason(), record.getRevokedAt(),
                record.getCreatedAt());
    }

    private UserExchangeResponse toUserExchange(ExchangeOrder order) {
        Product product = order.getProductId() == null ? null : productMapper.selectById(order.getProductId());
        Site site = order.getVerifiedSiteId() == null ? null : siteMapper.selectById(order.getVerifiedSiteId());
        Recycler verifier = order.getVerifiedBy() == null ? null : recyclerMapper.selectById(order.getVerifiedBy());
        return new UserExchangeResponse(order.getId(), order.getOrderNo(), order.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getCoverUrl(),
                order.getQuantity(), order.getPointsCost(), order.getStatus(), order.getRedeemCode(),
                order.getRedeemExpireAt(), site == null ? null : site.getName(),
                verifier == null ? null : verifier.getName(),
                order.getVerifiedAt(), order.getCancelReason(), order.getCreatedAt());
    }
}
