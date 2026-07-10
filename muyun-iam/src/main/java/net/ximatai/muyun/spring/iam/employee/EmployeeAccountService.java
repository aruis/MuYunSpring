package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeAccountService extends TenantStandardBusinessService<EmployeeAccount> {
    public static final String MODULE_ALIAS = "iam.employee_account";
    private static final String ACCOUNT_REMOVAL_OPERATOR_ID = "employee-account-removal";

    private final EmployeeService employeeService;
    private final UserAccountService userAccountService;

    @Autowired
    public EmployeeAccountService(EmployeeAccountDao employeeAccountDao,
                                  ActiveTenantVerifier activeTenantVerifier,
                                  EmployeeService employeeService,
                                  UserAccountService userAccountService) {
        super(MODULE_ALIAS, EmployeeAccount.class, employeeAccountDao, activeTenantVerifier);
        this.employeeService = employeeService;
        this.userAccountService = userAccountService;
    }

    @Override
    public void normalizeBeforeMutation(EmployeeAccount binding) {
        binding.setEmployeeId(Preconditions.requireText(binding.getEmployeeId(), "employeeId"));
        binding.setUserId(Preconditions.requireText(binding.getUserId(), "userId"));
    }

    @Override
    protected void validateBeforeSave(EmployeeAccount binding) {
        validateAccountReferences(binding);
        rejectDuplicate(binding, Criteria.of()
                .eq("employeeId", binding.getEmployeeId()),
                "employee can bind only one user account: " + binding.getEmployeeId());
        rejectDuplicate(binding, Criteria.of().eq("userId", binding.getUserId()),
                "user account can bind only one employee: " + binding.getUserId());
    }

    public EmployeeAccount accountOfEmployee(String employeeId) {
        return list(employeeCriteria(Preconditions.requireText(employeeId, "employeeId")),
                new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public String bindAccount(String employeeId, EmployeeAccount binding) {
        binding.setEmployeeId(Preconditions.requireText(employeeId, "employeeId"));
        return insert(binding);
    }

    @Transactional
    public AccountProvisionResult provisionAccount(String employeeId, UserAccount account) {
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        if (accountOfEmployee(validEmployeeId) != null) {
            throw new PlatformException("employee already has user account: " + validEmployeeId);
        }
        employeeService.requireEnabled(validEmployeeId, "employee is not active: " + validEmployeeId);
        UserAccount user = normalizeProvisionUser(account);
        String userId = userAccountService.insert(user);
        EmployeeAccount binding = new EmployeeAccount();
        binding.setEmployeeId(validEmployeeId);
        binding.setUserId(userId);
        String bindingId = bindAccount(validEmployeeId, binding);
        return new AccountProvisionResult(userAccountService.select(userId), select(bindingId));
    }

    @Transactional
    public int removeAccount(String employeeId) {
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        EmployeeAccount binding = accountOfEmployee(validEmployeeId);
        if (binding == null) {
            return 0;
        }
        String userId = binding.getUserId();
        int deleted = delete(binding);
        if (deleted > 0 && userAccountService.select(userId) == null) {
            userAccountService.cleanupDeletedUserReferences(userId);
        } else if (deleted > 0) {
            try (CurrentUserContext.Scope ignored = CurrentUserContext.use(CurrentUser.systemUser(
                    ACCOUNT_REMOVAL_OPERATOR_ID, "Employee Account Removal"))) {
                userAccountService.delete(userId);
            }
        }
        return deleted;
    }

    public String employeeIdOfUser(String userId) {
        EmployeeAccount binding = accountOfUser(userId);
        return binding == null ? null : binding.getEmployeeId();
    }

    public EmployeeAccount accountOfUser(String userId) {
        return list(Criteria.of().eq("userId", Preconditions.requireText(userId, "userId")),
                new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Criteria employeeCriteria(String employeeId) {
        return Criteria.of().eq("employeeId", employeeId);
    }

    private void validateAccountReferences(EmployeeAccount binding) {
        employeeService.requireEnabled(binding.getEmployeeId(),
                "employee is not active: " + binding.getEmployeeId());
        userAccountService.requireEnabled(binding.getUserId(),
                "user account is not active: " + binding.getUserId());
    }

    private UserAccount normalizeProvisionUser(UserAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }
        if (account.getEnabled() == null) {
            account.setEnabled(Boolean.TRUE);
        }
        return account;
    }

    public record AccountProvisionResult(UserAccount user, EmployeeAccount binding) {
    }
}
