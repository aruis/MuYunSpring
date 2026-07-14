package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeExecutor;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeResolver;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrant;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrant;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleOwnerScopeType;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuService;
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
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.role", title = "角色管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 70)
@RequestMapping("/iam.role")
public class RoleWebController extends WebSupport<RoleService> implements
        CrudWeb<Role, RoleService>,
        EnableWeb<Role, RoleService>,
        SortWeb<Role, RoleService>,
        MutationTenantScopeResolver<Role>,
        StaticModuleUiContributor {
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

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(RoleService.MODULE_ALIAS)
                .listView(list -> list
                        .title("角色列表")
                        .field("title", field -> field.label("角色名称").width("180px"))
                        .field("assignmentType", field -> field.label("授权层级").uiType("select").width("110px"))
                        .field("roleKind", field -> field.label("角色类型").uiType("select").width("130px"))
                        .field("sharePolicy", field -> field.label("公开策略").uiType("select").width("120px"))
                        .field("systemManaged", field -> field.label("系统托管").width("100px").align("center"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center")))
                .formView(form -> form
                        .title("角色档案")
                        .field("title", field -> field.label("角色名称").required())
                        .field("assignmentType", field -> field.label("授权层级").required().uiType("select"))
                        .field("roleKind", field -> field.label("角色类型").required().uiType("select"))
                        .field("memberRoleIds", field -> field.label("成员角色"))
                        .field("ownerScopeType", field -> field.label("归属范围").required().readOnly().uiType("select"))
                        .field("ownerScopeId", field -> field.label("归属对象").readOnly())
                        .field("sharePolicy", field -> field.label("公开策略").required().uiType("select"))
                        .field("description", field -> field.label("说明"))
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus"))
                        .field("sortOrder", field -> field.label("排序号")))
                .build();
    }

    @Override
    public Optional<String> tenantIdForCreate(Role record) {
        return tenantIdForRole(record);
    }

    @Override
    public Optional<String> tenantIdForUpdate(String id, Role record) {
        Role existing = service().select(id);
        if (existing != null) {
            return tenantIdForRole(existing);
        }
        return tenantIdForCreate(record);
    }

    @Override
    public Optional<String> tenantIdForExistingRecord(String id) {
        return tenantIdForRole(service().select(id));
    }

    @GetMapping("/{roleId}/account-grants")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public List<AccountRoleGrant> accountRoleGrants(@PathVariable String roleId) {
        return roleRecordScope(roleId, () -> service().accountRoleGrants(roleId));
    }

    @PostMapping("/{roleId}/account-grants")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public String grantAccountRole(@PathVariable String roleId,
                                   @RequestBody AccountRoleGrantRequest request) {
        return roleRecordScope(roleId, () -> service().grantAccountRole(
                roleId,
                request.userId(),
                request.managementScopeType(),
                request.managementScopeId()));
    }

    @PostMapping("/{roleId}/account-grants/{grantId}/delete")
    @CustomActionEndpoint(value = "accountRoleGrants", title = "账号角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int deleteAccountRoleGrant(@PathVariable String roleId,
                                                   @PathVariable String grantId) {
        return roleRecordScope(roleId,
                () -> service().deleteAccountRoleGrant(roleId, grantId));
    }

    @GetMapping("/{roleId}/employment-grants")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public List<EmploymentRoleGrant> employmentRoleGrants(@PathVariable String roleId) {
        return roleRecordScope(roleId, () -> service().employmentRoleGrants(roleId));
    }

    @PostMapping("/{roleId}/employment-grants")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public String grantEmploymentRole(@PathVariable String roleId,
                                      @RequestBody EmploymentRoleGrantRequest request) {
        return roleRecordScope(roleId, () -> service().grantEmploymentRole(roleId, request.employeePositionId()));
    }

    @PostMapping("/{roleId}/employment-grants/{grantId}/delete")
    @CustomActionEndpoint(value = "employmentRoleGrants", title = "任职角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int deleteEmploymentRoleGrant(@PathVariable String roleId,
                                                      @PathVariable String grantId) {
        return roleRecordScope(roleId,
                () -> service().deleteEmploymentRoleGrant(roleId, grantId));
    }

    @PostMapping("/grant/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int grantAction(@PathVariable String roleId,
                                        @RequestBody GrantActionRequest request) {
        return roleRecordScope(roleId, () -> service().grantAction(
                roleId,
                request.moduleAlias(),
                request.actionCode(),
                request.dataScopePolicy(),
                request.tenantScopePolicy(),
                request.scopeCondition(),
                request.referenceFieldId(),
                request.referenceActionCode()
        ));
    }

    @PostMapping("/grant/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int grantActions(@PathVariable String roleId,
                                         @RequestBody GrantActionsRequest request) {
        return roleRecordScope(roleId, () -> service().grantActions(
                roleId,
                request.actions().stream()
                        .map(GrantActionRequest::toCommand)
                        .toList()
        ));
    }

    @PostMapping("/revoke/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int revokeAction(@PathVariable String roleId,
                                         @RequestBody RevokeActionRequest request) {
        return roleRecordScope(roleId, () -> service().revokeAction(
                roleId, request.moduleAlias(), request.actionCode()));
    }

    @PostMapping("/revoke/{roleId}/batch")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public int revokeActions(@PathVariable String roleId,
                                          @RequestBody RevokeActionsRequest request) {
        return roleRecordScope(roleId, () -> service().revokeActions(
                roleId,
                request.actions().stream()
                        .map(RevokeActionRequest::toCommand)
                        .toList()
        ));
    }

    @PostMapping("/permissionMatrix/{roleId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public RolePermissionMatrix permissionMatrix(@PathVariable String roleId,
                                                 @RequestBody PermissionMatrixRequest request) {
        return roleRecordScope(roleId, () -> service().permissionMatrix(
                roleId,
                grantableActionResolver.resolve(request.moduleAliases())
        ));
    }

    @GetMapping("/menuMatrix/{roleId}/{schemeId}")
    @CustomActionEndpoint(value = "rolePermissions", title = "角色授权",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "roleId")
    public WebListResponse<RoleMenuNode> menuMatrix(@PathVariable String roleId,
                                                    @PathVariable String schemeId) {
        return roleRecordScope(roleId, () -> {
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

    private Optional<String> tenantIdForRole(Role role) {
        if (role == null || role.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM) {
            return Optional.empty();
        }
        return Optional.of(Preconditions.requireText(role.getTenantId(),
                "tenantId is required for tenant or organization role mutation"));
    }

    private <T> T roleRecordScope(String roleId, Supplier<T> action) {
        return MutationTenantScopeExecutor.forExistingRecord(this, roleId, () -> webScope(action));
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
