package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public record ActionExecutionContext(
        String moduleAlias,
        String actionCode,
        PlatformAction platformAction,
        ActionExecutionPolicy actionPolicy,
        String permissionCode,
        Set<String> recordIds,
        Optional<CurrentUser> currentUser
) {
    public ActionExecutionContext {
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        actionCode = requireText(actionCode, "actionCode");
        actionPolicy = actionPolicy == null
                ? defaultPolicy(actionCode, platformAction)
                : actionPolicy;
        permissionCode = requireText(permissionCode, "permissionCode");
        recordIds = recordIds == null ? Set.of() : Set.copyOf(recordIds);
        currentUser = currentUser == null ? Optional.empty() : currentUser;
    }

    public static ActionExecutionContext ofPlatformAction(String moduleAlias,
                                                          PlatformAction action,
                                                          Set<String> recordIds,
                                                          Optional<CurrentUser> currentUser) {
        java.util.Objects.requireNonNull(action, "action must not be null");
        ActionExecutionPolicy policy = action.executionPolicy();
        return new ActionExecutionContext(
                moduleAlias,
                action.code(),
                action,
                policy,
                PlatformPermissionCode.action(moduleAlias, policy.permissionActionCode()),
                normalizeRecordIds(recordIds),
                currentUser
        );
    }

    public static ActionExecutionContext ofActionCode(String moduleAlias,
                                                      String actionCode,
                                                      Set<String> recordIds,
                                                      Optional<CurrentUser> currentUser) {
        PlatformAction action = PlatformAction.fromCode(actionCode).orElse(null);
        ActionExecutionPolicy policy = defaultPolicy(actionCode, action);
        return new ActionExecutionContext(
                moduleAlias,
                actionCode,
                action,
                policy,
                PlatformPermissionCode.action(moduleAlias, policy.permissionActionCode()),
                normalizeRecordIds(recordIds),
                currentUser
        );
    }

    public static ActionExecutionContext ofPolicy(String moduleAlias,
                                                  ActionExecutionPolicy policy,
                                                  Set<String> recordIds,
                                                  Optional<CurrentUser> currentUser) {
        java.util.Objects.requireNonNull(policy, "policy must not be null");
        return new ActionExecutionContext(
                moduleAlias,
                policy.actionCode(),
                PlatformAction.fromCode(policy.actionCode()).orElse(null),
                policy,
                PlatformPermissionCode.action(moduleAlias, policy.permissionActionCode()),
                normalizeRecordIds(recordIds),
                currentUser
        );
    }

    public boolean hasRecordContext() {
        return !recordIds.isEmpty();
    }

    public ActionExecutionContext withRecordIds(Set<String> recordIds) {
        return new ActionExecutionContext(
                moduleAlias,
                actionCode,
                platformAction,
                actionPolicy,
                permissionCode,
                normalizeRecordIds(recordIds),
                currentUser
        );
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

    private static ActionExecutionPolicy defaultPolicy(String actionCode, PlatformAction action) {
        if (action != null) {
            return action.executionPolicy();
        }
        return new ActionExecutionPolicy(
                actionCode,
                PlatformActionLevel.DEFAULT,
                ActionAccessMode.AUTH_REQUIRED,
                true,
                false,
                ActionDefaultGrantPolicy.NONE,
                null
        );
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
