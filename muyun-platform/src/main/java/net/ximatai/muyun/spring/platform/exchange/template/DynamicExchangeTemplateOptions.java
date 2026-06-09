package net.ximatai.muyun.spring.platform.exchange.template;

import java.util.List;
import java.util.Set;

public record DynamicExchangeTemplateOptions(
        Set<String> disabledReferenceDropdownFields,
        int referenceDropdownLimit
) {
    public static final int DEFAULT_REFERENCE_DROPDOWN_LIMIT = 500;
    public static final DynamicExchangeTemplateOptions DEFAULT =
            new DynamicExchangeTemplateOptions(Set.of(), DEFAULT_REFERENCE_DROPDOWN_LIMIT);

    public DynamicExchangeTemplateOptions {
        disabledReferenceDropdownFields = disabledReferenceDropdownFields == null
                ? Set.of()
                : Set.copyOf(disabledReferenceDropdownFields);
        referenceDropdownLimit = referenceDropdownLimit <= 0
                ? DEFAULT_REFERENCE_DROPDOWN_LIMIT
                : referenceDropdownLimit;
    }

    public static DynamicExchangeTemplateOptions of(List<String> disabledReferenceDropdownFields,
                                                    Integer referenceDropdownLimit) {
        return new DynamicExchangeTemplateOptions(
                disabledReferenceDropdownFields == null ? Set.of() : Set.copyOf(disabledReferenceDropdownFields),
                referenceDropdownLimit == null ? DEFAULT_REFERENCE_DROPDOWN_LIMIT : referenceDropdownLimit
        );
    }

    boolean referenceDropdownEnabled(String entityAlias, String fieldName) {
        return !disabledReferenceDropdownFields.contains(entityAlias + "." + fieldName)
                && !disabledReferenceDropdownFields.contains(fieldName);
    }
}
