package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.*;
import com.nuaa.recycle.entity.*;
import com.nuaa.recycle.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AccountService {

    @Autowired private UserMapper userMapper;
    @Autowired private RecyclerMapper recyclerMapper;
    @Autowired private AppealMapper appealMapper;
    @Autowired private PointsLogMapper pointsLogMapper;
    @Autowired private SiteMapper siteMapper;
    @Autowired private AdminUserMapper adminUserMapper;

    public PageResult<User> userList(Integer page, Integer size, String keyword, String status) {
        Page<User> p = new Page<>(page, size);
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt);
        if (StringUtils.hasText(keyword)) qw.like(User::getPhone, keyword).or().like(User::getNickname, keyword);
        if (StringUtils.hasText(status)) qw.eq(User::getStatus, status);
        Page<User> result = userMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public User updateUserStatus(Long userId, String status, String reason) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(1005, "用户不存在");
        u.setStatus(status);
        if ("DISABLED".equals(status)) u.setDisabledReason(reason);
        u.setTokenVersion(u.getTokenVersion() + 1);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        return u;
    }

    @Transactional
    public Map<String, Object> resetUserPassword(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(1005, "用户不存在");
        String tempPassword = generateTempPassword(10);
        u.setPasswordHash(org.springframework.security.crypto.bcrypt.BCrypt.hashpw(tempPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        u.setMustChangePassword(true);
        u.setTokenVersion(u.getTokenVersion() + 1);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);
        Map<String, Object> map = new HashMap<>();
        map.put("tempPassword", tempPassword);
        map.put("expireAt", LocalDateTime.now().plusHours(24));
        return map;
    }

    public PageResult<Recycler> recyclerList(Integer page, Integer size, String status, String keyword) {
        Page<Recycler> p = new Page<>(page, size);
        LambdaQueryWrapper<Recycler> qw = new LambdaQueryWrapper<Recycler>().orderByDesc(Recycler::getCreatedAt);
        if (StringUtils.hasText(status)) qw.eq(Recycler::getStatus, status);
        if (StringUtils.hasText(keyword)) qw.like(Recycler::getPhone, keyword).or().like(Recycler::getName, keyword);
        Page<Recycler> result = recyclerMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public Recycler approveRecycler(Long recyclerId, Long siteId) {
        Recycler r = recyclerMapper.selectById(recyclerId);
        if (r == null) throw new BizException(1005, "回收员不存在");
        if (!"PENDING".equals(r.getStatus())) throw new BizException(1006, "仅待审核状态可通过");
        Site s = siteMapper.selectById(siteId);
        if (s == null) throw new BizException(1005, "站点不存在");
        r.setSiteId(siteId);
        r.setStatus("APPROVED");
        r.setApprovedBy(UserContext.getUserId());
        r.setApprovedAt(LocalDateTime.now());
        recyclerMapper.updateById(r);
        return r;
    }

    @Transactional
    public Recycler rejectRecycler(Long recyclerId, String reason) {
        Recycler r = recyclerMapper.selectById(recyclerId);
        if (r == null) throw new BizException(1005, "回收员不存在");
        if (!"PENDING".equals(r.getStatus())) throw new BizException(1006, "仅待审核状态可驳回");
        r.setStatus("REJECTED");
        r.setRejectReason(reason);
        recyclerMapper.updateById(r);
        return r;
    }

    @Transactional
    public Recycler updateRecyclerStatus(Long recyclerId, String status, String reason) {
        Recycler r = recyclerMapper.selectById(recyclerId);
        if (r == null) throw new BizException(1005, "回收员不存在");
        r.setStatus(status);
        r.setTokenVersion(r.getTokenVersion() + 1);
        recyclerMapper.updateById(r);
        return r;
    }

    @Transactional
    public Map<String, Object> adjustPoints(Long userId, Integer change, String reason) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException(1005, "用户不存在");
        int before = u.getPointsBalance();
        int after = before + change;
        if (after < 0) throw new BizException(3011, "操作会导致余额为负");
        u.setPointsBalance(after);
        u.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(u);

        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setType(change > 0 ? "INCOME" : "EXPENSE");
        log.setChange(change);
        log.setBalanceAfter(after);
        log.setBizType("ADMIN_ADJUST");
        log.setOperatorType("ADMIN");
        log.setOperatorId(UserContext.getUserId());
        log.setRemark(reason);
        log.setCreatedAt(LocalDateTime.now());
        pointsLogMapper.insert(log);

        Map<String, Object> map = new HashMap<>();
        map.put("logId", log.getId());
        map.put("userId", userId);
        map.put("change", change);
        map.put("balanceBefore", before);
        map.put("balanceAfter", after);
        return map;
    }

    public PageResult<Appeal> appealList(Integer page, Integer size, String status) {
        Page<Appeal> p = new Page<>(page, size);
        LambdaQueryWrapper<Appeal> qw = new LambdaQueryWrapper<Appeal>().orderByDesc(Appeal::getCreatedAt);
        if (StringUtils.hasText(status)) qw.eq(Appeal::getStatus, status);
        Page<Appeal> result = appealMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    @Transactional
    public Appeal handleAppeal(Long appealId, String result, String conclusion) {
        Appeal a = appealMapper.selectById(appealId);
        if (a == null) throw new BizException(1005, "申诉不存在");
        if (!"PENDING".equals(a.getStatus())) throw new BizException(1006, "该申诉已处理过");
        a.setStatus("ACCEPT".equals(result) ? "ACCEPTED" : "REJECTED");
        a.setConclusion(conclusion);
        a.setHandledBy(UserContext.getUserId());
        a.setHandledAt(LocalDateTime.now());
        appealMapper.updateById(a);
        return a;
    }

    @Transactional
    public Map<String, Object> resetRecyclerPassword(Long recyclerId) {
        Recycler r = recyclerMapper.selectById(recyclerId);
        if (r == null) throw new BizException(1005, "回收员不存在");
        String tempPassword = generateTempPassword(10);
        r.setPasswordHash(org.springframework.security.crypto.bcrypt.BCrypt.hashpw(tempPassword, org.springframework.security.crypto.bcrypt.BCrypt.gensalt()));
        r.setMustChangePassword(true);
        r.setTokenVersion(r.getTokenVersion() + 1);
        recyclerMapper.updateById(r);
        Map<String, Object> map = new HashMap<>();
        map.put("tempPassword", tempPassword);
        map.put("expireAt", LocalDateTime.now().plusHours(24));
        return map;
    }

    private String generateTempPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
