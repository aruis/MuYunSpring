package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicFormulaFieldDefinitions;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicFormulaDataSupport {
    private DynamicFormulaDataSupport() {
    }

    static List<FormulaFieldDefinition> fieldDefinitions(EntityDefinition entity, ModuleDefinition module) {
        List<FormulaFieldDefinition> definitions = new ArrayList<>(DynamicFormulaFieldDefinitions.mainFields(entity));
        if (module != null) {
            for (EntityRelationDefinition relation : module.relations()) {
                if (!entity.alias().equals(relation.parentEntityAlias())) {
                    continue;
                }
                module.entities().stream()
                        .filter(candidate -> relation.childEntityAlias().equals(candidate.alias()))
                        .findFirst()
                        .ifPresent(child -> definitions.addAll(
                                DynamicFormulaFieldDefinitions.childFields(relation.code(), child)
                        ));
            }
        }
        return List.copyOf(definitions);
    }

    static Map<String, Object> mainValues(DynamicRecord record, DynamicRecord existing) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (existing != null) {
            values.putAll(existing.getValues());
        }
        if (record != null) {
            values.putAll(record.getValues());
        }
        return values;
    }

    static Map<String, List<Map<String, Object>>> childValues(DynamicRecord record) {
        return childValues(record, Map.of());
    }

    static Map<String, List<Map<String, Object>>> childValues(DynamicRecord record,
                                                              Map<String, List<DynamicRecord>> existingChildren) {
        Map<String, List<Map<String, Object>>> values = new LinkedHashMap<>();
        if (record == null) {
            return values;
        }
        record.getChildren().forEach((relationCode, children) -> {
            if (children == null) {
                return;
            }
            Map<String, Map<String, Object>> existingById = existingChildrenById(existingChildren.get(relationCode));
            values.put(relationCode, children.stream()
                    .<Map<String, Object>>map(child -> mergedChildValues(child, existingById))
                    .toList());
        });
        return values;
    }

    private static Map<String, Map<String, Object>> existingChildrenById(List<DynamicRecord> children) {
        Map<String, Map<String, Object>> values = new LinkedHashMap<>();
        if (children == null) {
            return values;
        }
        for (DynamicRecord child : children) {
            if (child.getId() != null && !child.getId().isBlank()) {
                values.put(child.getId(), new LinkedHashMap<>(child.getValues()));
            }
        }
        return values;
    }

    private static Map<String, Object> mergedChildValues(DynamicRecord child, Map<String, Map<String, Object>> existingById) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (child.getId() != null && !child.getId().isBlank() && existingById.containsKey(child.getId())) {
            values.putAll(existingById.get(child.getId()));
        }
        values.putAll(child.getValues());
        return values;
    }
}
