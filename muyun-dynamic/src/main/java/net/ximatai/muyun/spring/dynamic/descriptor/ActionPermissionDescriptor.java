package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.platform.PlatformPermissionCode;

import java.util.Objects;

public record ActionPermissionDescriptor(
        String permissionCode,
        boolean actionAuth,
        boolean dataAuth,
        String inheritActionCode,
        String inheritPermissionCode
) {
    public ActionPermissionDescriptor {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("permissionCode must not be blank");
        }
        if (inheritActionCode != null && inheritActionCode.isBlank()) {
            throw new IllegalArgumentException("inheritActionCode must not be blank");
        }
        if (inheritPermissionCode != null && inheritPermissionCode.isBlank()) {
            throw new IllegalArgumentException("inheritPermissionCode must not be blank");
        }
    }

    public static ActionPermissionDescriptor of(String moduleAlias, DynamicActionDescriptor action) {
        Objects.requireNonNull(action, "action must not be null");
        String inheritPermissionCode = action.authInheritActionCode() == null
                ? null
                : PlatformPermissionCode.action(moduleAlias, action.authInheritActionCode());
        return new ActionPermissionDescriptor(
                PlatformPermissionCode.action(moduleAlias, action.code()),
                action.actionAuth(),
                action.dataAuth(),
                action.authInheritActionCode(),
                inheritPermissionCode
        );
    }
}
