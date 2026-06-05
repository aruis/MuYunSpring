package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.role", title = "角色管理")
@RequestMapping("/iam.role")
public class RoleWebController extends WebSupport<RoleService> implements
        CrudWeb<Role, RoleService>,
        EnableWeb<Role, RoleService>,
        SortWeb<Role, RoleService> {
    private final RoleGrantableActionResolver grantableActionResolver;

    public RoleWebController(RoleGrantableActionResolver grantableActionResolver) {
        this.grantableActionResolver = grantableActionResolver;
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
    }

    public record RevokeActionRequest(
            String moduleAlias,
            String actionCode
    ) {
    }

    public record GrantWildcardDataScopeRequest(
            String actionCode,
            DataScopePolicy dataScopePolicy,
            TenantScopePolicy tenantScopePolicy
    ) {
    }

    public record PermissionMatrixRequest(List<String> moduleAliases) {
    }
}
