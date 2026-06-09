package net.ximatai.muyun.spring.boot.dynamic;

import java.util.List;

public record DynamicExchangeTemplateRequest(
        List<String> disabledReferenceDropdownFields,
        Integer referenceDropdownLimit
) {
}
