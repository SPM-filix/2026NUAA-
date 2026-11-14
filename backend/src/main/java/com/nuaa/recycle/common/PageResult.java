package com.nuaa.recycle.common;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int size;
    private List<T> list;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setList(list);
        return result;
    }
}
