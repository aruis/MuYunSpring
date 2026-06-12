package net.ximatai.muyun.spring.iam.role;

import java.util.Objects;

public record EffectiveRoleActionGrant(
        RoleAction actionGrant,
        EffectiveRoleGrant roleGrant
) {
    public EffectiveRoleActionGrant {
        actionGrant = Objects.requireNonNull(actionGrant, "actionGrant must not be null");
    }
}
