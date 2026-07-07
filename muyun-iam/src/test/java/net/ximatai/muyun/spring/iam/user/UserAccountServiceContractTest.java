package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QuerySchema;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeFieldMapping;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void shouldExposeQuerySchemaForUserManagementScopes() {
        UserAccountService service = new UserAccountService(
                mock(UserAccountDao.class),
                tenantId -> {
                },
                passwordHashingService,
                Optional.of(mock(DataScopeCriteriaService.class))
        );

        assertThat(service).isInstanceOf(QueryAbility.class);
        QuerySchema schema = service.querySchema();

        assertThat(schema.fields()).extracting(QuerySchema.Field::name)
                .contains("tenantId", "organizationId", "username", "title", "mobile", "email", "enabled",
                        "passwordStatus", "lastLoginAt");
        assertThat(field(schema, "tenantId").operators())
                .containsExactly(QueryOperator.EQ, QueryOperator.IN, QueryOperator.NULL);
        assertThat(field(schema, "organizationId").operators()).containsExactly(QueryOperator.EQ, QueryOperator.IN);
        assertThat(schema.quickSearch().fields()).containsExactly("username", "title", "mobile", "email");
        assertThat(schema.defaultSorts()).extracting(QuerySchema.DefaultSort::field)
                .containsExactly("sortOrder", "username");
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
        assertThat(user.getPasswordStatus()).isEqualTo(PasswordStatus.INITIAL);
        assertThat(user.getPasswordChangedAt()).isNotNull();
        assertThat(user.getFailedLoginCount()).isZero();
    }

    @Test
    void shouldDefaultUserTitleFromUsernameWhenMissing() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.insert(any())).thenAnswer(invocation -> invocation.<UserAccount>getArgument(0).getId());
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserAccount user = new UserAccount();
        user.setUsername("alice");

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            service.createUser(user, "secret1");
        }

        assertThat(user.getTitle()).isEqualTo("alice");
    }

    @Test
    void shouldValidatePasswordPolicyWhenWritingUserPassword() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = activeUser();
        when(dao.insert(any())).thenAnswer(invocation -> {
            invocation.<UserAccount>getArgument(0).setId("user-2");
            return "user-2";
        });
        when(dao.count(any(Criteria.class))).thenReturn(1L);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(
                List.of(),
                List.of(user),
                List.of(user)
        );
        when(dao.updateById(any(UserAccount.class))).thenReturn(1);
        PasswordPolicyRuleService passwordPolicyRuleService = mock(PasswordPolicyRuleService.class);
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService, Optional.empty(), passwordPolicyRuleService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            UserAccount created = new UserAccount();
            created.setUsername("bob");
            service.createUser(created, "create1");
            service.changePassword("user-1", "admin2");
            service.changeOwnPassword("user-1", "admin2", "own3");
        }

        verify(passwordPolicyRuleService).validatePassword("create1");
        verify(passwordPolicyRuleService).validatePassword("admin2");
        verify(passwordPolicyRuleService).validatePassword("own3");
    }

    @Test
    void shouldRejectPasswordWhenPolicyRuleFails() {
        UserAccountDao dao = mock(UserAccountDao.class);
        PasswordPolicyRuleService passwordPolicyRuleService = mock(PasswordPolicyRuleService.class);
        org.mockito.Mockito.doThrow(new net.ximatai.muyun.spring.common.exception.PlatformException("密码必须包含数字"))
                .when(passwordPolicyRuleService).validatePassword("secret");
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService, Optional.empty(), passwordPolicyRuleService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            UserAccount user = new UserAccount();
            user.setUsername("alice");
            assertThatThrownBy(() -> service.createUser(user, "secret"))
                    .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                    .hasMessageContaining("密码必须包含数字");
        }

        verify(dao, never()).insert(any());
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
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any(),
                any(DataScopeFieldMapping.class)))
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

        assertThat(user.getPasswordStatus()).isEqualTo(PasswordStatus.NORMAL);
        assertThat(user.getPasswordExpiresAt()).isNull();

        verify(dataScope).resolveReadScope(
                eq(UserAccountService.MODULE_ALIAS),
                eq(policy),
                any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any(),
                any(DataScopeFieldMapping.class)
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
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any(),
                any(DataScopeFieldMapping.class)))
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
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any(),
                any(DataScopeFieldMapping.class)
        );
    }

    @Test
    void shouldRejectChangePasswordWhenRecordDataScopeDenied() {
        UserAccountDao dao = mock(UserAccountDao.class);
        when(dao.count(any(Criteria.class))).thenReturn(0L);
        DataScopeCriteriaService dataScope = mock(DataScopeCriteriaService.class);
        when(dataScope.resolveReadScope(eq(UserAccountService.MODULE_ALIAS),
                any(ActionExecutionPolicy.class), any(Criteria.class),
                org.mockito.ArgumentMatchers.<Optional<CurrentUser>>any(),
                any(DataScopeFieldMapping.class)))
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

    @Test
    void shouldResetPasswordWithTemporaryPasswordAndRequiredStatus() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount user = activeUser();
        when(dao.count(any(Criteria.class))).thenReturn(1L);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(user));
        when(dao.updateById(any(UserAccount.class))).thenReturn(1);
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            UserAccountService.PasswordResetResult result = service.resetPassword("user-1");

            assertThat(result.count()).isEqualTo(1);
            assertThat(result.temporaryPassword()).isNotBlank();
            assertThat(result.expiresAt()).isNotNull();
            assertThat(passwordHashingService.matches(result.temporaryPassword(), user.getPasswordHash())).isTrue();
            assertThat(user.getPasswordStatus()).isEqualTo(PasswordStatus.RESET_REQUIRED);
            assertThat(user.getPasswordExpiresAt()).isEqualTo(result.expiresAt());
        }
    }

    @Test
    void shouldPreserveSecurityFieldsWhenUpdatingProfile() {
        UserAccountDao dao = mock(UserAccountDao.class);
        UserAccount existing = activeUser();
        existing.setPasswordStatus(PasswordStatus.RESET_REQUIRED);
        existing.setPasswordChangedAt(java.time.Instant.parse("2026-07-01T00:00:00Z"));
        existing.setPasswordExpiresAt(java.time.Instant.parse("2026-07-02T00:00:00Z"));
        existing.setFailedLoginCount(3);
        existing.setLastLoginIp("127.0.0.1");
        UserAccountService service = new UserAccountService(dao, tenantId -> {
        }, passwordHashingService);
        UserAccount profile = activeUser();
        profile.setTitle("Alice Updated");
        profile.setPasswordStatus(PasswordStatus.NORMAL);
        when(dao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(existing));

        service.beforeUpdate(profile);

        assertThat(profile.getPasswordStatus()).isEqualTo(PasswordStatus.RESET_REQUIRED);
        assertThat(profile.getPasswordExpiresAt()).isEqualTo(existing.getPasswordExpiresAt());
        assertThat(profile.getFailedLoginCount()).isEqualTo(3);
        assertThat(profile.getLastLoginIp()).isEqualTo("127.0.0.1");
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

    private QuerySchema.Field field(QuerySchema schema, String fieldName) {
        return schema.fields().stream()
                .filter(field -> fieldName.equals(field.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing query field: " + fieldName));
    }
}
