package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceContractTest {
    private final PasswordHashingService passwordHashingService = new PasswordHashingService();

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ActionExecutionContextHolder.clear();
    }

    @Test
    void shouldExposeDataScopeAbility() {
        UserAccountService service = new UserAccountService(
                mock(UserAccountDao.class),
                tenantId -> {
                },
                passwordHashingService,
                Optional.of(mock(DataScopeCriteriaService.class))
        );

        assertThat(service).isInstanceOf(DataScopeAbility.class);
    }

    @Test
    void shouldSyncUserAccountDataScopeFieldsOnInsert() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.insert(any())).thenAnswer(invocation -> invocation.<UserAccount>getArgument(0).getId());
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserAccount user = new UserAccount();
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setOrganizationId("org-1");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.createUser(user, "secret1");
        }

        assertThat(user.getAuthUserId()).isEqualTo(user.getId());
        assertThat(user.getAuthOrganizationId()).isEqualTo("org-1");
        assertThat(user.getAuthModuleAlias()).isEqualTo(UserAccountService.MODULE_ALIAS);
    }

    @Test
    void shouldApplyRecordDataScopeWhenChangingPassword() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = activeUser();
        when(dao.count(any(Criteria.class))).thenReturn(1L);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        when(dao.updateById(any(UserAccount.class))).thenReturn(1);
        DataScopeCriteriaService dataScope = mock(DataScopeCriteriaService.class);
        when(dataScope.resolveReadScope(eq(UserAccountService.MODULE_ALIAS),
                any(ActionExecutionPolicy.class), any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any()))
                .thenReturn(DataScopeCriteriaResult.restricted(Criteria.of().eq("id", "user-1")));
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService, Optional.of(dataScope));
        ActionExecutionPolicy policy = new ActionExecutionPolicy(
                "changePassword",
                PlatformActionLevel.RECORD,
                ActionAccessMode.AUTH_REQUIRED,
                true,
                true,
                ActionDefaultGrantPolicy.NONE,
                null
        );

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a");
             ActionExecutionContextHolder.Scope ignoredAction = ActionExecutionContextHolder.use(
                     ActionExecutionContext.ofPolicy(
                             UserAccountService.MODULE_ALIAS,
                             policy,
                             Set.of("user-1"),
                             Optional.empty()
                     ))) {
            assertThat(service.changePassword("user-1", "secret2")).isEqualTo(1);
        }

        verify(dataScope).resolveReadScope(
                eq(UserAccountService.MODULE_ALIAS),
                eq(policy),
                any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any()
        );
    }

    @Test
    void shouldUseChangePasswordDataScopePolicyWithoutActionContext() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = activeUser();
        when(dao.count(any(Criteria.class))).thenReturn(1L);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        when(dao.updateById(any(UserAccount.class))).thenReturn(1);
        DataScopeCriteriaService dataScope = mock(DataScopeCriteriaService.class);
        when(dataScope.resolveReadScope(eq(UserAccountService.MODULE_ALIAS),
                any(ActionExecutionPolicy.class), any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any()))
                .thenReturn(DataScopeCriteriaResult.restricted(Criteria.of().eq("id", "user-1")));
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService, Optional.of(dataScope));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(service.changePassword("user-1", "secret2")).isEqualTo(1);
        }

        verify(dataScope).resolveReadScope(
                eq(UserAccountService.MODULE_ALIAS),
                org.mockito.ArgumentMatchers.<ActionExecutionPolicy>argThat(policy ->
                        "changePassword".equals(policy.actionCode()) && policy.requiresDataScope()),
                any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any()
        );
    }

    @Test
    void shouldRejectChangePasswordWhenRecordDataScopeDenied() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.count(any(Criteria.class))).thenReturn(0L);
        DataScopeCriteriaService dataScope = mock(DataScopeCriteriaService.class);
        when(dataScope.resolveReadScope(eq(UserAccountService.MODULE_ALIAS),
                any(ActionExecutionPolicy.class), any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any()))
                .thenReturn(DataScopeCriteriaResult.restricted(Criteria.of().eq("id", "user-1")));
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService, Optional.of(dataScope));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.changePassword("user-1", "secret2"))
                    .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                    .hasMessageContaining("record data permission denied");
        }

        verify(dao, never()).updateById(any(UserAccount.class));
    }

    private UserAccount activeUser() {
        UserAccount user = new UserAccount();
        user.setId("user-1");
        user.setTenantId("tenant-a");
        user.setUsername("alice");
        user.setTitle("Alice");
        user.setEnabled(Boolean.TRUE);
        user.setPasswordHash(passwordHashingService.hash("secret1"));
        return user;
    }
}
