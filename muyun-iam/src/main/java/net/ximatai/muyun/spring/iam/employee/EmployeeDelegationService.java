package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeDelegationService extends TenantStandardBusinessService<EmployeeDelegation> implements
        EnableAbility<EmployeeDelegation> {
    public static final String MODULE_ALIAS = "iam.employee_delegation";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final EmployeeService employeeService;
    private final EmployeePositionService employeePositionService;

    @Autowired
    public EmployeeDelegationService(EmployeeDelegationDao employeeDelegationDao,
                                     ActiveTenantVerifier activeTenantVerifier,
                                     EmployeeService employeeService,
                                     EmployeePositionService employeePositionService) {
        super(MODULE_ALIAS, EmployeeDelegation.class, employeeDelegationDao, activeTenantVerifier);
        this.employeeService = employeeService;
        this.employeePositionService = employeePositionService;
    }

    @Override
    public void normalizeBeforeMutation(EmployeeDelegation delegation) {
        delegation.setPrincipalEmployeeId(Preconditions.requireText(
                delegation.getPrincipalEmployeeId(), "principalEmployeeId"));
        delegation.setDelegateEmployeeId(Preconditions.requireText(
                delegation.getDelegateEmployeeId(), "delegateEmployeeId"));
        delegation.setPrincipalPositionId(normalizeBlank(delegation.getPrincipalPositionId()));
        delegation.setDelegatePositionId(normalizeBlank(delegation.getDelegatePositionId()));
        if (delegation.getDelegationType() == null) {
            delegation.setDelegationType(EmployeeDelegationType.BUSINESS);
        }
        if (delegation.getEnabled() == null) {
            delegation.setEnabled(Boolean.TRUE);
        }
    }

    @Override
    protected void validateBeforeSave(EmployeeDelegation delegation) {
        validateDelegationReferences(delegation);
        rejectDuplicate(delegation, duplicateCriteria(delegation), "employee delegation already exists");
    }

    public List<EmployeeDelegation> delegationsByPrincipal(String principalEmployeeId) {
        String validPrincipal = Preconditions.requireText(principalEmployeeId, "principalEmployeeId");
        return list(Criteria.of().eq("principalEmployeeId", validPrincipal), ALL,
                Sort.desc("updatedAt"), Sort.desc("createdAt"));
    }

    public List<EmployeeDelegation> delegationsByDelegate(String delegateEmployeeId) {
        String validDelegate = Preconditions.requireText(delegateEmployeeId, "delegateEmployeeId");
        return list(Criteria.of().eq("delegateEmployeeId", validDelegate), ALL,
                Sort.desc("updatedAt"), Sort.desc("createdAt"));
    }

    public String addDelegation(String principalEmployeeId, EmployeeDelegation delegation) {
        delegation.setPrincipalEmployeeId(Preconditions.requireText(principalEmployeeId, "principalEmployeeId"));
        return insert(delegation);
    }

    public int updateDelegation(String principalEmployeeId, String delegationId, EmployeeDelegation delegation) {
        EmployeeDelegation existing = requirePrincipalDelegation(principalEmployeeId, delegationId);
        delegation.setId(Preconditions.requireText(delegationId, "delegationId"));
        delegation.setPrincipalEmployeeId(Preconditions.requireText(principalEmployeeId, "principalEmployeeId"));
        delegation.setEnabled(existing.getEnabled());
        return update(delegation);
    }

    public int deleteDelegation(String principalEmployeeId, String delegationId) {
        EmployeeDelegation delegation = requirePrincipalDelegation(principalEmployeeId, delegationId);
        return delete(delegation);
    }

    public int enableDelegation(String principalEmployeeId, String delegationId) {
        requirePrincipalDelegation(principalEmployeeId, delegationId);
        return enable(delegationId);
    }

    public int disableDelegation(String principalEmployeeId, String delegationId) {
        requirePrincipalDelegation(principalEmployeeId, delegationId);
        return disable(delegationId);
    }

    private EmployeeDelegation requirePrincipalDelegation(String principalEmployeeId, String delegationId) {
        String validPrincipal = Preconditions.requireText(principalEmployeeId, "principalEmployeeId");
        String validDelegationId = Preconditions.requireText(delegationId, "delegationId");
        EmployeeDelegation delegation = select(validDelegationId);
        if (delegation == null || !validPrincipal.equals(delegation.getPrincipalEmployeeId())) {
            throw new PlatformException("employee delegation does not belong to principal employee: "
                    + validDelegationId);
        }
        return delegation;
    }

    private void validateDelegationReferences(EmployeeDelegation delegation) {
        if (delegation.getPrincipalEmployeeId().equals(delegation.getDelegateEmployeeId())) {
            throw new PlatformException("employee delegation delegate must differ from principal");
        }
        employeeService.requireEnabled(delegation.getPrincipalEmployeeId(),
                "principal employee is not active: " + delegation.getPrincipalEmployeeId());
        employeeService.requireEnabled(delegation.getDelegateEmployeeId(),
                "delegate employee is not active: " + delegation.getDelegateEmployeeId());
        requirePositionOwner(delegation.getPrincipalPositionId(), delegation.getPrincipalEmployeeId(),
                "principal position does not belong to principal employee: ");
        requirePositionOwner(delegation.getDelegatePositionId(), delegation.getDelegateEmployeeId(),
                "delegate position does not belong to delegate employee: ");
        if (delegation.getEffectiveFrom() != null && delegation.getEffectiveTo() != null
                && !delegation.getEffectiveFrom().isBefore(delegation.getEffectiveTo())) {
            throw new PlatformException("employee delegation effectiveFrom must be before effectiveTo");
        }
    }

    private void requirePositionOwner(String positionId, String employeeId, String message) {
        if (positionId == null) {
            return;
        }
        EmployeePosition position = employeePositionService.select(positionId);
        if (position == null
                || !Boolean.TRUE.equals(position.getEnabled())
                || !employeeId.equals(position.getEmployeeId())) {
            throw new PlatformException(message + positionId);
        }
    }

    private Criteria duplicateCriteria(EmployeeDelegation delegation) {
        Criteria criteria = Criteria.of()
                .eq("delegationType", delegation.getDelegationType())
                .eq("principalEmployeeId", delegation.getPrincipalEmployeeId())
                .eq("delegateEmployeeId", delegation.getDelegateEmployeeId());
        eqOrIsNull(criteria, "principalPositionId", delegation.getPrincipalPositionId());
        eqOrIsNull(criteria, "delegatePositionId", delegation.getDelegatePositionId());
        eqOrIsNull(criteria, "effectiveFrom", delegation.getEffectiveFrom());
        eqOrIsNull(criteria, "effectiveTo", delegation.getEffectiveTo());
        return criteria;
    }

    private void eqOrIsNull(Criteria criteria, String field, Object value) {
        if (value == null) {
            criteria.isNull(field);
            return;
        }
        criteria.eq(field, value);
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
