package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserAccountService extends TenantActiveScopedService<UserAccount> implements
        SoftDeleteAbility<UserAccount>,
        EnableAbility<UserAccount>,
        SortAbility<UserAccount>,
        ReferenceAbility<UserAccount>,
        DataScopeAbility<UserAccount> {
    public static final String MODULE_ALIAS = "iam.user";

    private final PasswordHashingService passwordHashingService;
    private final DataScopeCriteriaService dataScopeCriteriaService;
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
        this(userAccountDao, activeTenantVerifier, passwordHashingService, Optional.empty());
    }

    @Autowired
    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService,
                              Optional<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, UserAccount.class, userAccountDao, activeTenantVerifier);
        this.passwordHashingService = passwordHashingService;
        this.dataScopeCriteriaService = dataScopeCriteriaService
                .<DataScopeCriteriaService>map(service -> service)
                .orElseGet(AllowAllDataScopeCriteriaService::new);
    }

    @Override
    public DataScopeCriteriaService getDataScopeCriteriaService() {
        return dataScopeCriteriaService;
    }

    @Override
    public void normalizeBeforeMutation(UserAccount user) {
        user.setUsername(requireUsername(user.getUsername()));
        user.setMobile(normalizeBlank(user.getMobile()));
        user.setEmail(normalizeBlank(user.getEmail()));
        user.setOrganizationId(normalizeBlank(user.getOrganizationId()));
        user.setAuthOrganizationId(user.getOrganizationId());
        user.setAuthModuleAlias(MODULE_ALIAS);
    }

    @Override
    public void beforeInsert(UserAccount user) {
        syncSelfAuthUser(user);
        user.setPasswordHash(passwordHashingService.hash(user.getPassword()));
        rejectDuplicateUsername(user);
    }

    @Override
    public void beforeUpdate(UserAccount user) {
        UserAccount existing = select(user.getId());
        if (existing != null) {
            user.setPasswordHash(existing.getPasswordHash());
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
        user.setPasswordHash(passwordHashingService.hash(newPassword));
        return getDao().updateById(user);
    }

    public UserAccount requireActiveUser(String username) {
        String validUsername = requireUsername(username);
        UserAccount user = findOne(Criteria.of().eq("username", validUsername));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
            throw new PlatformException("invalid username or password");
        }
        return user;
    }

    public boolean passwordMatches(UserAccount user, String password) {
        return user != null && passwordHashingService.matches(password, user.getPasswordHash());
    }

    private void rejectDuplicateUsername(UserAccount user) {
        rejectDuplicate(user, Criteria.of().eq("username", user.getUsername()),
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

    private ActionExecutionPolicy currentRecordMutationPolicy() {
        return ActionExecutionContextHolder.current()
                .filter(context -> MODULE_ALIAS.equals(context.moduleAlias()))
                .map(context -> context.actionPolicy())
                .orElse(CHANGE_PASSWORD_POLICY);
    }
}
