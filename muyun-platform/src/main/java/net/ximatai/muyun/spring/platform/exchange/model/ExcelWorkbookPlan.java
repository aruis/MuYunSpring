package net.ximatai.muyun.spring.platform.exchange.model;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.List;

public record ExcelWorkbookPlan(
        ExcelWorkbookMeta meta,
        List<ExcelSheetPlan> sheets
) {
    public ExcelWorkbookPlan {
        if (sheets == null || sheets.isEmpty()) {
            throw new PlatformException("workbook sheets must not be empty");
        }
        sheets = List.copyOf(sheets);
    }

    public ExcelWorkbookPlan(List<ExcelSheetPlan> sheets) {
        this(null, sheets);
    }
}
