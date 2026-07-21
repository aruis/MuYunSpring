package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeEmploymentReadService {
    private final EmployeePositionService employeePositionService;
    private final EmployeeService employeeService;
    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final EmployeeAccountService employeeAccountService;
    private final UserAccountService userAccountService;

    public EmployeeEmploymentReadService(EmployeePositionService employeePositionService, EmployeeService employeeService,
                                         OrganizationService organizationService, DepartmentService departmentService,
                                         PositionService positionService, EmployeeAccountService employeeAccountService,
                                         UserAccountService userAccountService) {
        this.employeePositionService = employeePositionService; this.employeeService = employeeService;
        this.organizationService = organizationService; this.departmentService = departmentService;
        this.positionService = positionService; this.employeeAccountService = employeeAccountService;
        this.userAccountService = userAccountService;
    }

    public PageResult<EmployeeEmploymentView> page(Query query) {
        Query normalized = query == null ? Query.defaults() : query;
        Criteria criteria = Criteria.of();
        if (!Boolean.FALSE.equals(normalized.enabledOnly())) criteria.eq("enabled", Boolean.TRUE);
        if (normalized.employeeId() != null && !normalized.employeeId().isBlank()) criteria.eq("employeeId", normalized.employeeId().trim());
        if (normalized.organizationId() != null && !normalized.organizationId().isBlank()) criteria.eq("organizationId", normalized.organizationId().trim());
        if (normalized.departmentId() != null && !normalized.departmentId().isBlank()) criteria.eq("departmentId", normalized.departmentId().trim());
        if (!Boolean.FALSE.equals(normalized.boundOnly())) {
            List<String> employeeIds = employeeAccountService.list(Criteria.of(), new PageRequest(0, Integer.MAX_VALUE)).stream().map(EmployeeAccount::getEmployeeId).distinct().toList();
            criteria.in("employeeId", employeeIds.isEmpty() ? List.of("__none__") : employeeIds);
        }
        PageResult<EmployeePosition> page = employeePositionService.pageQuery(criteria, normalized.pageRequest(), Sort.asc("employeeId"));
        return PageResult.of(page.getRecords().stream().map(this::view).toList(), page.getTotal(), normalized.pageRequest());
    }

    private EmployeeEmploymentView view(EmployeePosition relation) {
        Employee employee = employeeService.select(relation.getEmployeeId()); Organization organization = organizationService.select(relation.getOrganizationId());
        Department department = departmentService.select(relation.getDepartmentId()); Position position = positionService.select(relation.getPositionId());
        EmployeeAccount account = employeeAccountService.accountOfEmployee(relation.getEmployeeId()); UserAccount user = account == null ? null : userAccountService.select(account.getUserId());
        return new EmployeeEmploymentView(relation.getId(), relation.getEmployeeId(), employee == null ? null : employee.getEmployeeNo(), employee == null ? null : employee.getTitle(), relation.getOrganizationId(), organization == null ? null : organization.getTitle(), relation.getDepartmentId(), department == null ? null : department.getTitle(), relation.getPositionId(), position == null ? null : position.getTitle(), relation.getPrimaryPosition(), relation.getEnabled(), user == null ? null : user.getUsername());
    }

    public record Query(String employeeId, String organizationId, String departmentId, Boolean enabledOnly, Boolean boundOnly, PageRequest pageRequest) {
        static Query defaults() { return new Query(null, null, null, Boolean.TRUE, Boolean.TRUE, new PageRequest(0, 50)); }
        public PageRequest pageRequest() { return pageRequest == null ? new PageRequest(0, 50) : pageRequest; }
    }
    public record EmployeeEmploymentView(String id, String employeeId, String employeeNo, String employeeTitle, String organizationId, String organizationTitle, String departmentId, String departmentTitle, String positionId, String positionTitle, Boolean primaryPosition, Boolean enabled, String username) { }
}
