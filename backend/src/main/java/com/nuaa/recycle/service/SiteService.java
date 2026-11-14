package com.nuaa.recycle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.entity.Site;
import com.nuaa.recycle.mapper.SiteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SiteService {

    @Autowired
    private SiteMapper siteMapper;

    public List<Site> list(Boolean enabledOnly) {
        LambdaQueryWrapper<Site> qw = new LambdaQueryWrapper<Site>();
        if (Boolean.TRUE.equals(enabledOnly)) {
            qw.eq(Site::getStatus, "ENABLED");
        }
        return siteMapper.selectList(qw);
    }

    @Transactional
    public Long create(Site dto) {
        siteMapper.insert(dto);
        return dto.getId();
    }

    @Transactional
    public Site update(Long id, Site dto) {
        Site s = siteMapper.selectById(id);
        if (s == null) throw new BizException(1005, "站点不存在");
        if (dto.getName() != null) s.setName(dto.getName());
        if (dto.getAddress() != null) s.setAddress(dto.getAddress());
        if (dto.getOpenTime() != null) s.setOpenTime(dto.getOpenTime());
        if (dto.getManager() != null) s.setManager(dto.getManager());
        if (dto.getPhone() != null) s.setPhone(dto.getPhone());
        siteMapper.updateById(s);
        return s;
    }

    @Transactional
    public Site updateStatus(Long id, String status) {
        Site s = siteMapper.selectById(id);
        if (s == null) throw new BizException(1005, "站点不存在");
        s.setStatus(status);
        siteMapper.updateById(s);
        return s;
    }
}
