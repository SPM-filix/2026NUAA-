package com.nuaa.recycle.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(long total, long page, long size, List<T> list) {

    public static <T> PageResult<T> of(IPage<?> page, List<T> list) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }
}
