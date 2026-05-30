package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
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
                .filter(view -> entity.code().equals(view.entityCode()))
                .toList();
        if (scoped.isEmpty()) {
            return List.of(defaultView(entity, EntityViewType.LIST), defaultView(entity, EntityViewType.FORM));
        }
        Map<String, FieldDefinition> fields = entity.fields().stream()
                .collect(Collectors.toMap(FieldDefinition::fieldName, Function.identity()));
        return scoped.stream()
                .map(view -> from(view, fields))
                .toList();
    }

    private static DynamicViewDescriptor defaultView(EntityDefinition entity, EntityViewType type) {
        return new DynamicViewDescriptor(
                type,
                entity.name(),
                entity.fields().stream()
                        .map(field -> new DynamicViewFieldDescriptor(field.fieldName(), field.name(), true, controlType(field)))
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
                viewField.controlType() == null ? controlType(field) : viewField.controlType()
        );
    }

    private static ViewControlType controlType(FieldDefinition field) {
        if (field.optionBinding() != null) {
            return ViewControlType.SELECT;
        }
        return switch (field.type()) {
            case STRING -> ViewControlType.TEXT;
            case TEXT -> ViewControlType.TEXTAREA;
            case INTEGER, LONG -> ViewControlType.NUMBER;
            case BOOLEAN -> ViewControlType.SWITCH;
            case DATE -> ViewControlType.DATE;
            case TIMESTAMP -> ViewControlType.DATETIME;
            case DECIMAL -> ViewControlType.DECIMAL;
            case JSON -> ViewControlType.JSON;
        };
    }
}
