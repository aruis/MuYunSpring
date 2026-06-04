package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService extends TenantActiveScopedService<UserAccount> implements
        SoftDeleteAbility<UserAccount>,
        EnableAbility<UserAccount>,
        SortAbility<UserAccount>,
        ReferenceAbility<UserAccount> {
    public static final String MODULE_ALIAS = "iam.user";

    private final PasswordHashingService passwordHashingService;

    public UserAccountService(UserAccountDao userAccountDao,
                              ActiveTenantVerifier activeTenantVerifier,
                              PasswordHashingService passwordHashingService) {
        super(MODULE_ALIAS, UserAccount.class, userAccountDao, activeTenantVerifier);
        this.passwordHashingService = passwordHashingService;
    }

    @Override
    public void normalizeBeforeMutation(UserAccount user) {
        user.setUsername(requireUsername(user.getUsername()));
        user.setMobile(normalizeBlank(user.getMobile()));
        user.setEmail(normalizeBlank(user.getEmail()));
        user.setOrganizationId(normalizeBlank(user.getOrganizationId()));
    }

    @Override
    public void beforeInsert(UserAccount user) {
        user.setPasswordHash(passwordHashingService.hash(user.getPassword()));
        rejectDuplicateUsername(user);
    }

    @Override
    public void beforeUpdate(UserAccount user) {
        UserAccount existing = select(user.getId());
        if (existing != null) {
            user.setPasswordHash(existing.getPasswordHash());
        }
        rejectDuplicateUsername(user);
    }

    public String createUser(UserAccount user, String password) {
        user.setPassword(password);
        return insert(user);
    }

    public int changePassword(String userId, String newPassword) {
        UserAccount user = requireEnabled(Preconditions.requireText(userId, "userId"),
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
}
