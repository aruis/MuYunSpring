package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.role", title = "角色管理")
@RequestMapping("/iam.role")
public class RoleWebController extends WebSupport<RoleService> implements
        CrudWeb<Role, RoleService>,
        EnableWeb<Role, RoleService>,
        SortWeb<Role, RoleService> {
    private final RoleGrantableActionResolver grantableActionResolver;
    private final MenuService menuService;

    public RoleWebController(RoleGrantableActionResolver grantableActionResolver) {
        this(grantableActionResolver, (MenuService) null);
    }

    @Autowired
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

    @PostMapping("/users/{roleId}/bind")
    @CustomActionEndpoint(value = "roleUsers", title = "角色用户",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse bindUsers(@PathVariable String roleId,
                                      @RequestBody UserIdsRequest request) {
        return webScope(() -> new WebCountResponse(service().bindUsers(roleId, request.userIds())));
    }

    @PostMapping("/users/{roleId}/unbind")
    @CustomActionEndpoint(value = "roleUsers", title = "角色用户",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse unbindUsers(@PathVariable String roleId,
                                        @RequestBody UserIdsRequest request) {
        return webScope(() -> new WebCountResponse(service().unbindUsers(roleId, request.userIds())));
    }

    @GetMapping("/users/{roleId}")
    @CustomActionEndpoint(value = "roleUsers", title = "角色用户",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public List<String> userIds(@PathVariable String roleId) {
        return webScope(() -> service().userIds(roleId));
    }

    @PostMapping("/grant/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse grantAction(@PathVariable String roleId,
                                        @RequestBody GrantActionRequest request) {
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

    @PostMapping("/grant/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse grantActions(@PathVariable String roleId,
                                         @RequestBody GrantActionsRequest request) {
        return webScope(() -> new WebCountResponse(service().grantActions(
                roleId,
                request.actions().stream()
                        .map(GrantActionRequest::toCommand)
                        .toList()
        )));
    }

    @PostMapping("/wildcard-data-scope/{roleId}/grant")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse grantWildcardDataScopeAction(@PathVariable String roleId,
                                                         @RequestBody GrantWildcardDataScopeRequest request) {
        return webScope(() -> new WebCountResponse(service().grantWildcardDataScopeAction(
                roleId,
                request.actionCode(),
                request.dataScopePolicy(),
                request.tenantScopePolicy()
        )));
    }

    @PostMapping("/revoke/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse revokeAction(@PathVariable String roleId,
                                         @RequestBody RevokeActionRequest request) {
        return webScope(() -> new WebCountResponse(service().revokeAction(
                roleId, request.moduleAlias(), request.actionCode())));
    }

    @PostMapping("/revoke/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebCountResponse revokeActions(@PathVariable String roleId,
                                          @RequestBody RevokeActionsRequest request) {
        return webScope(() -> new WebCountResponse(service().revokeActions(
                roleId,
                request.actions().stream()
                        .map(RevokeActionRequest::toCommand)
                        .toList()
        )));
    }

    @PostMapping("/permissionMatrix/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public RolePermissionMatrix permissionMatrix(@PathVariable String roleId,
                                                 @RequestBody PermissionMatrixRequest request) {
        return webScope(() -> service().permissionMatrix(
                roleId,
                grantableActionResolver.resolve(request.moduleAliases())
        ));
    }

    @GetMapping("/menuMatrix/{roleId}/{schemeId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebListResponse<RoleMenuNode> menuMatrix(@PathVariable String roleId,
                                                    @PathVariable String schemeId) {
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

    public record UserIdsRequest(List<String> userIds) {
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

    public record GrantWildcardDataScopeRequest(
            String actionCode,
            DataScopePolicy dataScopePolicy,
            TenantScopePolicy tenantScopePolicy
    ) {
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
                .filter(menu -> menu.getMenuType() == MenuType.MODULE)
                .map(Menu::getModuleAlias)
                .filter(moduleAlias -> moduleAlias != null && !moduleAlias.isBlank())
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
        boolean granted = menu.getMenuType() == MenuType.MODULE
                && Boolean.TRUE.equals(grantedByModule.get(menu.getModuleAlias()));
        return new RoleMenuNode(
                menu,
                granted,
                menuService.children(menu.getSchemeId(), menu.getId()).stream()
                        .map(child -> roleMenuNode(child, grantedByModule))
                        .toList()
        );
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
