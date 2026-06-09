package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordActionGateway;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;
import net.ximatai.muyun.spring.platform.exchange.template.DynamicExchangeTemplatePlanBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class DynamicExportExecutor {
    private static final String EXPORT_TRACE_ID = "dynamic-export";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

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
        Map<String, DynamicRelationDescriptor> relationsByChild =
                firstLevelRelationsByChildEntity(normalized.descriptor().relations(), normalized.descriptor().mainEntityAlias());
        return new ExcelWorkbookPlan(template.meta(), withRows(template.sheets(), mainRecords, records, relationsByChild));
    }

    private List<ExcelSheetPlan> withRows(List<ExcelSheetPlan> sheets,
                                          List<DynamicRecord> mainRecords,
                                          DynamicRecordActionGateway records,
                                          Map<String, DynamicRelationDescriptor> relationsByChild) {
        List<ExcelSheetPlan> result = new ArrayList<>();
        for (ExcelSheetPlan sheet : sheets) {
            List<List<Object>> rows = sheet.main()
                    ? rows(sheet.columns(), mainRecords)
                    : childRows(sheet, mainRecords, records, relationsByChild.get(sheet.entityAlias()));
            result.add(new ExcelSheetPlan(
                    sheet.sheetName(),
                    sheet.entityAlias(),
                    sheet.main(),
                    sheet.columns(),
                    rows
            ));
        }
        return result;
    }

    private Map<String, DynamicRelationDescriptor> firstLevelRelationsByChildEntity(
            List<DynamicRelationDescriptor> relations,
            String mainEntityAlias) {
        Map<String, DynamicRelationDescriptor> result = new LinkedHashMap<>();
        for (DynamicRelationDescriptor relation : relations) {
            if (relation == null || !mainEntityAlias.equals(relation.parentEntityAlias())) {
                continue;
            }
            result.putIfAbsent(relation.childEntityAlias(), relation);
        }
        return Map.copyOf(result);
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

    private List<List<Object>> childRows(ExcelSheetPlan sheet,
                                         List<DynamicRecord> mainRecords,
                                         DynamicRecordActionGateway records,
                                         DynamicRelationDescriptor relation) {
        if (relation == null || mainRecords.isEmpty()) {
            return List.of();
        }
        Map<String, List<DynamicRecord>> childrenByParentId = childrenByParentId(records, relation, parentIds(mainRecords));
        List<List<Object>> rows = new ArrayList<>();
        for (DynamicRecord parent : mainRecords) {
            String parentId = parent.getId();
            List<DynamicRecord> children = childrenByParentId.get(parentId);
            if (children == null || children.isEmpty()) {
                rows.add(emptyChildRow(sheet.columns(), parentId));
                continue;
            }
            children.stream()
                    .map(child -> childRow(sheet.columns(), parentId, child))
                    .forEach(rows::add);
        }
        return List.copyOf(rows);
    }

    private Set<String> parentIds(List<DynamicRecord> mainRecords) {
        return mainRecords.stream()
                .map(DynamicRecord::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private Map<String, List<DynamicRecord>> childrenByParentId(DynamicRecordActionGateway records,
                                                                DynamicRelationDescriptor relation,
                                                                Collection<String> parentIds) {
        if (parentIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<DynamicRecord>> grouped = new LinkedHashMap<>();
        records.list(
                relation.childEntityAlias(),
                Criteria.of().in(relation.childForeignKeyField(), List.copyOf(parentIds)),
                ALL
        ).forEach(child -> {
            Object parentId = child.getValues().get(relation.childForeignKeyField());
            if (parentId != null) {
                grouped.computeIfAbsent(String.valueOf(parentId), ignored -> new ArrayList<>()).add(child);
            }
        });
        grouped.replaceAll((ignored, value) -> List.copyOf(value));
        return Map.copyOf(grouped);
    }

    private List<Object> childRow(List<ExcelColumnPlan> columns, String relateId, DynamicRecord child) {
        Map<String, Object> values = child.getValues();
        return columns.stream()
                .map(column -> childValue(column, relateId, values))
                .toList();
    }

    private Object childValue(ExcelColumnPlan column, String relateId, Map<String, Object> values) {
        if (ExcelExchangeProtocol.RELATE_ID_FIELD.equals(column.fieldName())) {
            return relateId;
        }
        return values.get(column.fieldName());
    }

    private List<Object> emptyChildRow(List<ExcelColumnPlan> columns, String relateId) {
        return columns.stream()
                .map(column -> ExcelExchangeProtocol.RELATE_ID_FIELD.equals(column.fieldName()) ? (Object) relateId : null)
                .toList();
    }

    private Object value(ExcelColumnPlan column, DynamicRecord record, Map<String, Object> values) {
        if (ExcelExchangeProtocol.RELATE_ID_FIELD.equals(column.fieldName())) {
            return record.getId();
        }
        return values.get(column.fieldName());
    }
}
