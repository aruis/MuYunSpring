package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeePositionService extends TenantStandardBusinessService<EmployeePosition> implements
        SoftDeleteAbility<EmployeePosition>,
        EnableAbility<EmployeePosition>,
        SortAbility<EmployeePosition> {
    public static final String MODULE_ALIAS = "iam.employee_position";

    private final EmployeeService employeeService;
    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final PositionService positionService;

    @Autowired
    public EmployeePositionService(EmployeePositionDao employeePositionDao,
                                   ActiveTenantVerifier activeTenantVerifier,
                                   EmployeeService employeeService,
                                   OrganizationService organizationService,
                                   DepartmentService departmentService,
                                   PositionService positionService) {
        super(MODULE_ALIAS, EmployeePosition.class, employeePositionDao, activeTenantVerifier);
        this.employeeService = employeeService;
        this.organizationService = organizationService;
        this.departmentService = departmentService;
        this.positionService = positionService;
    }

    @Override
    public void normalizeBeforeMutation(EmployeePosition relation) {
        relation.setEmployeeId(Preconditions.requireText(relation.getEmployeeId(), "employeeId"));
        relation.setOrganizationId(Preconditions.requireText(relation.getOrganizationId(), "organizationId"));
        relation.setDepartmentId(Preconditions.requireText(relation.getDepartmentId(), "departmentId"));
        relation.setPositionId(Preconditions.requireText(relation.getPositionId(), "positionId"));
        relation.setPrimaryPosition(Boolean.TRUE.equals(relation.getPrimaryPosition()));
    }

    @Override
    protected void validateBeforeSave(EmployeePosition relation) {
        Employee employee = employeeService.requireEnabled(relation.getEmployeeId(),
                "employee is not active: " + relation.getEmployeeId());
        organizationService.requireEnabled(relation.getOrganizationId(),
                "organization is not active: " + relation.getOrganizationId());
        Department department = departmentService.requireEnabled(relation.getDepartmentId(),
                "department is not active: " + relation.getDepartmentId());
        positionService.requireEnabled(relation.getPositionId(),
                "position is not active: " + relation.getPositionId());
        if (!SortAbility.sameValue(relation.getOrganizationId(), department.getOrganizationId())) {
            throw new PlatformException("Employee position department must belong to the same organization");
        }
        if (Boolean.TRUE.equals(relation.getPrimaryPosition())) {
            validatePrimaryPosition(relation, employee);
        }
        rejectDuplicate(relation, Criteria.of()
                        .eq("employeeId", relation.getEmployeeId())
                        .eq("organizationId", relation.getOrganizationId())
                        .eq("departmentId", relation.getDepartmentId())
                        .eq("positionId", relation.getPositionId()),
                "employee position already exists");
    }

    @Override
    public Criteria sortScope(EmployeePosition relation) {
        return sortScopeByFields(relation, "employeeId");
    }

    @Override
    public void validateSortScope(EmployeePosition left, EmployeePosition right) {
        validateSortScopeByFields(left, right,
                "Employee position sort can only move records within the same employee", "employeeId");
    }

    public List<EmployeePosition> positions(String employeeId) {
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        return list(employeeCriteria(validEmployeeId), new PageRequest(0, Integer.MAX_VALUE),
                Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public String addPosition(String employeeId, EmployeePosition relation) {
        relation.setEmployeeId(Preconditions.requireText(employeeId, "employeeId"));
        return insert(relation);
    }

    public int updatePosition(String employeeId, String relationId, EmployeePosition relation) {
        requireEmployeePosition(employeeId, relationId);
        relation.setId(Preconditions.requireText(relationId, "relationId"));
        relation.setEmployeeId(Preconditions.requireText(employeeId, "employeeId"));
        return update(relation);
    }

    public int deletePosition(String employeeId, String relationId) {
        EmployeePosition relation = requireEmployeePosition(employeeId, relationId);
        return delete(relation);
    }

    public int enablePosition(String employeeId, String relationId) {
        requireEmployeePosition(employeeId, relationId);
        return enable(relationId);
    }

    public int disablePosition(String employeeId, String relationId) {
        requireEmployeePosition(employeeId, relationId);
        return disable(relationId);
    }

    public void moveEmployeePosition(String employeeId, String relationId, String previousId, String nextId) {
        requireEmployeePosition(employeeId, relationId);
        if (previousId != null && !previousId.isBlank()) {
            requireEmployeePosition(employeeId, previousId);
            moveAfter(relationId, previousId);
            return;
        }
        if (nextId != null && !nextId.isBlank()) {
            requireEmployeePosition(employeeId, nextId);
            moveBefore(relationId, nextId);
            return;
        }
        throw new IllegalArgumentException("sort requires previousId or nextId");
    }

    private EmployeePosition requireEmployeePosition(String employeeId, String relationId) {
        String validEmployeeId = Preconditions.requireText(employeeId, "employeeId");
        String validRelationId = Preconditions.requireText(relationId, "relationId");
        EmployeePosition relation = select(validRelationId);
        if (relation == null || !SortAbility.sameValue(validEmployeeId, relation.getEmployeeId())) {
            throw new PlatformException("employee position does not belong to employee: " + validRelationId);
        }
        return relation;
    }

    private Criteria employeeCriteria(String employeeId) {
        return Criteria.of().eq("employeeId", employeeId);
    }

    private void validatePrimaryPosition(EmployeePosition relation, Employee employee) {
        if (!SortAbility.sameValue(relation.getOrganizationId(), employee.getOrganizationId())
                || !SortAbility.sameValue(relation.getDepartmentId(), employee.getDepartmentId())) {
            throw new PlatformException("Primary employee position must match employee main organization and department");
        }
        rejectDuplicate(relation, Criteria.of()
                        .eq("employeeId", relation.getEmployeeId())
                        .eq("primaryPosition", Boolean.TRUE)
                        .eq("enabled", Boolean.TRUE),
                "employee can only have one primary position");
    }
}
