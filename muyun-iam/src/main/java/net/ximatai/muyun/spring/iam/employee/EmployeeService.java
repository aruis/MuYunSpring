package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPhase;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EmployeeService extends TenantStandardBusinessService<Employee> implements
        SoftDeleteAbility<Employee>,
        EnableAbility<Employee>,
        SortAbility<Employee>,
        ReferenceAbility<Employee>,
        InitialDataAbility<Employee>,
        QueryAbility<Employee> {
    public static final String MODULE_ALIAS = "iam.employee";
    public static final String PLATFORM_ADMIN_EMPLOYEE_ID = "platform.employee.admin";
    public static final String PLATFORM_ADMIN_EMPLOYEE_NO = "ADMIN";
    public static final String PLATFORM_ADMIN_EMPLOYEE_TITLE = "平台管理员";

    private final OrganizationService organizationService;
    private final DepartmentService departmentService;

    @Autowired
    public EmployeeService(EmployeeDao employeeDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           OrganizationService organizationService,
                           DepartmentService departmentService) {
        super(MODULE_ALIAS, Employee.class, employeeDao, activeTenantVerifier);
        this.organizationService = organizationService;
        this.departmentService = departmentService;
    }

    @Override
    public void normalizeBeforeMutation(Employee employee) {
        employee.setOrganizationId(Preconditions.requireText(employee.getOrganizationId(), "organizationId"));
        employee.setDepartmentId(Preconditions.requireText(employee.getDepartmentId(), "departmentId"));
        employee.setEmployeeNo(Preconditions.requireText(employee.getEmployeeNo(), "employeeNo"));
        employee.setTitle(Preconditions.requireText(employee.getTitle(), "employeeName"));
        employee.setGender(normalizeBlank(employee.getGender()));
        employee.setMobile(normalizeBlank(employee.getMobile()));
        employee.setEmail(normalizeBlank(employee.getEmail()));
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.defaults()
                .phase(InitialDataPhase.TENANT_INITIAL_DATA)
                .order(45)
                .tenant(TenantService.PLATFORM_TENANT_ID);
    }

    @Override
    public List<Employee> initialData() {
        Employee employee = new Employee();
        employee.setId(PLATFORM_ADMIN_EMPLOYEE_ID);
        employee.setOrganizationId(OrganizationService.PLATFORM_ROOT_ORGANIZATION_ID);
        employee.setDepartmentId(DepartmentService.PLATFORM_ROOT_DEPARTMENT_ID);
        employee.setEmployeeNo(PLATFORM_ADMIN_EMPLOYEE_NO);
        employee.setTitle(PLATFORM_ADMIN_EMPLOYEE_TITLE);
        employee.setEnabled(Boolean.TRUE);
        employee.setSortOrder(1);
        return List.of(employee);
    }

    @Override
    protected void validateBeforeSave(Employee employee) {
        organizationService.requireEnabled(employee.getOrganizationId(),
                "organization is not active: " + employee.getOrganizationId());
        Department department = departmentService.requireEnabled(employee.getDepartmentId(),
                "department is not active: " + employee.getDepartmentId());
        if (!SortAbility.sameValue(employee.getOrganizationId(), department.getOrganizationId())) {
            throw new PlatformException("Employee department must belong to the same organization");
        }
        rejectDuplicate(employee, Criteria.of()
                        .eq("organizationId", employee.getOrganizationId())
                        .eq("employeeNo", employee.getEmployeeNo()),
                "employeeNo must be unique within organization: " + employee.getEmployeeNo());
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("organizationId", QueryOperator.EQ, QueryOperator.IN).withTitle("所属机构"))
                .field(QueryField.of("departmentId", QueryOperator.EQ, QueryOperator.IN).withTitle("所属部门"))
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("employeeNo", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("职员编号").withQuickSearch().withSortable())
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("职员姓名").withQuickSearch().withSortable())
                .field(QueryField.of("gender", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("性别"))
                .field(QueryField.of("mobile", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("手机号").withQuickSearch())
                .field(QueryField.of("email", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("邮箱").withQuickSearch())
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
                .externalCriteria("departmentScope", this::departmentScopeCriteria)
                .defaultSort(net.ximatai.muyun.database.core.orm.Sort.asc("sortOrder"))
                .defaultSort(net.ximatai.muyun.database.core.orm.Sort.asc("employeeNo"))
                .build();
    }

    private Criteria departmentScopeCriteria(Object value) {
        Map<?, ?> scope = requireMap(value, "departmentScope");
        String organizationId = text(scope.get("organizationId"));
        String departmentId = text(scope.get("departmentId"));
        boolean includeChildren = Boolean.TRUE.equals(scope.get("includeChildren"));
        Criteria criteria = Criteria.of();
        if (organizationId != null) {
            criteria.eq("organizationId", organizationId);
        }
        if (departmentId == null) {
            return criteria;
        }
        if (!includeChildren) {
            return criteria.eq("departmentId", departmentId);
        }
        String validOrganizationId = requireText(organizationId, "departmentScope.organizationId");
        List<String> ids = departmentService.selfAndDescendantIds(validOrganizationId, departmentId);
        return criteria.in("departmentId", ids.isEmpty() ? List.of("__missing_department__") : ids);
    }

    private Map<?, ?> requireMap(Object value, String label) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalArgumentException(label + " must be an object");
    }

    private String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    @Override
    public Criteria sortScope(Employee employee) {
        return sortScopeByFields(employee, "organizationId", "departmentId");
    }

    @Override
    public void validateSortScope(Employee left, Employee right) {
        validateSortScopeByFields(left, right,
                "Employee sort can only move records within the same department",
                "organizationId", "departmentId");
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
