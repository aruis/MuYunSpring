package net.ximatai.muyun.spring.platform.exchange.model;

import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.List;

public record ParsedWorkbook(
        ExcelWorkbookMeta meta,
        List<ParsedSheet> sheets
) {
    public ParsedWorkbook {
        if (sheets == null || sheets.isEmpty()) {
            throw new PlatformException("parsed sheets must not be empty");
        }
        sheets = List.copyOf(sheets);
    }

    public ParsedWorkbook(List<ParsedSheet> sheets) {
        this(null, sheets);
    }
}
