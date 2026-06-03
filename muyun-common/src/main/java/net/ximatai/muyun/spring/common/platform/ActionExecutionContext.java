package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public record ActionExecutionContext(
        String moduleAlias,
        String actionCode,
        PlatformAction platformAction,
        String permissionCode,
        Set<String> recordIds,
        Optional<CurrentUser> currentUser
) {
    public ActionExecutionContext {
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        actionCode = requireText(actionCode, "actionCode");
        permissionCode = requireText(permissionCode, "permissionCode");
        recordIds = recordIds == null ? Set.of() : Set.copyOf(recordIds);
        currentUser = currentUser == null ? Optional.empty() : currentUser;
    }

    public static ActionExecutionContext ofPlatformAction(String moduleAlias,
                                                          PlatformAction action,
                                                          Set<String> recordIds,
                                                          Optional<CurrentUser> currentUser) {
        java.util.Objects.requireNonNull(action, "action must not be null");
        return new ActionExecutionContext(
                moduleAlias,
                action.code(),
                action,
                PlatformPermissionCode.action(moduleAlias, action.code()),
                normalizeRecordIds(recordIds),
                currentUser
        );
    }

    public static ActionExecutionContext ofActionCode(String moduleAlias,
                                                      String actionCode,
                                                      Set<String> recordIds,
                                                      Optional<CurrentUser> currentUser) {
        return new ActionExecutionContext(
                moduleAlias,
                actionCode,
                PlatformAction.fromCode(actionCode).orElse(null),
                PlatformPermissionCode.action(moduleAlias, actionCode),
                normalizeRecordIds(recordIds),
                currentUser
        );
    }

    public boolean hasRecordContext() {
        return !recordIds.isEmpty();
    }

    private static Set<String> normalizeRecordIds(Set<String> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        recordIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
