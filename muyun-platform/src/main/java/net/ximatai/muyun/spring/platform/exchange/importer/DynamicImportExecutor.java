package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordActionGateway;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicImportExecutor {
    private static final PageRequest MATCH_PAGE = PageRequest.of(1, 2);
    private static final String IMPORT_TRACE_ID = "dynamic-import";

    private final DynamicRecordService recordService;

    public DynamicImportExecutor(DynamicRecordService recordService) {
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
    }

    public DynamicImportExecutionResult execute(ExecuteDynamicImportCommand command) {
        if (command == null) {
            throw new PlatformException("dynamic import execution command must not be null");
        }
        DynamicImportPlan plan = command.plan();
        GroupedWorkbook workbook = command.workbook();
        List<ImportErrorRow> errorRows = new ArrayList<>(workbook.errorRows());
        Map<String, ImportEntityExecutionSummary> summaries = initialSummaries(plan);
        Map<String, DynamicImportPlan.SheetPlan> sheetPlansByKey = sheetPlansByKey(plan);
        Map<String, DynamicRelationDescriptor> mainChildRelations = mainChildRelations(plan);
        DynamicRecordActionGateway records = recordService.recordsForAction(
                plan.moduleAlias(), PlatformAction.IMPORT, IMPORT_TRACE_ID);

        DynamicImportPlan.SheetPlan mainSheet = plan.mainSheet();
        for (ImportGroup group : workbook.groups().values()) {
            WriteDecision mainDecision = executeRow(records, mainSheet, group.mainRow(), null, errorRows,
                    group.groupKey(), summaries);
            if (!mainDecision.success()) {
                continue;
            }
            for (Map.Entry<String, List<ParsedImportRow>> entry : group.childRowsBySheetKey().entrySet()) {
                DynamicImportPlan.SheetPlan childSheet = sheetPlansByKey.get(entry.getKey());
                if (childSheet == null) {
                    continue;
                }
                DynamicRelationDescriptor relation = mainChildRelations.get(childSheet.entityAlias());
                if (relation == null) {
                    for (ParsedImportRow childRow : entry.getValue()) {
                        addError(childSheet.entityAlias(), summaries, errorRows,
                                ImportErrorRow.of(childRow, "子表未找到主子关系: " + childSheet.entityAlias(),
                                        group.groupKey()));
                    }
                    continue;
                }
                for (ParsedImportRow childRow : entry.getValue()) {
                    executeRow(records, childSheet, childRow,
                            new ParentContext(relation.childForeignKeyField(), mainDecision.recordId()),
                            errorRows, group.groupKey(), summaries);
                }
            }
        }
        return DynamicImportExecutionResult.of(summaries, errorRows);
    }

    private WriteDecision executeRow(DynamicRecordActionGateway records,
                                     DynamicImportPlan.SheetPlan sheet,
                                     ParsedImportRow row,
                                     ParentContext parent,
                                     List<ImportErrorRow> errorRows,
                                     String groupIdentity,
                                     Map<String, ImportEntityExecutionSummary> summaries) {
        Object matchValue = row.convertedValues().get(sheet.matchFieldName());
        Criteria criteria = Criteria.of().eq(sheet.matchFieldName(), matchValue);
        if (parent != null) {
            criteria.eq(parent.foreignKeyField(), parent.parentId());
        }
        List<DynamicRecord> existing = records.list(sheet.entityAlias(), criteria, MATCH_PAGE);
        if (existing.size() > 1) {
            addError(sheet.entityAlias(), summaries, errorRows,
                    ImportErrorRow.of(row, "导入匹配到多条已有记录: " + sheet.matchFieldName(), groupIdentity));
            return WriteDecision.failed();
        }
        if (existing.size() == 1) {
            DynamicRecord existingRecord = existing.getFirst();
            return switch (sheet.duplicateStrategy()) {
                case ERROR -> {
                    addError(sheet.entityAlias(), summaries, errorRows,
                            ImportErrorRow.of(row, "导入匹配到已有记录: " + sheet.matchFieldName(), groupIdentity));
                    yield WriteDecision.failed();
                }
                case SKIP -> {
                    addSkipped(sheet.entityAlias(), summaries);
                    yield WriteDecision.success(existingRecord.getId());
                }
                case OVERWRITE -> {
                    DynamicRecord record = buildRecord(records, sheet, row, parent);
                    record.setId(existingRecord.getId());
                    record.setVersion(existingRecord.getVersion());
                    records.update(sheet.entityAlias(), record);
                    addUpdated(sheet.entityAlias(), summaries);
                    yield WriteDecision.success(existingRecord.getId());
                }
            };
        }

        DynamicRecord record = buildRecord(records, sheet, row, parent);
        String id = records.create(sheet.entityAlias(), record);
        addCreated(sheet.entityAlias(), summaries);
        return WriteDecision.success(id);
    }

    private DynamicRecord buildRecord(DynamicRecordActionGateway records,
                                      DynamicImportPlan.SheetPlan sheet,
                                      ParsedImportRow row,
                                      ParentContext parent) {
        DynamicRecord record = records.newRecord(sheet.entityAlias());
        for (DynamicImportPlan.FieldPlan field : sheet.fields()) {
            if (field.relateId() || field.companion()) {
                continue;
            }
            if (row.convertedValues().containsKey(field.fieldName())) {
                record.setValue(field.fieldName(), row.convertedValues().get(field.fieldName()));
            }
        }
        if (parent != null) {
            record.setValue(parent.foreignKeyField(), parent.parentId());
        }
        return record;
    }

    private Map<String, ImportEntityExecutionSummary> initialSummaries(DynamicImportPlan plan) {
        LinkedHashMap<String, ImportEntityExecutionSummary> summaries = new LinkedHashMap<>();
        for (DynamicImportPlan.SheetPlan sheet : plan.sheets()) {
            summaries.putIfAbsent(sheet.entityAlias(), new ImportEntityExecutionSummary(sheet.entityAlias(), 0, 0, 0, 0));
        }
        return summaries;
    }

    private Map<String, DynamicImportPlan.SheetPlan> sheetPlansByKey(DynamicImportPlan plan) {
        return plan.sheets().stream()
                .collect(Collectors.toMap(
                        DynamicImportPlan.SheetPlan::sheetKey,
                        Function.identity(),
                        (left, right) -> {
                            throw new PlatformException("dynamic import sheet duplicated: " + left.sheetKey());
                        },
                        LinkedHashMap::new
                ));
    }

    private Map<String, DynamicRelationDescriptor> mainChildRelations(DynamicImportPlan plan) {
        String mainEntityAlias = plan.mainSheet().entityAlias();
        return recordService.relations(plan.moduleAlias()).stream()
                .filter(relation -> relation != null)
                .filter(relation -> Objects.equals(relation.parentEntityAlias(), mainEntityAlias))
                .collect(Collectors.toMap(
                        DynamicRelationDescriptor::childEntityAlias,
                        Function.identity(),
                        (left, right) -> {
                            throw new PlatformException("dynamic import child relation duplicated: "
                                    + left.childEntityAlias());
                        },
                        LinkedHashMap::new
                ));
    }

    private void addCreated(String entityAlias, Map<String, ImportEntityExecutionSummary> summaries) {
        summaries.compute(entityAlias, (key, summary) -> currentSummary(entityAlias, summary).addCreated());
    }

    private void addUpdated(String entityAlias, Map<String, ImportEntityExecutionSummary> summaries) {
        summaries.compute(entityAlias, (key, summary) -> currentSummary(entityAlias, summary).addUpdated());
    }

    private void addSkipped(String entityAlias, Map<String, ImportEntityExecutionSummary> summaries) {
        summaries.compute(entityAlias, (key, summary) -> currentSummary(entityAlias, summary).addSkipped());
    }

    private void addError(String entityAlias,
                          Map<String, ImportEntityExecutionSummary> summaries,
                          List<ImportErrorRow> errorRows,
                          ImportErrorRow errorRow) {
        summaries.compute(entityAlias, (key, summary) -> currentSummary(entityAlias, summary).addError());
        errorRows.add(errorRow);
    }

    private ImportEntityExecutionSummary currentSummary(String entityAlias, ImportEntityExecutionSummary summary) {
        return summary == null ? new ImportEntityExecutionSummary(entityAlias, 0, 0, 0, 0) : summary;
    }

    private record ParentContext(String foreignKeyField, String parentId) {
    }

    private record WriteDecision(boolean success, String recordId) {
        static WriteDecision success(String recordId) {
            return new WriteDecision(true, recordId);
        }

        static WriteDecision failed() {
            return new WriteDecision(false, null);
        }
    }
}
