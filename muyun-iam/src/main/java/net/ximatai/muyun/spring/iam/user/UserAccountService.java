package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.initialdata.PlatformInitialAdminSettings;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class UserAccountService extends TenantActiveScopedService<UserAccount> implements
        SoftDeleteAbility<UserAccount>,
        EnableAbility<UserAccount>,
        SortAbility<UserAccount>,
        ReferenceAbility<UserAccount>,
        DataScopeAbility<UserAccount>,
        InitialDataAbility<UserAccount>,
        QueryAbility<UserAccount> {
    public static final String MODULE_ALIAS = "iam.user";
    public static final String PLATFORM_SUPER_ADMIN_USER_ID = "platform.user.super_admin";
    public static final String PLATFORM_SUPER_ADMIN_USERNAME = "admin";
    public static final String PLATFORM_SUPER_ADMIN_USER_TITLE = "平台超级管理员";
    private static final int TEMPORARY_PASSWORD_MAX_ATTEMPTS = 32;

    private final PasswordHashingService passwordHashingService;
    private final PasswordPolicyRuleService passwordPolicyRuleService;
    private final Supplier<DataScopeCriteriaService> dataScopeCriteriaService;
    private final SecureRandom secureRandom = new SecureRandom();
    private PlatformInitialAdminSettings initialAdminSettings = PlatformInitialAdminSettings.defaults();
    private static final ActionExecutionPolicy CHANGE_PASSWORD_POLICY = new ActionExecutionPolicy(
            "changePassword",
            PlatformActionLevel.RECORD,
            ActionAccessMode.AUTH_REQUIRED,
            true,
            true,
            ActionDefaultGrantPolicy.NONE,
            null
    );

    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService) {
        this(userAccountDao, activeTenantVerifier, passwordHashingService, Optional.empty(), null);
    }

    @Autowired
    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService,
                              ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService,
                              ObjectProvider<PasswordPolicyRuleService> passwordPolicyRuleService) {
        super(MODULE_ALIAS, UserAccount.class, userAccountDao, activeTenantVerifier);
        this.passwordHashingService = passwordHashingService;
        this.passwordPolicyRuleService = passwordPolicyRuleService == null
                ? null
                : passwordPolicyRuleService.getIfAvailable();
        this.dataScopeCriteriaService = () -> dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new);
    }

    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService,
                              Optional<DataScopeCriteriaService> dataScopeCriteriaService) {
        this(userAccountDao, activeTenantVerifier, passwordHashingService, dataScopeCriteriaService, null);
    }

    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService,
                              Optional<DataScopeCriteriaService> dataScopeCriteriaService,
                              PasswordPolicyRuleService passwordPolicyRuleService) {
        super(MODULE_ALIAS, UserAccount.class, userAccountDao, activeTenantVerifier);
        this.passwordHashingService = passwordHashingService;
        this.passwordPolicyRuleService = passwordPolicyRuleService;
        Optional<DataScopeCriteriaService> criteriaService = dataScopeCriteriaService == null
                ? Optional.empty()
                : dataScopeCriteriaService;
        this.dataScopeCriteriaService = () -> criteriaService
                .<DataScopeCriteriaService>map(service -> service)
                .orElseGet(AllowAllDataScopeCriteriaService::new);
    }

    @Autowired
    public void setInitialAdminSettings(Optional<PlatformInitialAdminSettings> settings) {
        this.initialAdminSettings = settings.orElseGet(PlatformInitialAdminSettings::defaults);
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.system("platform.system-admin-user", 50);
    }

    @Override
    public List<UserAccount> initialData() {
        UserAccount user = new UserAccount();
        user.setId(PLATFORM_SUPER_ADMIN_USER_ID);
        user.setUsername(PLATFORM_SUPER_ADMIN_USERNAME);
        user.setPassword(initialAdminSettings.initialPassword());
        user.setTitle(PLATFORM_SUPER_ADMIN_USER_TITLE);
        user.setAuthUserId(user.getId());
        user.setAuthModuleAlias(MODULE_ALIAS);
        user.setEnabled(Boolean.TRUE);
        user.setSortOrder(1);
        return List.of(user);
    }

    @Override
    public DataScopeCriteriaService getDataScopeCriteriaService() {
        return dataScopeCriteriaService.get();
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("tenantId", QueryOperator.EQ, QueryOperator.IN, QueryOperator.NULL).withTitle("租户"))
                .field(QueryField.of("organizationId", QueryOperator.EQ, QueryOperator.IN).withTitle("所属机构"))
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("username", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("账号").withQuickSearch().withSortable())
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("姓名").withQuickSearch().withSortable())
                .field(QueryField.of("mobile", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("手机号").withQuickSearch())
                .field(QueryField.of("email", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("邮箱").withQuickSearch())
                .field(QueryField.of("passwordStatus", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("密码状态"))
                .field(QueryField.of("lastLoginAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("最后登录时间")
                        .withSortable())
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间")
                        .withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间")
                        .withSortable())
                .defaultSort(net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"))
                .defaultSort(net.ximatai.muyun.database.core.orm.Sort.asc("username"))
                .build();
    }

    @Override
    public void normalizeBeforeMutation(UserAccount user) {
        String username = requireUsername(user.getUsername());
        user.setUsername(username);
        user.setTitle(normalizeBlank(user.getTitle()) == null ? username : user.getTitle().trim());
        user.setMobile(normalizeBlank(user.getMobile()));
        user.setEmail(normalizeBlank(user.getEmail()));
        user.setOrganizationId(normalizeBlank(user.getOrganizationId()));
        user.setAuthOrganizationId(user.getOrganizationId());
        user.setAuthModuleAlias(MODULE_ALIAS);
    }

    @Override
    public void beforePrepareInsert(UserAccount user) {
        if (!TenantContext.isSystem() || user.getTenantId() != null) {
            requireActiveTenantMutationContext();
        }
        normalizeBeforeMutation(user);
    }

    @Override
    public void beforeInsert(UserAccount user) {
        syncSelfAuthUser(user);
        validatePasswordPolicy(user.getPassword());
        user.setPasswordHash(passwordHashingService.hash(user.getPassword()));
        user.setPasswordStatus(user.getPasswordStatus() == null ? PasswordStatus.INITIAL : user.getPasswordStatus());
        user.setPasswordChangedAt(user.getPasswordChangedAt() == null ? Instant.now() : user.getPasswordChangedAt());
        user.setFailedLoginCount(user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount());
        rejectDuplicateUsername(user);
    }

    @Override
    public void beforeUpdate(UserAccount user) {
        UserAccount existing = select(user.getId());
        if (existing != null) {
            preserveSecurityFields(user, existing);
        }
        syncSelfAuthUser(user);
        rejectDuplicateUsername(user);
    }

    public String createUser(UserAccount user, String password) {
        user.setPassword(password);
        return insert(user);
    }

    public int changePassword(String userId, String newPassword) {
        String validUserId = Preconditions.requireText(userId, "userId");
        requireRecordScope(currentRecordMutationPolicy(), List.of(validUserId));
        UserAccount user = requireEnabled(validUserId,
                "user is not active: " + userId);
        validatePasswordPolicy(newPassword);
        user.setPasswordHash(passwordHashingService.hash(newPassword));
        user.setPasswordStatus(PasswordStatus.NORMAL);
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordExpiresAt(null);
        return getDao().updateById(user);
    }

    public PasswordResetResult resetPassword(String userId) {
        String validUserId = Preconditions.requireText(userId, "userId");
        requireRecordScope(resetPasswordPolicy(), List.of(validUserId));
        UserAccount user = requireEnabled(validUserId,
                "user is not active: " + userId);
        String temporaryPassword = generateTemporaryPassword();
        Instant now = Instant.now();
        user.setPasswordHash(passwordHashingService.hash(temporaryPassword));
        user.setPasswordStatus(PasswordStatus.RESET_REQUIRED);
        user.setPasswordChangedAt(now);
        user.setPasswordExpiresAt(now.plusSeconds(86_400));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        int count = getDao().updateById(user);
        return new PasswordResetResult(count, count > 0 ? temporaryPassword : null, user.getPasswordExpiresAt());
    }

    public int changeOwnPassword(String userId, String currentPassword, String newPassword) {
        String validUserId = Preconditions.requireText(userId, "userId");
        UserAccount user = requireEnabled(validUserId,
                "user is not active: " + userId);
        if (!passwordMatches(user, currentPassword)) {
            throw new AuthenticationFailedException("invalid username or password");
        }
        validatePasswordPolicy(newPassword);
        user.setPasswordHash(passwordHashingService.hash(newPassword));
        user.setPasswordStatus(PasswordStatus.NORMAL);
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordExpiresAt(null);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        return getDao().updateById(user);
    }

    public boolean passwordChangeRequired(UserAccount user, Instant now) {
        if (user == null) {
            return false;
        }
        if (passwordExpired(user, now)) {
            return true;
        }
        PasswordStatus status = effectivePasswordStatus(user);
        return status == PasswordStatus.INITIAL
                || status == PasswordStatus.RESET_REQUIRED
                || status == PasswordStatus.EXPIRED;
    }

    public boolean resetPasswordExpired(UserAccount user, Instant now) {
        return effectivePasswordStatus(user) == PasswordStatus.RESET_REQUIRED
                && passwordExpired(user, now);
    }

    public PasswordStatus effectivePasswordStatus(UserAccount user) {
        return user == null || user.getPasswordStatus() == null ? PasswordStatus.NORMAL : user.getPasswordStatus();
    }

    public void recordLoginSuccess(String userId, Instant loginAt, String ip, String userAgent) {
        UserAccount user = select(userId);
        if (user == null) {
            return;
        }
        user.setLastLoginAt(loginAt);
        user.setLastLoginIp(normalizeLength(ip, 64));
        user.setLastLoginUserAgent(normalizeLength(userAgent, 512));
        user.setFailedLoginCount(0);
        updateLoginAudit(user);
    }

    public void recordLoginFailure(UserAccount user, Instant failedAt) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            return;
        }
        UserAccount latest = select(user.getId());
        if (latest == null) {
            return;
        }
        latest.setLastFailedLoginAt(failedAt);
        latest.setFailedLoginCount((latest.getFailedLoginCount() == null ? 0 : latest.getFailedLoginCount()) + 1);
        updateLoginAudit(latest);
    }

    public UserAccount requireActiveUser(String username) {
        return requireActiveUser(TenantContext.currentTenantId().orElse(null), username);
    }

    public UserAccount requireActiveUser(String tenantId, String username) {
        String validUsername = requireUsername(username);
        UserAccount user = findOne(Criteria.of()
                .eq("username", validUsername)
                .eqNullable("tenantId", normalizeBlank(tenantId)));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new AuthenticationFailedException("invalid username or password");
        }
        return user;
    }

    public boolean passwordMatches(UserAccount user, String password) {
        return user != null && passwordHashingService.matches(password, user.getPasswordHash());
    }

    private void rejectDuplicateUsername(UserAccount user) {
        rejectDuplicate(user, Criteria.of()
                        .eq("username", user.getUsername())
                        .eqNullable("tenantId", user.getTenantId()),
                "username must be unique within tenant: " + user.getUsername());
    }

    private String requireUsername(String username) {
        return Preconditions.requireText(username, "username").trim();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void syncSelfAuthUser(UserAccount user) {
        if (user.getId() != null && !user.getId().isBlank()) {
            user.setAuthUserId(user.getId());
        }
    }

    private void preserveSecurityFields(UserAccount user, UserAccount existing) {
        user.setPasswordHash(existing.getPasswordHash());
        user.setPasswordStatus(existing.getPasswordStatus());
        user.setPasswordChangedAt(existing.getPasswordChangedAt());
        user.setPasswordExpiresAt(existing.getPasswordExpiresAt());
        user.setLastLoginAt(existing.getLastLoginAt());
        user.setLastLoginIp(existing.getLastLoginIp());
        user.setLastLoginUserAgent(existing.getLastLoginUserAgent());
        user.setLastFailedLoginAt(existing.getLastFailedLoginAt());
        user.setFailedLoginCount(existing.getFailedLoginCount());
        user.setLockedUntil(existing.getLockedUntil());
    }

    private void validatePasswordPolicy(String password) {
        if (passwordPolicyRuleService != null) {
            passwordPolicyRuleService.validatePassword(password);
            return;
        }
        if (password == null || password.length() < 6) {
            throw new PlatformException("密码长度不能少于 6 位");
        }
    }

    private ActionExecutionPolicy currentRecordMutationPolicy() {
        return ActionExecutionContextHolder.current()
                .filter(context -> MODULE_ALIAS.equals(context.moduleAlias()))
                .map(context -> context.actionPolicy())
                .orElse(CHANGE_PASSWORD_POLICY);
    }

    private ActionExecutionPolicy resetPasswordPolicy() {
        return ActionExecutionContextHolder.current()
                .filter(context -> MODULE_ALIAS.equals(context.moduleAlias()))
                .map(context -> context.actionPolicy())
                .orElse(new ActionExecutionPolicy(
                        "resetPassword",
                        PlatformActionLevel.RECORD,
                        ActionAccessMode.AUTH_REQUIRED,
                        true,
                        true,
                        ActionDefaultGrantPolicy.NONE,
                        null
                ));
    }

    private String generateTemporaryPassword() {
        for (int attempt = 0; attempt < TEMPORARY_PASSWORD_MAX_ATTEMPTS; attempt++) {
            byte[] bytes = new byte[12];
            secureRandom.nextBytes(bytes);
            String temporaryPassword = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            try {
                validatePasswordPolicy(temporaryPassword);
                return temporaryPassword;
            } catch (PlatformException ignored) {
                // Configured regex rules can reject random candidates; try another bounded candidate.
            }
        }
        throw new PlatformException("unable to generate temporary password that satisfies current policy");
    }

    private boolean passwordExpired(UserAccount user, Instant now) {
        return user != null
                && user.getPasswordExpiresAt() != null
                && now != null
                && !now.isBefore(user.getPasswordExpiresAt());
    }

    private void updateLoginAudit(UserAccount user) {
        if (user.getVersion() == null) {
            getDao().updateById(user);
            return;
        }
        getDao().updateByIdAndVersion(user, user.getVersion());
    }

    private String normalizeLength(String value, int maxLength) {
        String normalized = normalizeBlank(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    public record PasswordResetResult(int count, String temporaryPassword, Instant expiresAt) {
    }
}
