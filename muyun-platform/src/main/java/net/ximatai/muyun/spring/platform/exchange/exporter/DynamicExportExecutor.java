package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordActionGateway;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicExportExecutor {
    private static final String EXPORT_TRACE_ID = "dynamic-export";

    private final DynamicRecordService recordService;
    private final DynamicExchangeTemplatePlanBuilder templatePlanBuilder;

    public DynamicExportExecutor(DynamicRecordService recordService,
                                 DynamicExchangeTemplatePlanBuilder templatePlanBuilder) {
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
        this.templatePlanBuilder = Objects.requireNonNull(templatePlanBuilder, "templatePlanBuilder must not be null");
    }

    public ExcelWorkbookPlan export(DynamicExportCommand command) {
        DynamicExportCommand normalized = Objects.requireNonNull(command, "command must not be null");
        ExcelWorkbookPlan template = templatePlanBuilder.build(normalized.descriptor());
        DynamicRecordActionGateway records = recordService.recordsForAction(
                normalized.descriptor().moduleAlias(), PlatformAction.EXPORT, EXPORT_TRACE_ID);
        List<DynamicRecord> mainRecords = records.list(
                normalized.descriptor().mainEntityAlias(),
                normalized.criteria(),
                normalized.pageRequest(),
                normalized.sortArray()
        );
        return new ExcelWorkbookPlan(template.meta(), withMainRows(template.sheets(), mainRecords));
    }

    private List<ExcelSheetPlan> withMainRows(List<ExcelSheetPlan> sheets, List<DynamicRecord> mainRecords) {
        List<ExcelSheetPlan> result = new ArrayList<>();
        for (ExcelSheetPlan sheet : sheets) {
            if (!sheet.main()) {
                result.add(sheet);
                continue;
            }
            result.add(new ExcelSheetPlan(
                    sheet.sheetName(),
                    sheet.entityAlias(),
                    true,
                    sheet.columns(),
                    rows(sheet.columns(), mainRecords)
            ));
        }
        return result;
    }

    private List<List<Object>> rows(List<ExcelColumnPlan> columns, List<DynamicRecord> records) {
        return records.stream()
                .map(record -> row(columns, record))
                .toList();
    }

    private List<Object> row(List<ExcelColumnPlan> columns, DynamicRecord record) {
        Map<String, Object> values = record.getValues();
        return columns.stream()
                .map(column -> value(column, record, values))
                .toList();
    }

    private Object value(ExcelColumnPlan column, DynamicRecord record, Map<String, Object> values) {
        if (ExcelExchangeProtocol.RELATE_ID_FIELD.equals(column.fieldName())) {
            return record.getId();
        }
        return values.get(column.fieldName());
    }
}
