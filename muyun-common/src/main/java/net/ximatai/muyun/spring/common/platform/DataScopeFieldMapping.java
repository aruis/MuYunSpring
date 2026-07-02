package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

public record DataScopeFieldMapping(
        String ownerUserField,
        String organizationField,
        String departmentField,
        String assigneeColumn,
        String memberColumn
) {
    public static final DataScopeFieldMapping STANDARD = new DataScopeFieldMapping(
            PlatformAbilityFields.AUTH_USER_FIELD,
            PlatformAbilityFields.AUTH_ORGANIZATION_FIELD,
            PlatformAbilityFields.AUTH_DEPARTMENT_FIELD,
            PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN,
            PlatformAbilityFields.AUTH_MEMBER_COLUMN
    );

    public DataScopeFieldMapping {
        ownerUserField = normalize(ownerUserField);
        organizationField = normalize(organizationField);
        departmentField = normalize(departmentField);
        assigneeColumn = normalize(assigneeColumn);
        memberColumn = normalize(memberColumn);
    }

    public static DataScopeFieldMapping of(String ownerUserField,
                                           String organizationField,
                                           String departmentField) {
        return new DataScopeFieldMapping(ownerUserField, organizationField, departmentField, null, null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
