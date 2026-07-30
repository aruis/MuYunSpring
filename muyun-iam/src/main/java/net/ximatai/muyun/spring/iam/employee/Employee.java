package net.ximatai.muyun.spring.iam.employee;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.SortPartitionBy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;

@Getter
@Setter
@Table(name = "iam_employee", comment = "Employee")
@SortPartitionBy(fields = {"organizationId", "departmentId"}, message = "Employee sort can only move records within the same department")
@TenantUniqueConstraint(fields = {"organizationId", "employeeNo"})
@InitialDataFields(
        managed = {"organizationId", "departmentId", "employeeNo"},
        operator = {"title", "gender", "mobile", "email", "enabled", "sortOrder"}
)
public class Employee extends StandardEnabledSortableEntity {
    @Column(name = "organization_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Organization id")
    @ReferenceTo(target = OrganizationService.class,
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT))
    private String organizationId;

    @Column(name = "department_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Department id")
    @ReferenceTo(target = DepartmentService.class,
            integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT))
    private String departmentId;

    @Column(name = "employee_no", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Employee number")
    private String employeeNo;

    @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender")
    @Column(name = "gender", type = ColumnType.VARCHAR, length = 64, comment = "Gender")
    private String gender;

    private String genderTitle;

    @Column(name = "mobile", type = ColumnType.VARCHAR, length = 32, comment = "Mobile")
    private String mobile;

    @Column(name = "email", type = ColumnType.VARCHAR, length = 128, comment = "Email")
    private String email;
}
