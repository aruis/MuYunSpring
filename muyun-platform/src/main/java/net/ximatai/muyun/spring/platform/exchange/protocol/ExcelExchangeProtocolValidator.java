package net.ximatai.muyun.spring.platform.exchange.protocol;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedColumn;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedSheet;
import net.ximatai.muyun.spring.platform.exchange.model.ParsedWorkbook;

import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class ExcelExchangeProtocolValidator {
    public void validateWorkbookPlan(ExcelWorkbookPlan workbookPlan) {
        if (workbookPlan == null || workbookPlan.sheets() == null || workbookPlan.sheets().isEmpty()) {
            throw new PlatformException("exchange workbook plan must not be empty");
        }
        int mainCount = 0;
        Set<String> sheetNames = new LinkedHashSet<>();
        for (ExcelSheetPlan sheet : workbookPlan.sheets()) {
            if (sheet == null) {
                throw new PlatformException("exchange workbook plan contains null sheet");
            }
            validateBusinessSheetName(sheet.sheetName());
            if (!sheetNames.add(sheet.sheetName())) {
                throw new PlatformException("exchange sheet name duplicated: " + sheet.sheetName());
            }
            if (sheet.main()) {
                mainCount++;
            }
            validatePlanColumns(sheet.sheetName(), sheet.columns());
            requireRelateId(sheet.sheetName(), sheet.columns());
        }
        if (mainCount != 1) {
            throw new PlatformException("exchange workbook plan must contain exactly one main sheet");
        }
    }

    public void validateParsedWorkbook(ParsedWorkbook parsedWorkbook) {
        if (parsedWorkbook == null || parsedWorkbook.sheets() == null || parsedWorkbook.sheets().isEmpty()) {
            throw new PlatformException("parsed exchange workbook must not be empty");
        }
        validateWorkbookMeta(parsedWorkbook.meta());
        Set<String> sheetNames = new LinkedHashSet<>();
        for (ParsedSheet sheet : parsedWorkbook.sheets()) {
            if (sheet == null) {
                throw new PlatformException("parsed exchange workbook contains null sheet");
            }
            validateBusinessSheetName(sheet.sheetName());
            if (!sheetNames.add(sheet.sheetName())) {
                throw new PlatformException("exchange sheet name duplicated: " + sheet.sheetName());
            }
            validateParsedColumns(sheet.sheetName(), sheet.columns());
            requireParsedRelateId(sheet.sheetName(), sheet.columns());
        }
    }

    public void validateWorkbookMeta(ExcelWorkbookMeta meta) {
        if (meta == null || meta.timeZone() == null || meta.timeZone().isBlank()) {
            return;
        }
        try {
            ZoneId.of(meta.timeZone());
        } catch (Exception ex) {
            throw new PlatformException("exchange workbook timeZone is invalid: " + meta.timeZone(), ex);
        }
    }

    private void validatePlanColumns(String sheetName, Iterable<ExcelColumnPlan> columns) {
        Set<String> fieldNames = new LinkedHashSet<>();
        for (ExcelColumnPlan column : columns) {
            if (column == null) {
                throw new PlatformException("exchange sheet contains null column: " + sheetName);
            }
            if (!fieldNames.add(column.fieldName())) {
                throw new PlatformException("exchange sheet field duplicated: " + sheetName + "." + column.fieldName());
            }
        }
    }

    private void validateBusinessSheetName(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) {
            throw new PlatformException("exchange sheet name must not be blank");
        }
        if (sheetName.length() > 31) {
            throw new PlatformException("exchange sheet name is too long: " + sheetName);
        }
        if (ExcelExchangeProtocol.isInternalSheet(sheetName)) {
            throw new PlatformException("exchange sheet name is reserved: " + sheetName);
        }
        if (sheetName.contains(":") || sheetName.contains("\\") || sheetName.contains("/")
                || sheetName.contains("?") || sheetName.contains("*")
                || sheetName.contains("[") || sheetName.contains("]")) {
            throw new PlatformException("exchange sheet name contains invalid character: " + sheetName);
        }
    }

    private void validateParsedColumns(String sheetName, Iterable<ParsedColumn> columns) {
        Set<String> fieldNames = new LinkedHashSet<>();
        for (ParsedColumn column : columns) {
            if (column == null) {
                throw new PlatformException("parsed exchange sheet contains null column: " + sheetName);
            }
            if (!fieldNames.add(column.fieldName())) {
                throw new PlatformException("exchange sheet field duplicated: " + sheetName + "." + column.fieldName());
            }
        }
    }

    private void requireRelateId(String sheetName, Iterable<ExcelColumnPlan> columns) {
        for (ExcelColumnPlan column : columns) {
            if (Objects.equals(ExcelExchangeProtocol.RELATE_ID_FIELD, column.fieldName())) {
                return;
            }
        }
        throw new PlatformException("exchange sheet requires __relateId technical column: " + sheetName);
    }

    private void requireParsedRelateId(String sheetName, Iterable<ParsedColumn> columns) {
        for (ParsedColumn column : columns) {
            if (Objects.equals(ExcelExchangeProtocol.RELATE_ID_FIELD, column.fieldName())) {
                return;
            }
        }
        throw new PlatformException("exchange sheet requires __relateId technical column: " + sheetName);
    }
}
