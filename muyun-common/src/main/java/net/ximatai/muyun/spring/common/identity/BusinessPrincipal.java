package net.ximatai.muyun.spring.common.identity;

import java.util.Objects;

public record BusinessPrincipal(
        String userId,
        String employeeId,
        String organizationId,
        String departmentId,
        String employeePositionId,
        BusinessPrincipalSource source
) {
    public BusinessPrincipal {
        userId = normalize(userId);
        employeeId = normalize(employeeId);
        organizationId = normalize(organizationId);
        departmentId = normalize(departmentId);
        employeePositionId = normalize(employeePositionId);
        source = source == null ? BusinessPrincipalSource.CURRENT_USER : source;
        if (userId == null && employeeId == null && employeePositionId == null) {
            throw new IllegalArgumentException("business principal requires userId, employeeId or employeePositionId");
        }
    }

    public static BusinessPrincipal userAccount(String userId) {
        return new BusinessPrincipal(userId, null, null, null, null, BusinessPrincipalSource.CURRENT_USER);
    }

    public static BusinessPrincipal employee(String employeeId, String organizationId, String departmentId) {
        return new BusinessPrincipal(null, employeeId, organizationId, departmentId, null,
                BusinessPrincipalSource.DELEGATION);
    }

    public static BusinessPrincipal employeePosition(String employeeId,
                                                     String organizationId,
                                                     String departmentId,
                                                     String employeePositionId) {
        return new BusinessPrincipal(null, employeeId, organizationId, departmentId,
                Objects.requireNonNull(normalize(employeePositionId), "employeePositionId must not be blank"),
                BusinessPrincipalSource.DELEGATION);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
