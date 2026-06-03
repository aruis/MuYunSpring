package net.ximatai.muyun.spring.boot.web;

import java.util.List;

public record WebQueryCondition(String fieldName, String operator, List<Object> values) {
    public WebQueryCondition {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
