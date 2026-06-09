package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicImportErrorWorkbookBuilder {
    public static final String ERROR_FIELD = "errorReason";
    public static final String ERROR_TITLE = "错误原因";

    public ExcelWorkbookPlan build(DynamicImportPlan plan, List<ImportErrorRow> errorRows) {
        return build(plan, errorRows, null);
    }

    public ExcelWorkbookPlan build(DynamicImportPlan plan,
                                   List<ImportErrorRow> errorRows,
                                   ExcelWorkbookMeta meta) {
        if (plan == null) {
            throw new PlatformException("dynamic import error workbook requires import plan");
        }
        List<ImportErrorRow> safeErrorRows = errorRows == null ? List.of() : new ArrayList<>(errorRows);
        Map<String, List<ImportErrorRow>> errorRowsBySheetKey = groupErrorRowsBySheetKey(plan, safeErrorRows);

        List<ExcelSheetPlan> sheets = new ArrayList<>();
        for (DynamicImportPlan.SheetPlan sheet : plan.sheets()) {
            List<ExcelColumnPlan> columns = buildColumns(sheet);
            List<List<Object>> rows = errorRowsBySheetKey.getOrDefault(sheet.sheetKey(), List.of()).stream()
                    .map(errorRow -> toWorkbookRow(sheet, errorRow))
                    .toList();
            sheets.add(new ExcelSheetPlan(
                    sheet.sheetName(),
                    sheet.entityAlias(),
                    sheet.main(),
                    columns,
                    rows
            ));
        }
        return new ExcelWorkbookPlan(meta == null ? defaultMeta(plan) : meta, sheets);
    }

    private Map<String, List<ImportErrorRow>> groupErrorRowsBySheetKey(DynamicImportPlan plan,
                                                                       List<ImportErrorRow> errorRows) {
        Set<String> knownSheetKeys = new LinkedHashSet<>();
        for (DynamicImportPlan.SheetPlan sheet : plan.sheets()) {
            knownSheetKeys.add(sheet.sheetKey());
        }

        Map<String, List<ImportErrorRow>> errorRowsBySheetKey = new LinkedHashMap<>();
        for (ImportErrorRow errorRow : errorRows) {
            if (errorRow == null) {
                continue;
            }
            if (!knownSheetKeys.contains(errorRow.sheetKey())) {
                throw new PlatformException("dynamic import error row sheetKey does not belong to plan: "
                        + errorRow.sheetKey());
            }
            errorRowsBySheetKey.computeIfAbsent(errorRow.sheetKey(), ignored -> new ArrayList<>()).add(errorRow);
        }
        return errorRowsBySheetKey;
    }

    private List<ExcelColumnPlan> buildColumns(DynamicImportPlan.SheetPlan sheet) {
        List<ExcelColumnPlan> columns = new ArrayList<>();
        for (DynamicImportPlan.FieldPlan field : sheet.fields()) {
            if (ERROR_FIELD.equals(field.fieldName())) {
                throw new PlatformException("dynamic import error workbook field conflicts with error column: "
                        + sheet.sheetKey() + "." + ERROR_FIELD);
            }
            columns.add(new ExcelColumnPlan(field.fieldName(), field.title()));
        }
        columns.add(new ExcelColumnPlan(ERROR_FIELD, ERROR_TITLE));
        return columns;
    }

    private List<Object> toWorkbookRow(DynamicImportPlan.SheetPlan sheet, ImportErrorRow errorRow) {
        List<Object> row = new ArrayList<>();
        for (DynamicImportPlan.FieldPlan field : sheet.fields()) {
            row.add(errorRow.rawValues().get(field.title()));
        }
        row.add(errorRow.message());
        return row;
    }

    private ExcelWorkbookMeta defaultMeta(DynamicImportPlan plan) {
        return new ExcelWorkbookMeta(
                ExcelExchangeProtocol.PROTOCOL_VERSION,
                plan.moduleAlias(),
                plan.planSource(),
                null,
                null
        );
    }
}
