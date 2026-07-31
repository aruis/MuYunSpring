package net.ximatai.muyun.spring.platform.module;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates invariants shared by every static-module registration source. */
public final class StaticModuleRegistrationValidator {
    private StaticModuleRegistrationValidator() {
    }

    public static void validate(List<? extends StaticModuleRegistration> definitions) {
        Set<String> modules = new HashSet<>();
        for (StaticModuleRegistration definition : definitions) {
            if (!modules.add(definition.moduleAlias())) {
                throw new IllegalStateException("duplicate static module definition: " + definition.moduleAlias());
            }
            Set<String> actions = new HashSet<>();
            for (StaticModuleActionDefinition action : definition.actions()) {
                if (!actions.add(action.actionCode())) {
                    throw new IllegalStateException("duplicate static module action definition: "
                            + definition.moduleAlias() + "." + action.actionCode());
                }
            }
        }
    }
}
