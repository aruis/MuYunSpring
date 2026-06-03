package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.PageResult;

import java.util.List;

public record WebPageResponse<T>(List<T> records,
                                 long total,
                                 int pageNum,
                                 int pageSize,
                                 long pages,
                                 boolean totalKnown) {
    public static <T> WebPageResponse<T> from(PageResult<T> page) {
        return new WebPageResponse<>(
                page.getRecords(),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize(),
                page.getPages(),
                page.isTotalKnown()
        );
    }
}
