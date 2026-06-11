package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.Map;
import java.util.Set;

public record LowCodeModuleTemplateInstantiationRequest(
        String applicationAlias,
        String moduleAlias,
        String title,
        Map<String, Object> parameters
) {
    public LowCodeModuleTemplateInstantiationRequest {
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        moduleAlias = PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
        title = normalize(title);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        rejectReservedParameters(parameters);
    }

    private static final Set<String> RESERVED_PARAMETERS = Set.of("applicationAlias", "module", "moduleAlias");

    private static void rejectReservedParameters(Map<String, Object> parameters) {
        for (String reserved : RESERVED_PARAMETERS) {
            if (parameters.containsKey(reserved)) {
                throw new IllegalArgumentException("template parameter is reserved: " + reserved);
            }
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
