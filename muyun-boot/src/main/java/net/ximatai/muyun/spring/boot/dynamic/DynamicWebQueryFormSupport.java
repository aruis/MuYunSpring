package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

final class DynamicWebQueryFormSupport {
    private DynamicWebQueryFormSupport() {
    }

    static Criteria queryFormCriteria(String moduleAlias,
                                      WebQueryRequest request,
                                      PlatformPageConfigSnapshotService snapshotService,
                                      ModuleMetadataFieldService moduleFieldService,
                                      Function<List<DynamicQueryCondition>, Criteria> conditionCompiler) {
        if (request == null || request.queryForm().isEmpty()) {
            return Criteria.of();
        }
        Map<String, Object> effectiveValues = effectiveValues(request.queryForm());
        if (effectiveValues.isEmpty()) {
            return Criteria.of();
        }
        if (!hasText(request.uiConfigId())) {
            throw new PlatformException("Query form requires published LIST uiConfigId");
        }
        if (snapshotService == null || moduleFieldService == null) {
            throw new PlatformException("query form services are not configured");
        }
        PlatformPageConfigSnapshot snapshot = snapshotService.snapshot(moduleAlias);
        PlatformUiConfig uiConfig = publishedUiConfig(snapshot, request.uiConfigId());
        requireListUiConfig(snapshot, uiConfig);
        Set<String> allowedFields = visibleMainFields(snapshot, uiConfig, moduleFieldService);
        List<DynamicQueryCondition> conditions = new ArrayList<>();
        for (Map.Entry<String, Object> entry : effectiveValues.entrySet()) {
            String fieldName = normalizeKey(entry.getKey());
            if (!hasText(fieldName)) {
                continue;
            }
            if (!allowedFields.contains(fieldName)) {
                throw new PlatformException("Query form field is not available in UI config: " + fieldName);
            }
            List<?> values = values(entry.getValue());
            if (!values.isEmpty()) {
                conditions.add(new DynamicQueryCondition(fieldName, null, values));
            }
        }
        return conditions.isEmpty() ? Criteria.of() : conditionCompiler.apply(conditions);
    }

    private static Map<String, Object> effectiveValues(Map<String, Object> queryForm) {
        Map<String, Object> effective = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : queryForm.entrySet()) {
            if (!isEmptyValue(entry.getValue())) {
                effective.put(entry.getKey(), entry.getValue());
            }
        }
        return effective;
    }

    private static Set<String> visibleMainFields(PlatformPageConfigSnapshot snapshot,
                                                 PlatformUiConfig uiConfig,
                                                 ModuleMetadataFieldService moduleFieldService) {
        Set<String> fields = new LinkedHashSet<>();
        for (PlatformUiConfigField field : snapshot.uiFields()) {
            if (!Objects.equals(field.getUiConfigId(), uiConfig.getId())
                    || !Boolean.TRUE.equals(field.getVisible())) {
                continue;
            }
            ResolvedModuleMetadataField resolved = moduleFieldService.resolve(field.getModuleMetadataFieldId());
            if (resolved.relationRole() == RelationRole.MAIN) {
                fields.add(resolved.fieldName());
            }
        }
        return fields;
    }

    private static PlatformUiConfig publishedUiConfig(PlatformPageConfigSnapshot snapshot, String uiConfigId) {
        return snapshot.uiConfigs().stream()
                .filter(config -> Objects.equals(config.getId(), uiConfigId))
                .findFirst()
                .orElseThrow(() -> new PlatformException("UI config is not published in module snapshot: "
                        + uiConfigId));
    }

    private static void requireListUiConfig(PlatformPageConfigSnapshot snapshot, PlatformUiConfig uiConfig) {
        PlatformUiSet uiSet = snapshot.uiSets().stream()
                .filter(set -> Objects.equals(set.getId(), uiConfig.getUiSetId()))
                .findFirst()
                .orElseThrow(() -> new PlatformException("UI config set is not published in module snapshot: "
                        + uiConfig.getUiSetId()));
        if (uiSet.getSetType() != PlatformUiSetType.LIST) {
            throw new PlatformException("Query form requires LIST UI config: " + uiConfig.getId());
        }
    }

    private static List<?> values(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(item -> !isEmptyValue(item))
                    .toList();
        }
        if (value != null && value.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                Object item = Array.get(value, index);
                if (!isEmptyValue(item)) {
                    values.add(item);
                }
            }
            return values;
        }
        return List.of(value);
    }

    private static String normalizeKey(String key) {
        return key == null ? null : key.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        return false;
    }
}
