package net.ximatai.muyun.spring.platform.exchange.exporter;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicFieldCompanionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicFieldDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelColumnPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelSheetPlan;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelValueType;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookMeta;
import net.ximatai.muyun.spring.platform.exchange.model.ExcelWorkbookPlan;
import net.ximatai.muyun.spring.platform.exchange.protocol.ExcelExchangeProtocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicExportWorkbookPlanBuilder {
    public ExcelWorkbookPlan build(DynamicModuleDescriptor descriptor) {
        validateModule(descriptor);
        Map<String, DynamicEntityDescriptor> entitiesByAlias = entitiesByAlias(descriptor);
        DynamicEntityDescriptor mainEntity = requireEntity(entitiesByAlias, descriptor.mainEntityAlias(),
                "dynamic export main entity not found");

        List<ExcelSheetPlan> sheets = new ArrayList<>();
        sheets.add(buildSheet(mainEntity, true, true));
        Set<String> childEntityAliases = new LinkedHashSet<>();
        for (DynamicRelationDescriptor relation : descriptor.relations()) {
            if (!descriptor.mainEntityAlias().equals(relation.parentEntityAlias())) {
                continue;
            }
            if (!childEntityAliases.add(relation.childEntityAlias())) {
                continue;
            }
            DynamicEntityDescriptor childEntity = requireEntity(entitiesByAlias, relation.childEntityAlias(),
                    "dynamic export child entity not found");
            sheets.add(buildSheet(childEntity, false, false));
        }

        return new ExcelWorkbookPlan(new ExcelWorkbookMeta(
                ExcelExchangeProtocol.PROTOCOL_VERSION,
                descriptor.moduleAlias(),
                null,
                null,
                null
        ), sheets);
    }

    private void validateModule(DynamicModuleDescriptor descriptor) {
        if (descriptor == null) {
            throw new PlatformException("dynamic export requires module descriptor");
        }
        if (isBlank(descriptor.moduleAlias())) {
            throw new PlatformException("dynamic export requires moduleAlias");
        }
        if (isBlank(descriptor.mainEntityAlias())) {
            throw new PlatformException("dynamic export requires mainEntityAlias");
        }
    }

    private Map<String, DynamicEntityDescriptor> entitiesByAlias(DynamicModuleDescriptor descriptor) {
        if (descriptor.entities().isEmpty()) {
            throw new PlatformException("dynamic export requires module entities");
        }
        return descriptor.entities().stream()
                .collect(Collectors.toMap(
                        DynamicEntityDescriptor::entityAlias,
                        Function.identity(),
                        (left, right) -> {
                            throw new PlatformException("dynamic export entity duplicated: " + left.entityAlias());
                        },
                        LinkedHashMap::new
                ));
    }

    private DynamicEntityDescriptor requireEntity(Map<String, DynamicEntityDescriptor> entitiesByAlias,
                                                  String entityAlias,
                                                  String message) {
        DynamicEntityDescriptor entity = entitiesByAlias.get(entityAlias);
        if (entity == null) {
            throw new PlatformException(message + ": " + entityAlias);
        }
        return entity;
    }

    private ExcelSheetPlan buildSheet(DynamicEntityDescriptor entity, boolean main, boolean requireBusinessFields) {
        List<ExcelColumnPlan> columns = buildColumns(entity);
        if (requireBusinessFields && columns.size() == 1) {
            throw new PlatformException("dynamic export main sheet requires business fields: " + entity.entityAlias());
        }
        return new ExcelSheetPlan(sheetNameOf(entity), entity.entityAlias(), main, columns);
    }

    private List<ExcelColumnPlan> buildColumns(DynamicEntityDescriptor entity) {
        Set<String> companionFieldNames = companionFieldNames(entity);
        List<ExcelColumnPlan> columns = new ArrayList<>();
        columns.add(new ExcelColumnPlan(
                ExcelExchangeProtocol.RELATE_ID_FIELD,
                ExcelExchangeProtocol.RELATE_ID_TITLE,
                false,
                ExcelValueType.TEXT,
                List.of()
        ));
        for (DynamicFieldDescriptor field : entity.fields()) {
            if (field == null || isBlank(field.fieldName()) || companionFieldNames.contains(field.fieldName())) {
                continue;
            }
            columns.add(new ExcelColumnPlan(
                    field.fieldName(),
                    titleOf(field),
                    field.required(),
                    valueTypeOf(field.type()),
                    dropdownOptions()
            ));
        }
        return columns;
    }

    private Set<String> companionFieldNames(DynamicEntityDescriptor entity) {
        Set<String> fieldNames = new LinkedHashSet<>();
        for (DynamicFieldDescriptor field : entity.fields()) {
            if (field == null || field.companions() == null) {
                continue;
            }
            for (DynamicFieldCompanionDescriptor companion : field.companions()) {
                if (companion != null && !isBlank(companion.fieldName())) {
                    fieldNames.add(companion.fieldName());
                }
            }
        }
        return fieldNames;
    }

    private String titleOf(DynamicFieldDescriptor field) {
        return isBlank(field.title()) ? field.fieldName() : field.title();
    }

    private String sheetNameOf(DynamicEntityDescriptor entity) {
        return isBlank(entity.title()) ? entity.entityAlias() : entity.title();
    }

    private ExcelValueType valueTypeOf(FieldType type) {
        if (type == null) {
            return ExcelValueType.TEXT;
        }
        return switch (type) {
            case STRING, TEXT, JSON -> ExcelValueType.TEXT;
            case INTEGER, LONG, DECIMAL -> ExcelValueType.NUMBER;
            case BOOLEAN -> ExcelValueType.BOOLEAN;
            case DATE -> ExcelValueType.DATE;
            case TIMESTAMP -> ExcelValueType.DATE_TIME;
            case ZONED_TIMESTAMP -> ExcelValueType.DATE_TIME_WITH_TIME_ZONE;
        };
    }

    private List<String> dropdownOptions() {
        return List.of();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
