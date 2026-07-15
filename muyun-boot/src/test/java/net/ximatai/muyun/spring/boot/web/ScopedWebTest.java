package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopedWebTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldAllowSystemContextWithoutTenantId() {
        TestScopedWeb web = new TestScopedWeb();

        try (TenantContext.Scope ignored = TenantContext.system("test system web scope")) {
            assertThat(web.webScope(() -> "ok")).isEqualTo("ok");
        }
    }

    @Test
    void shouldRequireTenantOrSystemContext() {
        TestScopedWeb web = new TestScopedWeb();

        assertThatThrownBy(() -> web.webScope(() -> "ok"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("test.module requires tenant context");
    }

    @Test
    void shouldVerifyActiveTenantInTenantContext() {
        VerifyingScopedWeb web = new VerifyingScopedWeb();

        try (CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "alice", "tenant-a"));
             TenantContext.Scope ignoredTenant = TenantContext.use("tenant-a")) {
            assertThat(web.webScope(() -> "ok")).isEqualTo("ok");
        }

        assertThat(web.verifiedTenantId).isEqualTo("tenant-a");
    }

    private static class TestScopedWeb implements ScopedWeb<Object> {
        @Override
        public Object service() {
            return new Object();
        }

        @Override
        public String webScopeName() {
            return "test.module";
        }
    }

    private static class VerifyingScopedWeb implements ScopedWeb<ActiveTenantVerifier> {
        private String verifiedTenantId;

        @Override
        public ActiveTenantVerifier service() {
            return tenantId -> verifiedTenantId = tenantId;
        }

        @Override
        public String webScopeName() {
            return "test.module";
        }
    }
}
