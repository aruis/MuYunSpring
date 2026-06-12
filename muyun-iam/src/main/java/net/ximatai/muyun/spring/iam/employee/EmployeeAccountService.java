package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class EmployeeAccountService extends TenantStandardBusinessService<EmployeeAccount> implements
        EnableAbility<EmployeeAccount> {
    public static final String MODULE_ALIAS = "iam.employee_account";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

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
        binding.setPrimaryAccount(Boolean.TRUE.equals(binding.getPrimaryAccount()));
        if (binding.getEnabled() == null) {
            binding.setEnabled(true);
        }
    }

    @Override
    protected void validateBeforeSave(EmployeeAccount binding) {
        validateAccountReferences(binding);
        rejectDuplicate(binding, Criteria.of()
                        .eq("employeeId", binding.getEmployeeId())
                        .eq("userId", binding.getUserId()),
                "employee account binding already exists");
        rejectDuplicate(binding, Criteria.of().eq("userId", binding.getUserId()),
                "user account can bind only one employee: " + binding.getUserId());
        if (Boolean.TRUE.equals(binding.getPrimaryAccount())) {
            rejectDuplicate(binding, Criteria.of()
                            .eq("employeeId", binding.getEmployeeId())
                            .eq("primaryAccount", Boolean.TRUE)
                            .eq("enabled", Boolean.TRUE),
                    "employee can only have one primary account");
        }
    }

    public List<EmployeeAccount> accounts(String employeeId) {
        return list(employeeCriteria(Preconditions.requireText(employeeId, "employeeId")), ALL);
    }

    public String bindAccount(String employeeId, EmployeeAccount binding) {
        binding.setEmployeeId(Preconditions.requireText(employeeId, "employeeId"));
        return insert(binding);
    }

    public int deleteAccount(String employeeId, String bindingId) {
        EmployeeAccount binding = requireEmployeeAccount(employeeId, bindingId);
        return delete(binding);
    }

    public int enableAccount(String employeeId, String bindingId) {
        requireEmployeeAccount(employeeId, bindingId);
        return enable(bindingId);
    }

    public int disableAccount(String employeeId, String bindingId) {
        requireEmployeeAccount(employeeId, bindingId);
        return disable(bindingId);
    }

    @Transactional
    public int makePrimaryAccount(String employeeId, String bindingId) {
        requireActiveTenantMutationContext();
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        EmployeeAccount target = requireEmployeeAccount(validEmployeeId, bindingId);
        validateAccountReferences(target);
        int changed = 0;
        for (EmployeeAccount current : activePrimaryAccounts(validEmployeeId)) {
            if (Objects.equals(current.getId(), target.getId())) {
                continue;
            }
            current.setPrimaryAccount(Boolean.FALSE);
            changed += update(current);
        }
        if (!Boolean.TRUE.equals(target.getEnabled()) || !Boolean.TRUE.equals(target.getPrimaryAccount())) {
            target.setEnabled(Boolean.TRUE);
            target.setPrimaryAccount(Boolean.TRUE);
            changed += update(target);
        }
        return changed;
    }

    public String employeeIdOfUser(String userId) {
        String validUserId = Preconditions.requireText(userId, "userId");
        return list(Criteria.of()
                        .eq("userId", validUserId)
                        .eq("enabled", Boolean.TRUE),
                new PageRequest(0, 1))
                .stream()
                .findFirst()
                .map(EmployeeAccount::getEmployeeId)
                .orElse(null);
    }

    private EmployeeAccount requireEmployeeAccount(String employeeId, String bindingId) {
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        String validBindingId = Preconditions.requireText(bindingId, "bindingId");
        EmployeeAccount binding = select(validBindingId);
        if (binding == null || !validEmployeeId.equals(binding.getEmployeeId())) {
            throw new PlatformException("employee account does not belong to employee: " + validBindingId);
        }
        return binding;
    }

    private Criteria employeeCriteria(String employeeId) {
        return Criteria.of().eq("employeeId", employeeId);
    }

    private List<EmployeeAccount> activePrimaryAccounts(String employeeId) {
        return list(employeeCriteria(employeeId)
                .eq("primaryAccount", Boolean.TRUE)
                .eq("enabled", Boolean.TRUE), ALL);
    }

    private void validateAccountReferences(EmployeeAccount binding) {
        employeeService.requireEnabled(binding.getEmployeeId(),
                "employee is not active: " + binding.getEmployeeId());
        userAccountService.requireEnabled(binding.getUserId(),
                "user account is not active: " + binding.getUserId());
    }
}
