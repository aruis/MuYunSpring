package net.ximatai.muyun.spring.iam.role;

import java.util.List;

public record RoleDataGrantActionMatrix(String roleId, List<Action> actions) {
    public RoleDataGrantActionMatrix {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public record Action(String actionCode, String title, boolean configured, DataScopePolicy dataScopePolicy) {
    }
}
