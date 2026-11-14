package com.nuaa.recycle.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.auth.entity.Recycler;
import com.nuaa.recycle.auth.entity.User;
import com.nuaa.recycle.auth.mapper.RecyclerMapper;
import com.nuaa.recycle.auth.mapper.UserMapper;
import com.nuaa.recycle.catalog.entity.PointsRule;
import com.nuaa.recycle.catalog.entity.RecycleCategory;
import com.nuaa.recycle.catalog.entity.Site;
import com.nuaa.recycle.catalog.mapper.PointsRuleMapper;
import com.nuaa.recycle.catalog.mapper.RecycleCategoryMapper;
import com.nuaa.recycle.catalog.mapper.SiteMapper;
import com.nuaa.recycle.common.api.PageResult;
import com.nuaa.recycle.common.auth.JwtService;
import com.nuaa.recycle.common.auth.Role;
import com.nuaa.recycle.common.auth.UserContext;
import com.nuaa.recycle.common.error.BizException;
import com.nuaa.recycle.common.error.ErrorCode;
import com.nuaa.recycle.common.util.CodeGenerator;
import com.nuaa.recycle.common.util.MaskUtils;
import com.nuaa.recycle.delivery.dto.DeliveryCreateRequest;
import com.nuaa.recycle.delivery.dto.DeliveryCreateResponse;
import com.nuaa.recycle.delivery.dto.DeliveryRecordResponse;
import com.nuaa.recycle.delivery.dto.RevokeResponse;
import com.nuaa.recycle.delivery.entity.DeliveryRecord;
import com.nuaa.recycle.delivery.entity.PointsLog;
import com.nuaa.recycle.delivery.mapper.DeliveryRecordMapper;
import com.nuaa.recycle.delivery.mapper.PointsLogMapper;
import com.nuaa.recycle.user.dto.UserLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final int REVOKE_WINDOW_MINUTES = 5;

    private final UserMapper userMapper;
    private final RecyclerMapper recyclerMapper;
    private final RecycleCategoryMapper categoryMapper;
    private final PointsRuleMapper pointsRuleMapper;
    private final SiteMapper siteMapper;
    private final DeliveryRecordMapper deliveryRecordMapper;
    private final PointsLogMapper pointsLogMapper;
    private final JwtService jwtService;
    private final CodeGenerator codeGenerator;

    @Transactional
    public DeliveryCreateResponse create(DeliveryCreateRequest request) {
        Recycler recycler = requireRecycler();
        User user = userMapper.selectById(request.userId());
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        RecycleCategory category = categoryMapper.selectById(request.categoryId());
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "垃圾类别不存在");
        }
        if (!"ENABLED".equals(category.getStatus())) {
            throw new BizException(ErrorCode.CATEGORY_DISABLED);
        }
        PointsRule rule = pointsRuleMapper.findCurrent(category.getId());
        if (rule == null) {
            throw new BizException(ErrorCode.CATEGORY_DISABLED, "该类别未配置生效的积分规则");
        }
        int points = calculatePoints(request.weight(), rule.getPointsPerKg());

        LocalDateTime now = LocalDateTime.now();
        DeliveryRecord record = new DeliveryRecord();
        record.setRecordNo(nextRecordNo());
        record.setUserId(user.getId());
        record.setRecyclerId(recycler.getId());
        record.setSiteId(recycler.getSiteId());
        record.setCategoryId(category.getId());
        record.setRuleId(rule.getId());
        record.setWeight(request.weight().setScale(2, RoundingMode.HALF_UP));
        record.setPoints(points);
        record.setStatus("NORMAL");
        record.setRemark(request.remark());
        record.setCreatedAt(now);
        deliveryRecordMapper.insert(record);

        if (userMapper.addPoints(user.getId(), points) != 1) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "积分更新失败");
        }
        User updated = userMapper.selectById(user.getId());

        PointsLog log = new PointsLog();
        log.setUserId(user.getId());
        log.setType("INCOME");
        log.setChange(points);
        log.setBalanceAfter(updated.getPointsBalance());
        log.setBizType("DELIVERY");
        log.setBizId(record.getId());
        log.setBizNo(record.getRecordNo());
        log.setOperatorType("RECYCLER");
        log.setOperatorId(recycler.getId());
        log.setCreatedAt(now);
        pointsLogMapper.insert(log);

        return new DeliveryCreateResponse(record.getId(), record.getRecordNo(), points,
                record.getWeight(), category.getName(), updated.getPointsBalance(), now,
                now.plusMinutes(REVOKE_WINDOW_MINUTES));
    }

    public UserLookupResponse lookup(String keyword, String qrContent) {
        User user;
        if (StringUtils.hasText(qrContent)) {
            JwtService.JwtPayload payload = jwtService.parse(stripPrefix(qrContent));
            if (!"QR".equals(payload.type())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "二维码无效");
            }
            user = userMapper.selectById(payload.id());
        } else if (StringUtils.hasText(keyword) && keyword.matches("^1\\d{10}$")) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, keyword));
        } else if (StringUtils.hasText(keyword) && keyword.matches("^\\d{6}$")) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserCode, keyword));
        } else {
            throw new BizException(ErrorCode.PARAM_INVALID, "请输入手机号、6 位用户编号或扫描二维码");
        }
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return new UserLookupResponse(user.getId(), user.getUserCode(), user.getNickname(),
                MaskUtils.maskPhone(user.getPhone()), user.getAvatarUrl(),
                user.getPointsBalance(), user.getStatus());
    }

    public PageResult<DeliveryRecordResponse> myRecords(long page, long size, String status,
                                                        Long categoryId, LocalDate from, LocalDate to) {
        Long recyclerId = UserContext.id();
        LambdaQueryWrapper<DeliveryRecord> wrapper = baseQuery(status, categoryId, from, to)
                .eq(DeliveryRecord::getRecyclerId, recyclerId)
                .orderByDesc(DeliveryRecord::getCreatedAt);
        Page<DeliveryRecord> result = deliveryRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<DeliveryRecordResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(result, list);
    }

    public PageResult<DeliveryRecordResponse> adminList(long page, long size, Long recyclerId,
                                                        String status, Long categoryId,
                                                        LocalDate from, LocalDate to) {
        LambdaQueryWrapper<DeliveryRecord> wrapper = baseQuery(status, categoryId, from, to)
                .eq(recyclerId != null, DeliveryRecord::getRecyclerId, recyclerId)
                .orderByDesc(DeliveryRecord::getCreatedAt);
        Page<DeliveryRecord> result = deliveryRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<DeliveryRecordResponse> list = result.getRecords().stream().map(this::toResponse).toList();
        return PageResult.of(result, list);
    }

    @Transactional
    public RevokeResponse revoke(Long recordId, String reason) {
        Long recyclerId = UserContext.id();
        DeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null || !record.getRecyclerId().equals(recyclerId)) {
            throw new BizException(ErrorCode.DELIVERY_NOT_FOUND);
        }
        if ("REVOKED".equals(record.getStatus())) {
            throw new BizException(ErrorCode.ALREADY_REVOKED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!isWithinRevokeWindow(record.getCreatedAt(), now)) {
            throw new BizException(ErrorCode.REVOKE_WINDOW_EXPIRED);
        }
        if (deliveryRecordMapper.revokeByRecycler(recordId, recyclerId, reason) == 0) {
            throw new BizException(ErrorCode.ALREADY_REVOKED);
        }
        if (userMapper.deductPoints(record.getUserId(), record.getPoints()) == 0) {
            throw new BizException(ErrorCode.POINTS_INSUFFICIENT, "用户余额不足以扣回该笔积分");
        }
        User user = userMapper.selectById(record.getUserId());
        writeRevokeLog(record, user, record.getPoints(), "RECYCLER", recyclerId, reason, now);
        return new RevokeResponse(recordId, "REVOKED", record.getPoints(),
                user.getPointsBalance(), now, null, null);
    }

    @Transactional
    public RevokeResponse adminRevoke(Long recordId, String reason) {
        Long adminId = UserContext.id();
        DeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.DELIVERY_NOT_FOUND);
        }
        if (deliveryRecordMapper.revokeByAdmin(recordId, adminId, reason) == 0) {
            throw new BizException(ErrorCode.ALREADY_REVOKED);
        }
        User user = userMapper.selectById(record.getUserId());
        int deducted = Math.min(record.getPoints(), user.getPointsBalance());
        int deficit = record.getPoints() - deducted;
        if (userMapper.deductPointsAllowZero(record.getUserId(), record.getPoints()) == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "积分扣回失败");
        }
        User updated = userMapper.selectById(record.getUserId());
        writeRevokeLog(record, updated, deducted, "ADMIN", adminId, reason, LocalDateTime.now());
        return new RevokeResponse(recordId, "REVOKED", record.getPoints(),
                updated.getPointsBalance(), LocalDateTime.now(), deficit, "ADMIN");
    }

    public DeliveryRecordResponse detail(Long recordId) {
        DeliveryRecord record = deliveryRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.DELIVERY_NOT_FOUND);
        }
        if (UserContext.role() == Role.RECYCLER && !record.getRecyclerId().equals(UserContext.id())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toResponse(record);
    }

    static int calculatePoints(BigDecimal weight, int pointsPerKg) {
        int points = weight.multiply(BigDecimal.valueOf(pointsPerKg))
                .setScale(0, RoundingMode.HALF_UP).intValue();
        if (points < 1 && weight.compareTo(BigDecimal.ZERO) > 0) {
            return 1;
        }
        return Math.max(points, 0);
    }

    static boolean isWithinRevokeWindow(LocalDateTime createdAt, LocalDateTime now) {
        return createdAt != null && !now.isAfter(createdAt.plusMinutes(REVOKE_WINDOW_MINUTES));
    }

    private Recycler requireRecycler() {
        Recycler recycler = recyclerMapper.selectById(UserContext.id());
        if (recycler == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "回收员不存在");
        }
        return recycler;
    }

    private LambdaQueryWrapper<DeliveryRecord> baseQuery(String status, Long categoryId,
                                                         LocalDate from, LocalDate to) {
        return new LambdaQueryWrapper<DeliveryRecord>()
                .eq(StringUtils.hasText(status), DeliveryRecord::getStatus, status)
                .eq(categoryId != null, DeliveryRecord::getCategoryId, categoryId)
                .ge(from != null, DeliveryRecord::getCreatedAt, from == null ? null : from.atStartOfDay())
                .lt(to != null, DeliveryRecord::getCreatedAt, to == null ? null : to.plusDays(1).atStartOfDay());
    }

    private DeliveryRecordResponse toResponse(DeliveryRecord record) {
        User user = userMapper.selectById(record.getUserId());
        Recycler recycler = recyclerMapper.selectById(record.getRecyclerId());
        RecycleCategory category = record.getCategoryId() == null ? null
                : categoryMapper.selectById(record.getCategoryId());
        Site site = record.getSiteId() == null ? null : siteMapper.selectById(record.getSiteId());
        boolean revocable = "NORMAL".equals(record.getStatus())
                && isWithinRevokeWindow(record.getCreatedAt(), LocalDateTime.now());
        return new DeliveryRecordResponse(record.getId(), record.getRecordNo(), record.getUserId(),
                user == null ? null : user.getNickname(),
                user == null ? null : MaskUtils.maskPhone(user.getPhone()),
                record.getRecyclerId(), recycler == null ? null : recycler.getName(),
                site == null ? null : site.getName(), record.getCategoryId(),
                category == null ? null : category.getName(), record.getWeight(), record.getPoints(),
                record.getStatus(), record.getRemark(), record.getRevokeReason(),
                record.getRevokedByType(), record.getRevokedAt(), record.getCreatedAt(), revocable);
    }

    private void writeRevokeLog(DeliveryRecord record, User user, int deducted,
                                String operatorType, Long operatorId, String reason, LocalDateTime now) {
        PointsLog log = new PointsLog();
        log.setUserId(record.getUserId());
        log.setType("REVOKE");
        log.setChange(-deducted);
        log.setBalanceAfter(user.getPointsBalance());
        log.setBizType("REVOKE");
        log.setBizId(record.getId());
        log.setBizNo(record.getRecordNo());
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(reason);
        log.setCreatedAt(now);
        pointsLogMapper.insert(log);
    }

    private String nextRecordNo() {
        for (int i = 0; i < 5; i++) {
            String recordNo = codeGenerator.recordNo();
            if (deliveryRecordMapper.selectCount(
                    new LambdaQueryWrapper<DeliveryRecord>().eq(DeliveryRecord::getRecordNo, recordNo)) == 0) {
                return recordNo;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "流水号生成失败");
    }

    private String stripPrefix(String qrContent) {
        String prefix = "NUAA-RECYCLE:U:";
        return qrContent.startsWith(prefix) ? qrContent.substring(prefix.length()) : qrContent;
    }
}
