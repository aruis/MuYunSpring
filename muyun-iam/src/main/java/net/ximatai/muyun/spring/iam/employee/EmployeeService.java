package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.DataScopeFieldMappingAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ModuleReadProjection;
import net.ximatai.muyun.spring.ability.reference.ModuleReadProjectionContributor;
import net.ximatai.muyun.spring.ability.reference.ModuleReferencePath;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.action.BusinessExceptions;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeFieldMapping;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class EmployeeService extends TenantStandardBusinessService<Employee> implements
        SoftDeleteAbility<Employee>,
        RecycleBinAbility<Employee>,
        EnableAbility<Employee>,
        SortAbility<Employee>,
        ReferenceAbility<Employee>,
        DataScopeAbility<Employee>,
        DataScopeFieldMappingAbility,
        QueryAbility<Employee>,
        ModuleReadProjectionContributor {
    public static final String MODULE_ALIAS = "iam.employee";
    private static final DataScopeFieldMapping DATA_SCOPE_FIELD_MAPPING =
            DataScopeFieldMapping.of(null, "organizationId", "departmentId");

    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final Supplier<DataScopeCriteriaService> dataScopeCriteriaService;

    public EmployeeService(EmployeeDao employeeDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           OrganizationService organizationService,
                           DepartmentService departmentService) {
        this(employeeDao, activeTenantVerifier, organizationService, departmentService, Optional.empty());
    }

    @Autowired
    public EmployeeService(EmployeeDao employeeDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           OrganizationService organizationService,
                           DepartmentService departmentService,
                           ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Employee.class, employeeDao, activeTenantVerifier);
        this.organizationService = organizationService;
        this.departmentService = departmentService;
        this.dataScopeCriteriaService = () -> dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new);
    }

    public EmployeeService(EmployeeDao employeeDao,
                           ActiveTenantVerifier activeTenantVerifier,
                           OrganizationService organizationService,
                           DepartmentService departmentService,
                           Optional<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Employee.class, employeeDao, activeTenantVerifier);
        this.organizationService = organizationService;
        this.departmentService = departmentService;
        Optional<DataScopeCriteriaService> criteriaService = dataScopeCriteriaService == null
                ? Optional.empty()
                : dataScopeCriteriaService;
        this.dataScopeCriteriaService = () -> criteriaService
                .<DataScopeCriteriaService>map(service -> service)
                .orElseGet(AllowAllDataScopeCriteriaService::new);
    }

    @Override
    public DataScopeCriteriaService getDataScopeCriteriaService() {
        return dataScopeCriteriaService.get();
    }

    @Override
    public boolean canAccessRecycleBinRecord(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        DataScopeCriteriaResult scope = recycleBinScope(Criteria.of()
                .eq(StandardEntitySchema.ID_FIELD, id));
        return withDataScopeTenant(scope,
                () -> !getDao().query(scope.criteria(), PageRequest.of(1, 1)).isEmpty());
    }

    @Override
    public String getDeletionEntityAlias() {
        return "employee";
    }

    @Override
    public DataScopeFieldMapping dataScopeFieldMapping() {
        return DATA_SCOPE_FIELD_MAPPING;
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
    protected void validateBeforeSave(Employee employee) {
        organizationService.requireEnabled(employee.getOrganizationId(),
                "organization is not active: " + employee.getOrganizationId());
        Department department = departmentService.requireEnabled(employee.getDepartmentId(),
                "department is not active: " + employee.getDepartmentId());
        if (!SortAbility.sameValue(employee.getOrganizationId(), department.getOrganizationId())) {
            throw BusinessExceptions.warning("iam.employee.department-organization-mismatch",
                    "职员所属部门必须隶属于同一机构");
        }
        rejectSoftDeletedEmployeeNoConflict(employee);
        rejectDuplicate(employee, Criteria.of()
                        .eq("organizationId", employee.getOrganizationId())
                        .eq("employeeNo", employee.getEmployeeNo()),
                "employeeNo must be unique within organization: " + employee.getEmployeeNo());
    }

    private DataScopeCriteriaResult recycleBinScope(Criteria criteria) {
        Criteria retained = RecycleBinAbility.super.recycleBinCriteria(criteria)
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.TRUE);
        return readScope(PlatformAction.RECYCLE_BIN_QUERY, retained);
    }

    private void rejectSoftDeletedEmployeeNoConflict(Employee employee) {
        Criteria criteria = Criteria.of()
                .eq(StandardEntitySchema.TENANT_ID_FIELD,
                        Preconditions.requireText(employee.getTenantId(), "employee.tenantId"))
                .eq("organizationId", employee.getOrganizationId())
                .eq("employeeNo", employee.getEmployeeNo())
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.TRUE);
        Employee retained = getDao().query(criteria, PageRequest.of(1, 1)).stream()
                .filter(existing -> !java.util.Objects.equals(existing.getId(), employee.getId()))
                .findFirst()
                .orElse(null);
        if (retained == null) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("resourceModuleAlias", MODULE_ALIAS);
        details.put("resourceRecordId", retained.getId());
        if (retained.getDeletedAt() != null) {
            details.put("deletedAt", retained.getDeletedAt());
        }
        details.put("recoveryAvailable", Boolean.TRUE);
        throw PlatformErrors.conflict(PlatformErrorCodes.RESOURCE_SOFT_DELETED_CONFLICT,
                "Employee number is retained by a soft-deleted employee; restore it from the recycle bin before reusing it",
                details);
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

    @Override
    public List<ModuleReadProjection> moduleReadProjections() {
        return List.of(
                ModuleReadProjection.of(
                        ModuleReferencePath.from(Employee::getOrganizationId)
                                .select(Organization::getTitle),
                        "organizationTitle"),
                ModuleReadProjection.filterableOnly(
                        ModuleReferencePath.inverseOne(EmployeeAccount::getEmployeeId)
                                .then(EmployeeAccount::getUserId)
                                .select(UserAccount::getUsername),
                        "username"),
                ModuleReadProjection.exists(
                        ModuleReferencePath.inverseOne(EmployeeAccount::getEmployeeId)
                                .select(EmployeeAccount::getId),
                        "accountBound")
        );
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
