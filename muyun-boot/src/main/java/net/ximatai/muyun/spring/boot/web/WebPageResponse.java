package net.ximatai.muyun.spring.boot.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import net.ximatai.muyun.database.core.orm.PageResult;

import java.util.List;

public record WebPageResponse<T>(List<T> records,
                                 long total,
                                 int pageNum,
                                 int pageSize,
                                 long pages,
                                 boolean totalKnown,
                                 @JsonInclude(JsonInclude.Include.NON_NULL) Object navigation) {
    public static <T> WebPageResponse<T> from(PageResult<T> page) {
        return from(page, null);
    }

    public static <T> WebPageResponse<T> from(PageResult<T> page, Object navigation) {
        return new WebPageResponse<>(
                page.getRecords(),
                page.getTotal(),
                page.getPageNum(),
                page.getPageSize(),
                page.getPages(),
                page.isTotalKnown(),
                navigation
        );
    }
}
