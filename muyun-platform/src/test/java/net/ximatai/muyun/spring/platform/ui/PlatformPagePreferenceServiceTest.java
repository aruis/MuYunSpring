package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class PlatformPagePreferenceServiceTest {
    private final PlatformPagePreferenceService service =
            new PlatformPagePreferenceService(new TestMemoryDao<>());

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldSaveCurrentUserPagePreferenceWithoutChangingPlatformConfig() {
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            PlatformPagePreference saved = service.saveCurrentUserPreference(
                    "sales.contract", PlatformUiClientType.WEB, "list", "{\"columns\":[\"code\"]}");
            PlatformPagePreference updated = service.saveCurrentUserPreference(
                    "sales.contract", PlatformUiClientType.WEB, "list", "{\"columns\":[\"code\",\"amount\"]}");

            assertThat(updated.getId()).isEqualTo(saved.getId());
            assertThat(service.currentUserPreference("sales.contract", PlatformUiClientType.WEB, "list")
                    .getPreferenceJson()).contains("amount");
        }

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-2", "User", "tenant-a"))) {
            assertThat(service.currentUserPreference("sales.contract", PlatformUiClientType.WEB, "list"))
                    .isNull();
        }
    }

    @Test
    void shouldRejectInvalidPreferenceJson() {
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            assertThatThrownBy(() -> service.saveCurrentUserPreference(
                    "sales.contract", PlatformUiClientType.WEB, "list", "not json"))
                    .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                    .hasMessage("page preference preferenceJson must be valid JSON");
        }
    }
}
