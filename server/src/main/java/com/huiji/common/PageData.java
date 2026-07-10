package com.huiji.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页响应数据: { "list": [], "total": 0, "page": 1, "size": 20 }
 */
@Data
@AllArgsConstructor
public class PageData<T> {

    private List<T> list;
    private long total;
    private int page;
    private int size;

    public static <T> PageData<T> of(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }

    public static <T> PageData<T> of(List<T> list, long total, int page, int size) {
        return new PageData<>(list, total, page, size);
    }
}
