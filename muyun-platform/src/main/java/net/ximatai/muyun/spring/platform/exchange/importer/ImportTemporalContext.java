package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedWorkbook;

import java.time.ZoneId;
import java.time.ZoneOffset;

public record ImportTemporalContext(ZoneId workbookZoneId) {
    public static final ImportTemporalContext UTC = new ImportTemporalContext(ZoneOffset.UTC);

    public ImportTemporalContext {
        if (workbookZoneId == null) {
            workbookZoneId = ZoneOffset.UTC;
        }
    }

    public static ImportTemporalContext from(ParsedWorkbook workbook) {
        ExcelWorkbookMeta meta = workbook == null ? null : workbook.meta();
        if (meta == null || meta.timeZone() == null || meta.timeZone().isBlank()) {
            return UTC;
        }
        try {
            return new ImportTemporalContext(ZoneId.of(meta.timeZone().trim()));
        } catch (RuntimeException ex) {
            throw new PlatformException("exchange workbook timeZone is invalid: " + meta.timeZone(), ex);
        }
    }
}
