package net.ximatai.muyun.spring.ability.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record QueryRequest(List<QueryCondition> conditions,
                           QueryCriteria criteria,
                           Map<String, Object> queryForm,
                           List<QuerySort> sorts,
                           String uiConfigId,
                           String queryTemplateId,
                           Map<String, Object> externalQueryValues,
                           String quickSearch,
                           List<String> quickSearchFields,
                           boolean navigationSession,
                           String navigationQueryKey) {
    public QueryRequest {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        queryForm = queryForm == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(queryForm));
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        externalQueryValues = externalQueryValues == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(externalQueryValues));
        quickSearchFields = quickSearchFields == null ? List.of() : List.copyOf(quickSearchFields);
    }

    public static QueryRequest empty() {
        return new QueryRequest(List.of(), null, Map.of(), List.of(), null, null,
                Map.of(), null, List.of(), false, null);
    }
}
