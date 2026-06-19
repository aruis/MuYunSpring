package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPhase;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.RoleAction;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleGrant;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantSubjectType;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PlatformSuperAdminAuthorizationInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final RoleService roleService;
    private final RoleGrantDao roleGrantDao;
    private final RoleActionDao roleActionDao;
    private final PlatformModuleActionService moduleActionService;

    public PlatformSuperAdminAuthorizationInitialDataDeclarationProvider(RoleService roleService,
                                                                         RoleGrantDao roleGrantDao,
                                                                         RoleActionDao roleActionDao,
                                                                         PlatformModuleActionService moduleActionService) {
        this.roleService = roleService;
        this.roleGrantDao = roleGrantDao;
        this.roleActionDao = roleActionDao;
        this.moduleActionService = moduleActionService;
    }

    @Override
    public String name() {
        return "platform.super-admin-authorization";
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public InitialDataPhase phase() {
        return InitialDataPhase.AUTHORIZATION_INITIAL_DATA;
    }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        List<InitialDataDeclaration<?>> declarations = new ArrayList<>();
        declarations.add(roleGrant());
        grantableActions().forEach(action -> declarations.add(roleAction(action)));
        return declarations;
    }

    private InitialDataDeclaration<RoleGrant> roleGrant() {
        RoleGrant desired = new RoleGrant();
        desired.setRoleId(RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID);
        desired.setSubjectType(RoleGrantSubjectType.USER_ACCOUNT);
        desired.setSubjectId(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
        desired.setEnabled(Boolean.TRUE);
        return InitialDataDeclaration.reconcileManaged(
                roleGrantKey(),
                RoleGrant.class,
                desired,
                () -> existingRoleGrant(desired),
                grant -> roleService.grantRole(grant.getRoleId(), grant.getSubjectType(), grant.getSubjectId()),
                grant -> roleService.grantRole(grant.getRoleId(), grant.getSubjectType(), grant.getSubjectId())
        ).inTenant(TenantService.PLATFORM_TENANT_ID);
    }

    private InitialDataDeclaration<RoleAction> roleAction(PlatformModuleAction moduleAction) {
        RoleAction desired = new RoleAction();
        desired.setRoleId(RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID);
        desired.setModuleAlias(moduleAction.getModuleAlias());
        desired.setActionCode(permissionActionCode(moduleAction));
        desired.setDataScopePolicy(Boolean.TRUE.equals(moduleAction.getDataAuth())
                ? DataScopePolicy.ALL
                : DataScopePolicy.NONE);
        desired.setTenantScopePolicy(TenantScopePolicy.ALL_TENANTS);
        desired.setEnabled(Boolean.TRUE);
        return InitialDataDeclaration.reconcileManaged(
                roleActionKey(desired),
                RoleAction.class,
                desired,
                () -> existingRoleAction(desired),
                ignored -> grantAction(moduleAction),
                ignored -> grantAction(moduleAction)
        ).inTenant(TenantService.PLATFORM_TENANT_ID);
    }

    private RoleGrant existingRoleGrant(RoleGrant desired) {
        return roleGrantDao.query(Criteria.of()
                        .eq("tenantId", TenantService.PLATFORM_TENANT_ID)
                        .eq("roleId", desired.getRoleId())
                        .eq("subjectType", desired.getSubjectType())
                        .eq("subjectId", desired.getSubjectId()),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private RoleAction existingRoleAction(RoleAction desired) {
        return roleActionDao.query(Criteria.of()
                        .eq("tenantId", TenantService.PLATFORM_TENANT_ID)
                        .eq("roleId", desired.getRoleId())
                        .eq("moduleAlias", desired.getModuleAlias())
                        .eq("actionCode", desired.getActionCode()),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private void grantAction(PlatformModuleAction moduleAction) {
        roleService.grantAction(
                RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID,
                moduleAction.getModuleAlias(),
                moduleAction.getActionCode(),
                Boolean.TRUE.equals(moduleAction.getDataAuth()) ? DataScopePolicy.ALL : DataScopePolicy.NONE,
                TenantScopePolicy.ALL_TENANTS
        );
    }

    private List<PlatformModuleAction> grantableActions() {
        Map<String, PlatformModuleAction> actions = new LinkedHashMap<>();
        moduleActionService.list(Criteria.of(), ALL, Sort.asc("moduleAlias"), Sort.asc("sortOrder")).stream()
                .filter(action -> Boolean.TRUE.equals(action.getEnabled()))
                .filter(action -> action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth()))
                .forEach(action -> actions.putIfAbsent(action.getModuleAlias() + ":" + permissionActionCode(action),
                        action));
        return List.copyOf(actions.values());
    }

    private String permissionActionCode(PlatformModuleAction action) {
        String permissionActionCode = action.getPermissionActionCode();
        return permissionActionCode == null || permissionActionCode.isBlank()
                ? action.getActionCode()
                : permissionActionCode;
    }

    private String roleGrantKey() {
        return RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID + ".grant."
                + UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID;
    }

    private String roleActionKey(RoleAction action) {
        return action.getRoleId() + ".action." + action.getModuleAlias() + "." + action.getActionCode();
    }
}
