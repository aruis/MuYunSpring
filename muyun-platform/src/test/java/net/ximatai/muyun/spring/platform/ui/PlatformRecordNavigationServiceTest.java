package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformRecordNavigationServiceTest {
    private final PlatformRecordNavigationService service =
            new PlatformRecordNavigationService(new TestMemoryDao<>());

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldCreateCurrentUserNavigationSessionAndResolveNeighbors() {
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            PlatformRecordNavigationContext context = service.createCurrentUserSession(
                    "sales.contract", "contract", List.of("contract-1", "contract-2", "contract-3"),
                    2, 30, 90, " query-1 ");

            PlatformRecordNavigationMove move = service.move("sales.contract", context.sessionId(), "contract-2");

            assertThat(context.recordIds()).containsExactly("contract-1", "contract-2", "contract-3");
            assertThat(context.querySnapshotKey()).isEqualTo("query-1");
            assertThat(service.select(context.sessionId()).getQuerySnapshotKey()).isEqualTo("query-1");
            assertThat(move.previousRecordId()).isEqualTo("contract-1");
            assertThat(move.nextRecordId()).isEqualTo("contract-3");
            assertThat(move.first()).isFalse();
            assertThat(move.last()).isFalse();
        }
    }

    @Test
    void shouldRejectNavigationSessionFromAnotherUser() {
        String sessionId;
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            sessionId = service.createCurrentUserSession("sales.contract", "contract", List.of("contract-1"),
                    1, 20, 1).sessionId();
        }

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-2", "User", "tenant-a"))) {
            assertThatThrownBy(() -> service.move("sales.contract", sessionId, "contract-1"))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("record navigation session is not available");
        }
    }
}
