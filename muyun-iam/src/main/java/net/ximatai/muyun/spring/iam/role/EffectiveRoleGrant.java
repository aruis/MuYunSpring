package net.ximatai.muyun.spring.iam.role;

public record EffectiveRoleGrant(
        String roleId,
        RoleAssignmentType sourceType,
        String sourceId,
        String organizationId,
        String departmentId,
        String employeePositionId,
        ManagementScopeType managementScopeType,
        String managementScopeId
) {
    public static EffectiveRoleGrant account(String roleId,
                                             String userId,
                                             ManagementScopeType managementScopeType,
                                             String managementScopeId) {
        return new EffectiveRoleGrant(roleId, RoleAssignmentType.ACCOUNT, userId, null, null, null,
                managementScopeType, managementScopeId);
    }

    public static EffectiveRoleGrant employment(String roleId,
                                                String employeePositionId,
                                                String organizationId,
                                                String departmentId) {
        return new EffectiveRoleGrant(roleId, RoleAssignmentType.EMPLOYMENT, employeePositionId,
                organizationId, departmentId, employeePositionId, null, null);
    }
}
