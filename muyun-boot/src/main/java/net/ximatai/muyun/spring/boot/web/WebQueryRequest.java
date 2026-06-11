package net.ximatai.muyun.spring.boot.web;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

public record WebQueryRequest(WebPageRequest page,
                              List<WebQueryCondition> conditions,
                              WebQueryCriteria criteria,
                              List<WebSort> sorts,
                              String uiConfigId,
                              String queryTemplateId,
                              Map<String, Object> externalQueryValues,
                              Boolean navigationSession,
                              String quickSearch,
                              List<String> quickSearchFields,
                              String navigationQueryKey) {
    public WebQueryRequest {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        externalQueryValues = externalQueryValues == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(externalQueryValues));
        quickSearchFields = quickSearchFields == null ? List.of() : List.copyOf(quickSearchFields);
    }

    public WebQueryRequest(WebPageRequest page,
                           List<WebQueryCondition> conditions,
                           List<WebSort> sorts) {
        this(page, conditions, null, sorts, null, null, Map.of(), null, null, List.of(), null);
    }

    public WebPageRequest pageOrDefault() {
        return page == null ? WebPageRequest.DEFAULT : page;
    }

    public boolean navigationSessionEnabled() {
        return Boolean.TRUE.equals(navigationSession);
    }
}
