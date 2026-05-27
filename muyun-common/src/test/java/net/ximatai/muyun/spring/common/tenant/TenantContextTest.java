package net.ximatai.muyun.spring.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {
    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeNoContextByDefault() {
        assertThat(TenantContext.hasContext()).isFalse();
        assertThat(TenantContext.isSystem()).isFalse();
        assertThat(TenantContext.currentTenantId()).isEmpty();
    }

    @Test
    void shouldUseTenantScopeAndRestorePreviousContext() {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(TenantContext.hasContext()).isTrue();
            assertThat(TenantContext.isSystem()).isFalse();
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");

            try (TenantContext.Scope nested = TenantContext.use("tenant-b")) {
                assertThat(TenantContext.currentTenantId()).contains("tenant-b");
            }

            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
        }

        assertThat(TenantContext.hasContext()).isFalse();
        assertThat(TenantContext.currentTenantId()).isEmpty();
    }

    @Test
    void shouldUseSystemScopeAndRestoreTenantContext() {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            try (TenantContext.Scope system = TenantContext.system()) {
                assertThat(TenantContext.hasContext()).isTrue();
                assertThat(TenantContext.isSystem()).isTrue();
                assertThat(TenantContext.currentTenantId()).isEmpty();
            }

            assertThat(TenantContext.isSystem()).isFalse();
            assertThat(TenantContext.currentTenantId()).contains("tenant-a");
        }
    }

    @Test
    void shouldClearBlankTenantToNoContext() {
        TenantContext.setTenantId("tenant-a");
        TenantContext.setTenantId(" ");

        assertThat(TenantContext.hasContext()).isFalse();
        assertThat(TenantContext.currentTenantId()).isEmpty();
    }
}
