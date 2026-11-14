package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.common.PageResult;
import com.nuaa.recycle.entity.Announcement;
import com.nuaa.recycle.mapper.ModuleAnnouncementMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AnnouncementService {

    @Autowired
    private ModuleAnnouncementMapper announcementMapper;

    public PageResult<Announcement> list(Integer page, Integer size, String type) {
        Page<Announcement> p = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, "PUBLISHED")
                .orderByDesc(Announcement::getPublishedAt);
        if (StringUtils.hasText(type)) {
            qw.eq(Announcement::getType, type);
        }
        Page<Announcement> result = announcementMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }

    public Announcement detail(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new BizException(1005, "公告不存在");
        return a;
    }

    @Transactional
    public Long create(Announcement dto) {
        if (dto.getStatus() == null) dto.setStatus("PUBLISHED");
        if ("PUBLISHED".equals(dto.getStatus())) {
            dto.setPublishedAt(LocalDateTime.now());
        }
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        announcementMapper.insert(dto);
        return dto.getId();
    }

    @Transactional
    public Announcement update(Long id, Announcement dto) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new BizException(1005, "公告不存在");
        if (dto.getTitle() != null) a.setTitle(dto.getTitle());
        if (dto.getType() != null) a.setType(dto.getType());
        if (dto.getContent() != null) a.setContent(dto.getContent());
        if (dto.getCoverUrl() != null) a.setCoverUrl(dto.getCoverUrl());
        if (dto.getStatus() != null) a.setStatus(dto.getStatus());
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
        return a;
    }

    @Transactional
    public Announcement updateStatus(Long id, String status) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) throw new BizException(1005, "公告不存在");
        a.setStatus(status);
        if ("PUBLISHED".equals(status) && a.getPublishedAt() == null) {
            a.setPublishedAt(LocalDateTime.now());
        }
        a.setUpdatedAt(LocalDateTime.now());
        announcementMapper.updateById(a);
        return a;
    }

    @Transactional
    public void delete(Long id) {
        announcementMapper.deleteById(id);
    }

    public PageResult<Announcement> adminList(Integer page, Integer size, String type, String status, String keyword) {
        Page<Announcement> p = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> qw = new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getCreatedAt);
        if (StringUtils.hasText(type)) qw.eq(Announcement::getType, type);
        if (StringUtils.hasText(status)) qw.eq(Announcement::getStatus, status);
        if (StringUtils.hasText(keyword)) qw.like(Announcement::getTitle, keyword);
        Page<Announcement> result = announcementMapper.selectPage(p, qw);
        return PageResult.of(result.getTotal(), page, size, result.getRecords());
    }
}
