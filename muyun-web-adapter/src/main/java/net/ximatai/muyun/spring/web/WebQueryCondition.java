package net.ximatai.muyun.spring.web;

import java.util.List;

public record WebQueryCondition(String fieldName, String operator, List<Object> values, String timeZone) {
    public WebQueryCondition(String fieldName, String operator, List<Object> values) {
        this(fieldName, operator, values, null);
    }

    public WebQueryCondition {
        values = values == null ? List.of() : List.copyOf(values);
        timeZone = timeZone == null || timeZone.isBlank() ? null : timeZone.trim();
    }
}
