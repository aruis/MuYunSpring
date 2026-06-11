package net.ximatai.muyun.spring.common.platform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformActionTest {
    @Test
    void shouldMergeReadAndStatePermissionActionsWithoutChangingExecutionCodes() {
        assertThat(PlatformAction.VIEW.permissionActionCode()).isEqualTo("view");
        assertThat(PlatformAction.QUERY.permissionActionCode()).isEqualTo("view");
        assertThat(PlatformAction.TREE.permissionActionCode()).isEqualTo("view");
        assertThat(PlatformAction.REFERENCE.permissionActionCode()).isEqualTo("view");
        assertThat(PlatformAction.ENABLE.permissionActionCode()).isEqualTo("enable");
        assertThat(PlatformAction.DISABLE.permissionActionCode()).isEqualTo("enable");
        assertThat(PlatformAction.BATCH_DELETE.permissionActionCode()).isEqualTo("delete");
        assertThat(PlatformAction.VIEW.inheritActionCode()).isNull();
        assertThat(PlatformAction.QUERY.inheritActionCode()).isEqualTo("view");
        assertThat(PlatformAction.ENABLE.inheritActionCode()).isNull();
        assertThat(PlatformAction.DISABLE.inheritActionCode()).isEqualTo("enable");
        assertThat(PlatformAction.BATCH_DELETE.inheritActionCode()).isEqualTo("delete");

        assertThat(PlatformAction.QUERY.executionPolicy().actionCode()).isEqualTo("query");
        assertThat(PlatformAction.QUERY.executionPolicy().permissionActionCode()).isEqualTo("view");
        assertThat(PlatformAction.BATCH_DELETE.executionPolicy().actionCode()).isEqualTo("batchDelete");
        assertThat(PlatformAction.BATCH_DELETE.executionPolicy().permissionActionCode()).isEqualTo("delete");
        assertThat(PlatformAction.DISABLE.executionPolicy().actionCode()).isEqualTo("disable");
        assertThat(PlatformAction.DISABLE.executionPolicy().permissionActionCode()).isEqualTo("enable");
        assertThat(PlatformAction.permissionActionCodeOf("query")).isEqualTo("view");
        assertThat(PlatformAction.permissionActionCodeOf("batchDelete")).isEqualTo("delete");
        assertThat(PlatformAction.permissionActionCodeOf("disable")).isEqualTo("enable");
        assertThat(PlatformAction.permissionActionCodeOf("customSubmit")).isEqualTo("customSubmit");
        assertThat(ActionExecutionContext.ofPlatformAction("sales.contract", PlatformAction.QUERY,
                        java.util.Set.of(), java.util.Optional.empty()).permissionCode())
                .isEqualTo("sales.contract:view");
    }
}
