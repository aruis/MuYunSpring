package net.ximatai.muyun.spring.iam.role;

public record EffectiveRoleGrant(
        String roleId,
        RoleGrantSubjectType sourceType,
        String sourceId,
        String organizationId,
        String departmentId,
        String employeePositionId
) {
}
