package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrant;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrant;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
@PlatformStaticModule(application = "iam", alias = "iam.role", title = "角色管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 70)
@Path("/iam.role")
public class RoleWebController extends WebSupport<RoleService> implements
        CrudWeb<Role, RoleService>,
        EnableWeb<Role, RoleService>,
        SortWeb<Role, RoleService> {
    private final RoleGrantableActionResolver grantableActionResolver;
    private final MenuService menuService;

    public RoleWebController(RoleGrantableActionResolver grantableActionResolver) {
        this(grantableActionResolver, (MenuService) null);
    }

    @Inject
    public RoleWebController(RoleGrantableActionResolver grantableActionResolver,
                             ObjectProvider<MenuService> menuServiceProvider) {
        this(grantableActionResolver,
                menuServiceProvider == null ? null : menuServiceProvider.getIfAvailable());
    }

    private RoleWebController(RoleGrantableActionResolver grantableActionResolver,
                              MenuService menuService) {
        this.grantableActionResolver = grantableActionResolver;
        this.menuService = menuService;
    }

    @GET
    @Path("/{roleId}/account-grants")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public List<AccountRoleGrant> accountRoleGrants(@PathParam("roleId") String roleId) {
        return webScope(() -> service().accountRoleGrants(roleId));
    }

    @POST
    @Path("/{roleId}/account-grants")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public String grantAccountRole(@PathParam("roleId") String roleId,
                                   AccountRoleGrantRequest request) {
        return webScope(() -> service().grantAccountRole(
                roleId,
                request.userId(),
                request.managementScopeType(),
                request.managementScopeId()));
    }

    @POST
    @Path("/{roleId}/account-grants/{grantId}/delete")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse deleteAccountRoleGrant(@PathParam("roleId") String roleId,
                                                   @PathParam("grantId") String grantId) {
        return webScope(() -> new WebCountResponse(service().deleteAccountRoleGrant(roleId, grantId)));
    }

    @GET
    @Path("/{roleId}/employment-grants")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public List<EmploymentRoleGrant> employmentRoleGrants(@PathParam("roleId") String roleId) {
        return webScope(() -> service().employmentRoleGrants(roleId));
    }

    @POST
    @Path("/{roleId}/employment-grants")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public String grantEmploymentRole(@PathParam("roleId") String roleId,
                                      EmploymentRoleGrantRequest request) {
        return webScope(() -> service().grantEmploymentRole(roleId, request.employeePositionId()));
    }

    @POST
    @Path("/{roleId}/employment-grants/{grantId}/delete")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse deleteEmploymentRoleGrant(@PathParam("roleId") String roleId,
                                                      @PathParam("grantId") String grantId) {
        return webScope(() -> new WebCountResponse(service().deleteEmploymentRoleGrant(roleId, grantId)));
    }

    @POST
    @Path("/grant/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse grantAction(@PathParam("roleId") String roleId,
                                        GrantActionRequest request) {
        return webScope(() -> new WebCountResponse(service().grantAction(
                roleId,
                request.moduleAlias(),
                request.actionCode(),
                request.dataScopePolicy(),
                request.tenantScopePolicy(),
                request.scopeCondition(),
                request.referenceFieldId(),
                request.referenceActionCode()
        )));
    }

    @POST
    @Path("/grant/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse grantActions(@PathParam("roleId") String roleId,
                                         GrantActionsRequest request) {
        return webScope(() -> new WebCountResponse(service().grantActions(
                roleId,
                request.actions().stream()
                        .map(GrantActionRequest::toCommand)
                        .toList()
        )));
    }

    @POST
    @Path("/revoke/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse revokeAction(@PathParam("roleId") String roleId,
                                         RevokeActionRequest request) {
        return webScope(() -> new WebCountResponse(service().revokeAction(
                roleId, request.moduleAlias(), request.actionCode())));
    }

    @POST
    @Path("/revoke/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse revokeActions(@PathParam("roleId") String roleId,
                                          RevokeActionsRequest request) {
        return webScope(() -> new WebCountResponse(service().revokeActions(
                roleId,
                request.actions().stream()
                        .map(RevokeActionRequest::toCommand)
                        .toList()
        )));
    }

    @POST
    @Path("/permissionMatrix/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public RolePermissionMatrix permissionMatrix(@PathParam("roleId") String roleId,
                                                 PermissionMatrixRequest request) {
        return webScope(() -> service().permissionMatrix(
                roleId,
                grantableActionResolver.resolve(request.moduleAliases())
        ));
    }

    @GET
    @Path("/menuMatrix/{roleId}/{schemeId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebListResponse<RoleMenuNode> menuMatrix(@PathParam("roleId") String roleId,
                                                    @PathParam("schemeId") String schemeId) {
        return webScope(() -> {
            if (menuService == null) {
                throw new IllegalStateException("menu service is not available");
            }
            List<Menu> roots = menuService.rootMenus(schemeId);
            Map<String, Boolean> grantedByModule = menuGrantState(roleId, roots);
            return new WebListResponse<>(roots.stream()
                    .map(menu -> roleMenuNode(menu, grantedByModule))
                    .toList());
        });
    }

    public record AccountRoleGrantRequest(
            String userId,
            ManagementScopeType managementScopeType,
            String managementScopeId
    ) {
    }

    public record EmploymentRoleGrantRequest(
            String employeePositionId
    ) {
    }

    public record GrantActionRequest(
            String moduleAlias,
            String actionCode,
            DataScopePolicy dataScopePolicy,
            TenantScopePolicy tenantScopePolicy,
            String scopeCondition,
            String referenceFieldId,
            String referenceActionCode
    ) {
        RoleService.ActionGrantCommand toCommand() {
            return new RoleService.ActionGrantCommand(
                    moduleAlias,
                    actionCode,
                    dataScopePolicy,
                    tenantScopePolicy,
                    scopeCondition,
                    referenceFieldId,
                    referenceActionCode
            );
        }
    }

    public record GrantActionsRequest(List<GrantActionRequest> actions) {
        public GrantActionsRequest {
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }

    public record RevokeActionRequest(
            String moduleAlias,
            String actionCode
    ) {
        RoleService.ActionRevokeCommand toCommand() {
            return new RoleService.ActionRevokeCommand(moduleAlias, actionCode);
        }
    }

    public record RevokeActionsRequest(List<RevokeActionRequest> actions) {
        public RevokeActionsRequest {
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }

    public record PermissionMatrixRequest(List<String> moduleAliases) {
    }

    public record RoleMenuNode(
            Menu menu,
            boolean granted,
            List<RoleMenuNode> children
    ) {
        public RoleMenuNode {
            children = children == null ? List.of() : List.copyOf(children);
        }
    }

    private Map<String, Boolean> menuGrantState(String roleId, List<Menu> roots) {
        List<String> moduleAliases = flattenMenus(roots).stream()
                .filter(this::isModuleEntryMenu)
                .map(Menu::getModuleAlias)
                .distinct()
                .toList();
        if (moduleAliases.isEmpty()) {
            service().permissionMatrix(roleId, List.of());
            return Map.of();
        }
        RolePermissionMatrix matrix = service().permissionMatrix(roleId, moduleAliases.stream()
                .map(moduleAlias -> GrantableAction.ofPlatformDefaults(moduleAlias, PlatformAction.MENU))
                .toList());
        return matrix.modules().stream()
                .flatMap(module -> module.actions().stream())
                .collect(Collectors.toMap(
                        RolePermissionAction::moduleAlias,
                        RolePermissionAction::granted,
                        (left, right) -> left || right,
                        LinkedHashMap::new
                ));
    }

    private RoleMenuNode roleMenuNode(Menu menu, Map<String, Boolean> grantedByModule) {
        boolean granted = isModuleEntryMenu(menu)
                && Boolean.TRUE.equals(grantedByModule.get(menu.getModuleAlias()));
        return new RoleMenuNode(
                menu,
                granted,
                menuService.children(menu.getSchemeId(), menu.getId()).stream()
                        .map(child -> roleMenuNode(child, grantedByModule))
                        .toList()
        );
    }

    private boolean isModuleEntryMenu(Menu menu) {
        return menu.getModuleAlias() != null
                && !menu.getModuleAlias().isBlank();
    }

    private List<Menu> flattenMenus(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        return menus.stream()
                .flatMap(menu -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(menu),
                        flattenMenus(menuService.children(menu.getSchemeId(), menu.getId())).stream()
                ))
                .toList();
    }
}
