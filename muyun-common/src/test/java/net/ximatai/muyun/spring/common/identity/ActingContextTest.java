package net.ximatai.muyun.spring.common.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActingContextTest {
    @Test
    void shouldRestorePreviousActingContext() {
        ActingContext outer = context("delegation-1", "sales.contract", "create");
        ActingContext inner = context("delegation-2", null, null);

        try (ActingContextHolder.Scope ignored = ActingContextHolder.use(outer)) {
            assertThat(ActingContextHolder.current()).contains(outer);
            try (ActingContextHolder.Scope ignoredInner = ActingContextHolder.use(inner)) {
                assertThat(ActingContextHolder.current()).contains(inner);
            }
            assertThat(ActingContextHolder.current()).contains(outer);
        }

        assertThat(ActingContextHolder.current()).isEmpty();
    }

    @Test
    void shouldMatchOptionalModuleAndAction() {
        ActingContext scoped = context("delegation-1", "sales.contract", "create");
        ActingContext general = context("delegation-2", null, null);

        assertThat(scoped.matches("sales.contract", "create")).isTrue();
        assertThat(scoped.matches("sales.contract", "update")).isFalse();
        assertThat(general.matches("sales.contract", "create")).isTrue();
    }

    @Test
    void shouldRequirePrincipalIdentity() {
        assertThatThrownBy(() -> new BusinessPrincipal(null, null, null, null, null,
                BusinessPrincipalSource.DELEGATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("business principal");
    }

    private ActingContext context(String delegationId, String moduleAlias, String actionCode) {
        return new ActingContext(
                delegationId,
                CurrentUser.tenantUser("assistant-user", "Assistant", "tenant-a"),
                BusinessPrincipal.employeePosition("sales-employee", "org-1", "dept-1", "position-1"),
                moduleAlias,
                actionCode
        );
    }
}
