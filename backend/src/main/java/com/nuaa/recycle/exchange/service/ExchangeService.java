package com.nuaa.recycle.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.auth.entity.Recycler;
import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.entity.Product;
import com.nuaa.recycle.catalog.entity.Site;
import com.nuaa.recycle.catalog.mapper.ProductMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.common.util.CodeGenerator;
import com.nuaa.recycle.delivery.entity.PointsLog;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.exchange.dto.CancelResponse;
import com.nuaa.recycle.exchange.dto.ExchangeCreateRequest;
import com.nuaa.recycle.exchange.dto.ExchangeOrderResponse;
import com.nuaa.recycle.exchange.dto.RedeemResponse;
import com.nuaa.recycle.exchange.entity.ExchangeOrder;
import com.nuaa.recycle.exchange.mapper.ExchangeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private static final int REDEEM_CODE_VALID_DAYS = 30;
    private static final int STOCK_RETRY_TIMES = 3;
    private static final int CODE_RETRY_TIMES = 5;

    private final UserMapper userMapper;
    private final RecyclerMapper recyclerMapper;
    private final ProductMapper productMapper;
    private final ExchangeOrderMapper exchangeOrderMapper;
    private final PointsLogMapper pointsLogMapper;
    private final SiteMapper siteMapper;
    private final CodeGenerator codeGenerator;
    private final TransactionTemplate transactionTemplate;

    public ExchangeOrderResponse create(ExchangeCreateRequest request) {
        Long userId = UserContext.id();
        ExchangeOrder existing = exchangeOrderMapper.findByIdempotencyKey(userId, request.idempotencyKey());
        if (existing != null) {
            User current = userMapper.selectById(userId);
            return toResponse(existing, current == null ? null : current.getPointsBalance());
        }
        try {
            return transactionTemplate.execute(status -> doCreate(userId, request));
        } catch (DuplicateKeyException e) {
            if (isIdempotencyConflict(e)) {
                ExchangeOrder first = exchangeOrderMapper.findByIdempotencyKey(userId, request.idempotencyKey());
                if (first != null) {
                    User current = userMapper.selectById(userId);
                    return toResponse(first, current == null ? null : current.getPointsBalance());
                }
            }
            throw new BizException(ErrorCode.STATE_NOT_ALLOWED, "兑换冲突，请重试");
        }
    }

    private ExchangeOrderResponse doCreate(Long userId, ExchangeCreateRequest request) {
        Product product = productMapper.selectById(request.productId());
        if (product == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        if (!"ON".equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF);
        }
        int quantity = request.quantity();
        int maxQuantity = Math.min(product.getStock() == null ? 0 : product.getStock(), 5);
        if (quantity < 1 || quantity > maxQuantity) {
            throw new BizException(ErrorCode.QUANTITY_INVALID);
        }
        int cost = product.getPointsPrice() * quantity;

        if (userMapper.deductPoints(userId, cost) == 0) {
            throw new BizException(ErrorCode.POINTS_INSUFFICIENT);
        }
        User userAfterPoints = userMapper.selectById(userId);

        int version = product.getVersion() == null ? 0 : product.getVersion();
        boolean deducted = false;
        for (int attempt = 0; attempt < STOCK_RETRY_TIMES; attempt++) {
            if (productMapper.deductStock(product.getId(), quantity, version) == 1) {
                deducted = true;
                break;
            }
            Product latest = productMapper.selectById(product.getId());
            if (latest == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
            }
            if (!"ON".equals(latest.getStatus())) {
                throw new BizException(ErrorCode.PRODUCT_OFF_SHELF);
            }
            if (latest.getStock() == null || latest.getStock() < quantity) {
                throw new BizException(ErrorCode.STOCK_INSUFFICIENT);
            }
            version = latest.getVersion() == null ? 0 : latest.getVersion();
        }
        if (!deducted) {
            throw new BizException(ErrorCode.STOCK_INSUFFICIENT);
        }

        ExchangeOrder order = new ExchangeOrder();
        order.setUserId(userId);
        order.setProductId(product.getId());
        order.setQuantity(quantity);
        order.setPointsCost(cost);
        order.setStatus("PENDING");
        order.setRedeemExpireAt(LocalDateTime.now().plusDays(REDEEM_CODE_VALID_DAYS));
        order.setIdempotencyKey(request.idempotencyKey());
        order.setCreatedAt(LocalDateTime.now());
        insertWithRetry(order);

        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setType("EXPENSE");
        log.setChange(-cost);
        log.setBalanceAfter(userAfterPoints.getPointsBalance());
        log.setBizType("EXCHANGE");
        log.setBizId(order.getId());
        log.setBizNo(order.getOrderNo());
        log.setOperatorType("USER");
        log.setOperatorId(userId);
        log.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(log);

        return toResponse(order, userAfterPoints.getPointsBalance());
    }

    @Transactional
    public RedeemResponse redeem(String redeemCode) {
        Long recyclerId = UserContext.id();
        Recycler recycler = recyclerMapper.selectById(recyclerId);
        if (recycler == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "回收员不存在");
        }
        String code = redeemCode.trim().toUpperCase();
        ExchangeOrder order = exchangeOrderMapper.findByRedeemCode(code);
        if (order == null) {
            throw new BizException(ErrorCode.REDEEM_CODE_INVALID);
        }
        if (exchangeOrderMapper.complete(order.getId(), recyclerId, recycler.getSiteId()) == 0) {
            throw new BizException(ErrorCode.REDEEM_CODE_INVALID);
        }
        ExchangeOrder updated = exchangeOrderMapper.selectById(order.getId());
        return new RedeemResponse(updated.getId(), updated.getOrderNo(), productName(updated.getProductId()),
                updated.getQuantity(), userName(updated.getUserId()), updated.getStatus(),
                updated.getVerifiedAt(), recycler.getName());
    }

    @Transactional
    public CancelResponse cancel(Long orderId, String reason) {
        Long adminId = UserContext.id();
        ExchangeOrder order = exchangeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (exchangeOrderMapper.cancel(orderId, adminId, reason) == 0) {
            throw new BizException(ErrorCode.ORDER_STATE_INVALID);
        }
        if (userMapper.addPoints(order.getUserId(), order.getPointsCost()) != 1) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "积分退回失败");
        }
        if (productMapper.restoreStock(order.getProductId(), order.getQuantity()) == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "库存恢复失败");
        }
        User user = userMapper.selectById(order.getUserId());

        PointsLog log = new PointsLog();
        log.setUserId(order.getUserId());
        log.setType("REFUND");
        log.setChange(order.getPointsCost());
        log.setBalanceAfter(user.getPointsBalance());
        log.setBizType("REFUND");
        log.setBizId(order.getId());
        log.setBizNo(order.getOrderNo());
        log.setOperatorType("ADMIN");
        log.setOperatorId(adminId);
        log.setRemark(reason);
        log.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(log);

        return new CancelResponse(orderId, "CANCELLED", order.getPointsCost(),
                order.getQuantity(), user.getPointsBalance());
    }

    public ExchangeOrderResponse detail(Long orderId) {
        ExchangeOrder order = exchangeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (UserContext.role() == Role.USER && !order.getUserId().equals(UserContext.id())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toResponse(order, null);
    }

    public ExchangeOrderResponse lookup(String redeemCode) {
        ExchangeOrder order = exchangeOrderMapper.findByRedeemCode(redeemCode.trim().toUpperCase());
        if (order == null || !"PENDING".equals(order.getStatus())
                || order.getRedeemExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.REDEEM_CODE_INVALID);
        }
        return toResponse(order, null);
    }

    public PageResult<ExchangeOrderResponse> myRedemptions(long page, long size,
                                                           LocalDate from, LocalDate to) {
        Long recyclerId = UserContext.id();
        LambdaQueryWrapper<ExchangeOrder> wrapper = new LambdaQueryWrapper<ExchangeOrder>()
                .eq(ExchangeOrder::getVerifiedBy, recyclerId)
                .eq(ExchangeOrder::getStatus, "COMPLETED")
                .ge(from != null, ExchangeOrder::getVerifiedAt, from == null ? null : from.atStartOfDay())
                .lt(to != null, ExchangeOrder::getVerifiedAt, to == null ? null : to.plusDays(1).atStartOfDay())
                .orderByDesc(ExchangeOrder::getVerifiedAt);
        Page<ExchangeOrder> result = exchangeOrderMapper.selectPage(new Page<>(page, size), wrapper);
        List<ExchangeOrderResponse> list = result.getRecords().stream()
                .map(order -> toResponse(order, null)).toList();
        return PageResult.of(result, list);
    }

    public PageResult<ExchangeOrderResponse> adminList(long page, long size, String status,
                                                       Long userId, String orderNo) {
        LambdaQueryWrapper<ExchangeOrder> wrapper = new LambdaQueryWrapper<ExchangeOrder>()
                .eq(StringUtils.hasText(status), ExchangeOrder::getStatus, status)
                .eq(userId != null, ExchangeOrder::getUserId, userId)
                .like(StringUtils.hasText(orderNo), ExchangeOrder::getOrderNo, orderNo)
                .orderByDesc(ExchangeOrder::getCreatedAt);
        Page<ExchangeOrder> result = exchangeOrderMapper.selectPage(new Page<>(page, size), wrapper);
        List<ExchangeOrderResponse> list = result.getRecords().stream()
                .map(order -> toResponse(order, null)).toList();
        return PageResult.of(result, list);
    }

    private void insertWithRetry(ExchangeOrder order) {
        for (int attempt = 0; attempt < CODE_RETRY_TIMES; attempt++) {
            try {
                order.setOrderNo(nextOrderNo());
                order.setRedeemCode(nextRedeemCode());
                exchangeOrderMapper.insert(order);
                return;
            } catch (DuplicateKeyException e) {
                if (isIdempotencyConflict(e)) {
                    throw e;
                }
                order.setId(null);
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "订单号生成失败");
    }

    private String nextOrderNo() {
        for (int i = 0; i < CODE_RETRY_TIMES; i++) {
            String orderNo = codeGenerator.orderNo();
            if (exchangeOrderMapper.selectCount(
                    new LambdaQueryWrapper<ExchangeOrder>().eq(ExchangeOrder::getOrderNo, orderNo)) == 0) {
                return orderNo;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "订单号生成失败");
    }

    private String nextRedeemCode() {
        for (int i = 0; i < CODE_RETRY_TIMES; i++) {
            String code = codeGenerator.redeemCode();
            if (exchangeOrderMapper.selectCount(
                    new LambdaQueryWrapper<ExchangeOrder>().eq(ExchangeOrder::getRedeemCode, code)) == 0) {
                return code;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "核销码生成失败");
    }

    private boolean isIdempotencyConflict(DuplicateKeyException e) {
        String message = e.getMessage();
        return message != null && (message.contains("uk_user_idem") || message.contains("idempotency_key"));
    }

    private ExchangeOrderResponse toResponse(ExchangeOrder order, Integer balanceAfter) {
        Product product = productMapper.selectById(order.getProductId());
        Site site = order.getVerifiedSiteId() == null ? null : siteMapper.selectById(order.getVerifiedSiteId());
        Recycler verifier = order.getVerifiedBy() == null ? null : recyclerMapper.selectById(order.getVerifiedBy());
        return new ExchangeOrderResponse(order.getId(), order.getOrderNo(), order.getUserId(),
                userName(order.getUserId()), order.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getCoverUrl(),
                order.getQuantity(), order.getPointsCost(), order.getStatus(), order.getRedeemCode(),
                order.getRedeemExpireAt(), site == null ? null : site.getName(),
                verifier == null ? null : verifier.getName(),
                order.getVerifiedAt(), order.getCancelReason(), balanceAfter, order.getCreatedAt());
    }

    private String productName(Long productId) {
        Product product = productMapper.selectById(productId);
        return product == null ? null : product.getName();
    }

    private String userName(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getNickname();
    }
}
