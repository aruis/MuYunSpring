package net.ximatai.muyun.spring.iam.role;

import java.util.List;

public record RolePermissionMatrix(
        String roleId,
        List<Module> modules
) {
    public RolePermissionMatrix {
        modules = modules == null ? List.of() : List.copyOf(modules);
    }

    public record Module(
            String moduleAlias,
            List<RolePermissionAction> actions
    ) {
        public Module {
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }
}
