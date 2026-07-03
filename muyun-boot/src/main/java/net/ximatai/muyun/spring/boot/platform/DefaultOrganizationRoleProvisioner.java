package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.iam.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.OrganizationCreationProvisioner;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public class DefaultOrganizationRoleProvisioner implements OrganizationCreationProvisioner {
    public static final String ORGANIZATION_ADMIN_ROLE_TITLE = "机构管理员";
    public static final String ORGANIZATION_ADMIN_ROLE_DESCRIPTION =
            "机构内置管理员角色，拥有当前机构及授权范围内的平台可授权动作和数据范围。";
    private static final String SYSTEM_OPERATOR_ID = "organization-provisioner";
    private static final String ROLE_ID_PREFIX = "organization_admin_";
    private static final int HASH_LENGTH = 16;

    private final RoleService roleService;
    private final BuiltInRolePermissionTemplateService rolePermissionTemplateService;

    public DefaultOrganizationRoleProvisioner(RoleService roleService,
                                              BuiltInRolePermissionTemplateService rolePermissionTemplateService) {
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
        this.rolePermissionTemplateService = Objects.requireNonNull(rolePermissionTemplateService,
                "rolePermissionTemplateService must not be null");
    }

    @Override
    public void afterOrganizationCreated(String tenantId, String organizationId) {
        ensureOrganizationAdminRole(tenantId, organizationId);
    }

    public Role ensureOrganizationAdminRole(String tenantId, String organizationId) {
        String validTenantId = Preconditions.requireText(tenantId, "tenantId");
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.systemUser(SYSTEM_OPERATOR_ID, "Organization Provisioner"));
             TenantContext.Scope ignoredTenant = TenantContext.use(validTenantId)) {
            Role role = roleService.ensureSystemManagedOrganizationAdminRole(
                    validTenantId,
                    validOrganizationId,
                    organizationAdminRoleId(validTenantId, validOrganizationId),
                    ORGANIZATION_ADMIN_ROLE_TITLE,
                    ORGANIZATION_ADMIN_ROLE_DESCRIPTION
            );
            rolePermissionTemplateService.applyOrganizationAdminTemplate(role.getId());
            return role;
        }
    }

    public static String organizationAdminRoleId(String tenantId, String organizationId) {
        return ROLE_ID_PREFIX + shortHash(Preconditions.requireText(tenantId, "tenantId")
                + ":" + Preconditions.requireText(organizationId, "organizationId"));
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
