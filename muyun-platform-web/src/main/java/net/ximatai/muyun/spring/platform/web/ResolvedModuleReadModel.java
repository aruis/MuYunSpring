package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

public record ResolvedModuleReadModel(String moduleAlias,
                                      String mainEntityAlias,
                                      List<ResolvedModuleReadField> fields) {
    public ResolvedModuleReadModel {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        mainEntityAlias = mainEntityAlias == null || mainEntityAlias.isBlank() ? null : mainEntityAlias.trim();
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
