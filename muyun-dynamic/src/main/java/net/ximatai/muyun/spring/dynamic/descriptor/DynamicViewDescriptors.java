package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DynamicViewDescriptors {
    private DynamicViewDescriptors() {
    }

    static List<DynamicViewDescriptor> from(EntityDefinition entity, List<EntityViewDefinition> views) {
        List<EntityViewDefinition> scoped = views == null ? List.of() : views.stream()
                .filter(view -> entity.alias().equals(view.entityAlias()))
                .toList();
        if (scoped.isEmpty()) {
            return List.of(defaultView(entity, EntityViewType.LIST), defaultView(entity, EntityViewType.FORM));
        }
        Map<String, FieldDefinition> fields = entity.fields().stream()
                .collect(Collectors.toMap(FieldDefinition::fieldName, Function.identity()));
        Map<EntityViewType, EntityViewDefinition> configuredByType = scoped.stream()
                .collect(Collectors.toMap(EntityViewDefinition::viewType, Function.identity()));
        return List.of(
                viewOrDefault(entity, fields, configuredByType, EntityViewType.LIST),
                viewOrDefault(entity, fields, configuredByType, EntityViewType.FORM)
        );
    }

    private static DynamicViewDescriptor viewOrDefault(EntityDefinition entity,
                                                       Map<String, FieldDefinition> fields,
                                                       Map<EntityViewType, EntityViewDefinition> configuredByType,
                                                       EntityViewType type) {
        EntityViewDefinition view = configuredByType.get(type);
        return view == null ? defaultView(entity, type) : from(view, fields);
    }

    private static DynamicViewDescriptor defaultView(EntityDefinition entity, EntityViewType type) {
        return new DynamicViewDescriptor(
                type,
                entity.name(),
                entity.fields().stream()
                        .map(field -> new DynamicViewFieldDescriptor(field.fieldName(), field.name(), true,
                                controlType(field), field.defaultUiTypeAlias(), companions(field), false, field.isRequired()))
                        .toList()
        );
    }

    private static DynamicViewDescriptor from(EntityViewDefinition view, Map<String, FieldDefinition> fields) {
        return new DynamicViewDescriptor(
                view.viewType(),
                view.title(),
                view.fields().stream()
                        .map(field -> from(field, fields.get(field.fieldName())))
                        .toList()
        );
    }

    private static DynamicViewFieldDescriptor from(EntityViewFieldDefinition viewField, FieldDefinition field) {
        return new DynamicViewFieldDescriptor(
                viewField.fieldName(),
                viewField.title() == null || viewField.title().isBlank() ? field.name() : viewField.title(),
                viewField.visible(),
                effectiveControlType(viewField, field),
                effectiveFieldUiTypeAlias(viewField, field),
                companions(field),
                Boolean.TRUE.equals(viewField.readOnly()),
                field.isRequired() || Boolean.TRUE.equals(viewField.required())
        );
    }

    private static String effectiveFieldUiTypeAlias(EntityViewFieldDefinition viewField, FieldDefinition field) {
        return viewField.fieldUiTypeAlias() == null || viewField.fieldUiTypeAlias().isBlank()
                ? field.defaultUiTypeAlias()
                : viewField.fieldUiTypeAlias();
    }

    private static List<DynamicFieldCompanionDescriptor> companions(FieldDefinition field) {
        return FieldCompanionRules.group(field).stream()
                .flatMap(group -> group.companions().stream()
                        .map(companion -> DynamicFieldCompanionDescriptor.from(group.kind(), companion)))
                .toList();
    }

    private static ViewControlType effectiveControlType(EntityViewFieldDefinition viewField, FieldDefinition field) {
        if (field.dictionaryBinding() != null
                && field.dictionaryBinding().selectionMode() == OptionSelectionMode.MULTIPLE) {
            return ViewControlType.MULTI_SELECT;
        }
        return viewField.controlType() == null ? controlType(field) : viewField.controlType();
    }

    private static ViewControlType controlType(FieldDefinition field) {
        if (field.optionBinding() != null) {
            if (field.dictionaryBinding() != null
                    && field.dictionaryBinding().selectionMode() == OptionSelectionMode.MULTIPLE) {
                return ViewControlType.MULTI_SELECT;
            }
            return ViewControlType.SELECT;
        }
        return switch (field.type()) {
            case STRING -> ViewControlType.TEXT;
            case TEXT -> ViewControlType.TEXTAREA;
            case INTEGER, LONG -> ViewControlType.NUMBER;
            case BOOLEAN -> ViewControlType.SWITCH;
            case DATE -> ViewControlType.DATE;
            case TIMESTAMP, ZONED_TIMESTAMP -> ViewControlType.DATETIME;
            case DECIMAL -> ViewControlType.DECIMAL;
            case JSON -> ViewControlType.JSON;
        };
    }
}
