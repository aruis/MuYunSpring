package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.util.List;

public final class DynamicAbilityFields {
    private DynamicAbilityFields() {
    }

    public static List<FieldDefinition> dataScopeFields() {
        return List.of(
                FieldDefinition.string(PlatformAbilityFields.AUTH_USER_FIELD, "Auth User")
                        .column(PlatformAbilityFields.AUTH_USER_COLUMN)
                        .length(PlatformAbilityFields.AUTH_USER_LENGTH),
                FieldDefinition.text(PlatformAbilityFields.AUTH_ASSIGNEE_FIELD, "Auth Assignees")
                        .column(PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN),
                FieldDefinition.text(PlatformAbilityFields.AUTH_MEMBER_FIELD, "Auth Members")
                        .column(PlatformAbilityFields.AUTH_MEMBER_COLUMN),
                FieldDefinition.string(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, "Auth Organization")
                        .column(PlatformAbilityFields.AUTH_ORGANIZATION_COLUMN)
                        .length(PlatformAbilityFields.AUTH_ORGANIZATION_LENGTH),
                FieldDefinition.string(PlatformAbilityFields.AUTH_MODULE_FIELD, "Auth Module")
                        .column(PlatformAbilityFields.AUTH_MODULE_COLUMN)
                        .length(PlatformAbilityFields.AUTH_MODULE_LENGTH)
        );
    }
}
