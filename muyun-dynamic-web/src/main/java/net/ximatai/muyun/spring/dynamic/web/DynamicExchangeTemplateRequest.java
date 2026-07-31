package net.ximatai.muyun.spring.dynamic.web;

import java.util.List;

public record DynamicExchangeTemplateRequest(
        List<String> disabledReferenceDropdownFields,
        Integer referenceDropdownLimit
) {
}
