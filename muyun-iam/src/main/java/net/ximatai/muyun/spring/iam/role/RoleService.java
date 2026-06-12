package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformAliasRules;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RoleService extends TenantActiveScopedService<Role> implements
        SoftDeleteAbility<Role>,
        EnableAbility<Role>,
        SortAbility<Role>,
        ReferenceAbility<Role> {
    public static final String MODULE_ALIAS = "iam.role";
    public static final String WILDCARD_DATA_SCOPE_MODULE_ALIAS = "iam.data_scope";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final RoleGrantDao roleGrantDao;
    private final RoleActionDao roleActionDao;
    private final RoleActionGrantVerifier grantVerifier;
    private final UserAccountService userAccountService;
    private final EmployeeService employeeService;
    private final EmployeePositionService employeePositionService;
    private final EmployeeAccountService employeeAccountService;

    public RoleService(RoleDao roleDao,
                       RoleGrantDao roleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier) {
        this(roleDao, roleGrantDao, roleActionDao, activeTenantVerifier,
                RoleActionGrantVerifier.platformActionsOnly(), null, null, null);
    }

    public RoleService(RoleDao roleDao,
                       RoleGrantDao roleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier) {
        this(roleDao, roleGrantDao, roleActionDao, activeTenantVerifier,
                grantVerifier, null, null, null);
    }

    @Autowired
    public RoleService(RoleDao roleDao,
                       RoleGrantDao roleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService,
                       EmployeeAccountService employeeAccountService) {
        this(roleDao, roleGrantDao, roleActionDao, activeTenantVerifier,
                RoleActionGrantVerifier.platformActionsOnly(),
                userAccountService, employeeService, employeePositionService, employeeAccountService);
    }

    public RoleService(RoleDao roleDao,
                       RoleGrantDao roleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService) {
        this(roleDao, roleGrantDao, roleActionDao, activeTenantVerifier,
                grantVerifier, userAccountService, employeeService, employeePositionService, null);
    }

    public RoleService(RoleDao roleDao,
                       RoleGrantDao roleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService,
                       EmployeeAccountService employeeAccountService) {
        super(MODULE_ALIAS, Role.class, roleDao, activeTenantVerifier);
        this.roleGrantDao = Objects.requireNonNull(roleGrantDao, "roleGrantDao must not be null");
        this.roleActionDao = Objects.requireNonNull(roleActionDao, "roleActionDao must not be null");
        this.grantVerifier = Objects.requireNonNull(grantVerifier, "grantVerifier must not be null");
        this.userAccountService = userAccountService;
        this.employeeService = employeeService;
        this.employeePositionService = employeePositionService;
        this.employeeAccountService = employeeAccountService;
    }

    @Override
    public void normalizeBeforeMutation(Role role) {
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        if (role.getPublicRole() == null) {
            role.setPublicRole(false);
        }
        if (role.getBuiltIn() == null) {
            role.setBuiltIn(false);
        }
        if (role.getSystemManaged() == null) {
            role.setSystemManaged(false);
        }
        role.setGrantSubjectTypes(normalizeGrantSubjectTypes(role.getGrantSubjectTypes(), role.getRoleKind()));
        if (role.getRoleKind() != RoleKind.GROUP) {
            role.setMemberRoleIds(null);
        } else {
            role.setMemberRoleIds(normalizeRoleIdCsv(role.getMemberRoleIds()));
            validateGroupMembers(role.getMemberRoleIds());
        }
    }

    public String bindUser(String roleId, String userId) {
        return grantRole(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId);
    }

    public int bindUsers(String roleId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (String userId : userIds.stream().filter(Objects::nonNull).distinct().toList()) {
            if (grantRoleIfAbsent(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId).created()) {
                changed++;
            }
        }
        return changed;
    }

    public int unbindUser(String roleId, String userId) {
        return revokeRole(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId);
    }

    public int unbindUsers(String roleId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (String userId : userIds.stream().filter(Objects::nonNull).distinct().toList()) {
            changed += revokeRole(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId);
        }
        return changed;
    }

    public List<String> userIds(String roleId) {
        Role role = requireEnabledRole(roleId);
        return roleGrantDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", role.getId())
                        .eq("subjectType", RoleGrantSubjectType.USER_ACCOUNT)
                        .eq("enabled", Boolean.TRUE)), ALL)
                .stream()
                .map(RoleGrant::getSubjectId)
                .distinct()
                .toList();
    }

    public String grantRole(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        return grantRoleIfAbsent(roleId, subjectType, subjectId).grantId();
    }

    public int revokeRole(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        Role role = requireEnabledRole(roleId);
        RoleGrant grant = findRoleGrant(role.getId(), requireSubjectType(subjectType), requireSubjectId(subjectId));
        if (grant == null) {
            return 0;
        }
        return roleGrantDao.deleteById(grant.getId());
    }

    public int deleteGrant(String roleId, String grantId) {
        Role role = requireEnabledRole(roleId);
        RoleGrant grant = roleGrantDao.query(scopedChildCriteria(Criteria.of()
                        .eq("id", Preconditions.requireText(grantId, "grantId"))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
        if (grant == null || !SortAbility.sameValue(role.getId(), grant.getRoleId())) {
            throw new PlatformException("role grant does not belong to role: " + grantId);
        }
        return roleGrantDao.deleteById(grant.getId());
    }

    public List<RoleGrant> roleGrants(String roleId) {
        Role role = requireEnabledRole(roleId);
        return roleGrantDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", role.getId())
                        .eq("enabled", Boolean.TRUE)), ALL);
    }

    public List<RoleGrant> subjectRoleGrants(RoleGrantSubjectType subjectType, String subjectId) {
        return roleGrantDao.query(scopedChildCriteria(Criteria.of()
                        .eq("subjectType", requireSubjectType(subjectType))
                        .eq("subjectId", requireSubjectId(subjectId))
                        .eq("enabled", Boolean.TRUE)), ALL);
    }

    public int grantAction(String roleId, String moduleAlias, String actionCode) {
        return grantAction(roleId, moduleAlias, actionCode, null,
                TenantScopePolicy.CURRENT_TENANT, null, null, null);
    }

    public int grantAction(String roleId,
                           String moduleAlias,
                           String actionCode,
                           DataScopePolicy dataScopePolicy,
                           TenantScopePolicy tenantScopePolicy) {
        return grantAction(roleId, moduleAlias, actionCode, dataScopePolicy, tenantScopePolicy, null, null, null);
    }

    public int grantAction(String roleId,
                           String moduleAlias,
                           String actionCode,
                           DataScopePolicy dataScopePolicy,
                           TenantScopePolicy tenantScopePolicy,
                           String scopeCondition,
                           String referenceFieldId,
                           String referenceActionCode) {
        Role role = requireStandardRoleForActionGrant(roleId);

        String validModuleAlias = requireModuleAlias(moduleAlias);
        String requestedActionCode = requireActionCode(actionCode);
        String validActionCode = resolveGrantablePermissionActionCode(validModuleAlias, requestedActionCode);
        DataScopePolicy validDataScopePolicy = normalizeDataScopePolicy(dataScopePolicy, scopeCondition, referenceFieldId);
        validateRoleActionDataScopePolicy(role, validDataScopePolicy);

        RoleAction roleAction = findRoleAction(roleId, validModuleAlias, validActionCode);
        boolean exists = roleAction != null;
        if (!exists) {
            roleAction = new RoleAction();
            roleAction.setRoleId(roleId);
            roleAction.setModuleAlias(validModuleAlias);
            roleAction.setActionCode(validActionCode);
        }
        roleAction.setDataScopePolicy(validDataScopePolicy);
        roleAction.setTenantScopePolicy(normalizeTenantScopePolicy(tenantScopePolicy));
        roleAction.setScopeCondition(normalizeBlank(scopeCondition));
        roleAction.setReferenceFieldId(normalizeBlank(referenceFieldId));
        roleAction.setReferenceActionCode(normalizeBlank(referenceActionCode));
        roleAction.setEnabled(true);

        if (exists) {
            prepareChildUpdate(roleAction);
            return roleActionDao.updateById(roleAction);
        }
        prepareChildInsert(roleAction);
        roleActionDao.insert(roleAction);
        return 1;
    }

    public int grantActions(String roleId, List<ActionGrantCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (ActionGrantCommand command : commands.stream().filter(Objects::nonNull).toList()) {
            changed += grantAction(
                    roleId,
                    command.moduleAlias(),
                    command.actionCode(),
                    command.dataScopePolicy(),
                    command.tenantScopePolicy(),
                    command.scopeCondition(),
                    command.referenceFieldId(),
                    command.referenceActionCode()
            );
        }
        return changed;
    }

    public int grantWildcardDataScopeAction(String roleId,
                                            String actionCode,
                                            DataScopePolicy dataScopePolicy,
                                            TenantScopePolicy tenantScopePolicy) {
        Role role = requireEnabledRole(roleId);
        if (role.getRoleKind() != RoleKind.WILDCARD_DATA_SCOPE) {
            throw new PlatformException("role is not wildcard data scope role: " + roleId);
        }
        String requestedActionCode = requireActionCode(actionCode);
        PlatformAction platformAction = PlatformAction.fromCode(requestedActionCode).orElse(null);
        if (platformAction != null && !platformAction.dataAuth()) {
            throw new PlatformException("wildcard data scope action must support data auth: " + actionCode);
        }
        DataScopePolicy validPolicy = normalizeWildcardDataScopePolicy(dataScopePolicy);
        String validActionCode = permissionActionCode(requestedActionCode);
        RoleAction roleAction = findRoleAction(role.getId(), WILDCARD_DATA_SCOPE_MODULE_ALIAS, validActionCode);
        boolean exists = roleAction != null;
        if (!exists) {
            roleAction = new RoleAction();
            roleAction.setRoleId(role.getId());
            roleAction.setModuleAlias(WILDCARD_DATA_SCOPE_MODULE_ALIAS);
            roleAction.setActionCode(validActionCode);
        }
        roleAction.setDataScopePolicy(validPolicy);
        roleAction.setTenantScopePolicy(normalizeTenantScopePolicy(tenantScopePolicy));
        roleAction.setScopeCondition(null);
        roleAction.setReferenceFieldId(null);
        roleAction.setReferenceActionCode(null);
        roleAction.setEnabled(true);
        if (exists) {
            prepareChildUpdate(roleAction);
            return roleActionDao.updateById(roleAction);
        }
        prepareChildInsert(roleAction);
        roleActionDao.insert(roleAction);
        return 1;
    }

    public int revokeAction(String roleId, String moduleAlias, String actionCode) {
        String validModuleAlias = requireModuleAlias(moduleAlias);
        String validActionCode = resolveGrantablePermissionActionCode(validModuleAlias, actionCode);
        RoleAction roleAction = findRoleAction(roleId, validModuleAlias, validActionCode);
        if (roleAction == null) {
            return 0;
        }
        roleAction.setEnabled(false);
        prepareChildUpdate(roleAction);
        return roleActionDao.updateById(roleAction);
    }

    public int revokeActions(String roleId, List<ActionRevokeCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return 0;
        }
        int changed = 0;
        for (ActionRevokeCommand command : commands.stream().filter(Objects::nonNull).toList()) {
            changed += revokeAction(roleId, command.moduleAlias(), command.actionCode());
        }
        return changed;
    }

    public boolean hasActionPermission(String userId, String moduleAlias, String actionCode) {
        return !effectiveActionGrants(userId, moduleAlias, actionCode).isEmpty();
    }

    public List<RoleAction> effectiveActionGrants(String userId, String moduleAlias, String actionCode) {
        return effectiveActionGrantsWithContext(userId, moduleAlias, actionCode).stream()
                .map(EffectiveRoleActionGrant::actionGrant)
                .distinct()
                .toList();
    }

    public List<EffectiveRoleActionGrant> effectiveActionGrantsWithContext(String userId,
                                                                           String moduleAlias,
                                                                           String actionCode) {
        List<EffectiveRoleGrant> roleGrants = effectiveRoleGrants(userId);
        if (roleGrants.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> roleIds = new LinkedHashSet<>();
        roleGrants.stream()
                .map(EffectiveRoleGrant::roleId)
                .forEach(roleIds::add);
        String permissionActionCode = permissionActionCode(actionCode);
        List<RoleAction> actionGrants = roleActionDao.query(Criteria.of()
                        .in("roleId", List.copyOf(roleIds))
                        .eq("moduleAlias", requireModuleAlias(moduleAlias))
                        .eq("actionCode", permissionActionCode)
                        .eq("enabled", Boolean.TRUE),
                ALL);
        if (actionGrants.isEmpty()) {
            return List.of();
        }
        Map<String, List<EffectiveRoleGrant>> grantsByRoleId = roleGrantsByRoleId(roleGrants);
        java.util.ArrayList<EffectiveRoleActionGrant> effective = new java.util.ArrayList<>();
        for (RoleAction actionGrant : actionGrants) {
            List<EffectiveRoleGrant> matchedRoleGrants = grantsByRoleId.get(actionGrant.getRoleId());
            if (matchedRoleGrants == null || matchedRoleGrants.isEmpty()) {
                continue;
            }
            matchedRoleGrants.forEach(roleGrant -> effective.add(new EffectiveRoleActionGrant(actionGrant, roleGrant)));
        }
        return List.copyOf(effective);
    }

    public RoleAction effectiveWildcardDataScopeGrant(String userId, String actionCode) {
        Set<String> roleIds = effectiveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return null;
        }
        List<Role> wildcardRoles = roleIds.stream()
                .map(this::select)
                .filter(Objects::nonNull)
                .filter(role -> role.getRoleKind() == RoleKind.WILDCARD_DATA_SCOPE)
                .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
                .toList();
        if (wildcardRoles.isEmpty()) {
            return null;
        }
        if (wildcardRoles.size() > 1) {
            throw new PlatformException("user has more than one wildcard data scope role: " + userId);
        }
        String validActionCode = permissionActionCode(actionCode);
        return roleActionDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", wildcardRoles.get(0).getId())
                        .eq("moduleAlias", WILDCARD_DATA_SCOPE_MODULE_ALIAS)
                        .eq("actionCode", validActionCode)
                        .eq("enabled", Boolean.TRUE)),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    public Set<String> effectiveRoleIds(String userId) {
        LinkedHashSet<String> effective = new LinkedHashSet<>();
        effectiveRoleGrants(userId).stream()
                .map(EffectiveRoleGrant::roleId)
                .forEach(effective::add);
        return effective;
    }

    public List<EffectiveRoleGrant> effectiveRoleGrants(String userId) {
        String validUserId = Preconditions.requireText(userId, "userId");
        java.util.ArrayList<EffectiveRoleGrant> effective = new java.util.ArrayList<>();
        appendEffectiveRoleGrants(effective,
                subjectRoleGrants(RoleGrantSubjectType.USER_ACCOUNT, validUserId),
                null, null, null);

        String employeeId = employeeAccountService == null ? null : employeeAccountService.employeeIdOfUser(validUserId);
        if (employeeId == null || employeeId.isBlank() || employeeService == null) {
            return List.copyOf(effective);
        }
        Employee employee = employeeService.select(employeeId);
        if (employee == null || !Boolean.TRUE.equals(employee.getEnabled())) {
            return List.copyOf(effective);
        }
        appendEffectiveRoleGrants(effective,
                subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE, employee.getId()),
                employee.getOrganizationId(), employee.getDepartmentId(), null);

        if (employeePositionService != null) {
            for (EmployeePosition position : employeePositionService.positions(employee.getId())) {
                if (position == null || !Boolean.TRUE.equals(position.getEnabled())) {
                    continue;
                }
                appendEffectiveRoleGrants(effective,
                        subjectRoleGrants(RoleGrantSubjectType.EMPLOYEE_POSITION, position.getId()),
                        position.getOrganizationId(), position.getDepartmentId(), position.getId());
            }
        }
        return List.copyOf(effective);
    }

    public List<RoleAction> alignedActions(String roleId, List<String> moduleAliases, List<String> actionCodes) {
        Preconditions.requireText(roleId, "roleId");
        if (moduleAliases == null || moduleAliases.isEmpty() || actionCodes == null || actionCodes.isEmpty()) {
            return List.of();
        }
        List<RoleAction> configured = roleActionDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", roleId)
                        .in("moduleAlias", moduleAliases)
                        .in("actionCode", actionCodes)),
                ALL,
                Sort.asc("moduleAlias"),
                Sort.asc("actionCode"));
        return moduleAliases.stream()
                .flatMap(moduleAlias -> actionCodes.stream().map(actionCode ->
                        configured.stream()
                                .filter(item -> moduleAlias.equals(item.getModuleAlias())
                                        && actionCode.equals(item.getActionCode()))
                                .findFirst()
                                .orElseGet(() -> disabledActionView(roleId, moduleAlias, actionCode))))
                .toList();
    }

    public RolePermissionMatrix permissionMatrix(String roleId, List<GrantableAction> actions) {
        Role role = requireConfigurableRole(roleId);
        String validRoleId = role.getId();
        if (actions == null || actions.isEmpty()) {
            return new RolePermissionMatrix(validRoleId, List.of());
        }
        LinkedHashMap<String, GrantableAction> actionByKey = new LinkedHashMap<>();
        for (GrantableAction action : actions) {
            if (action == null) {
                continue;
            }
            String key = actionKey(action.moduleAlias(), action.permissionActionCode());
            GrantableAction existing = actionByKey.get(key);
            if (existing == null || action.actionCode().equals(action.permissionActionCode())) {
                actionByKey.put(key, action);
            }
        }
        if (actionByKey.isEmpty()) {
            return new RolePermissionMatrix(validRoleId, List.of());
        }

        List<String> moduleAliases = actionByKey.values().stream()
                .map(GrantableAction::moduleAlias)
                .distinct()
                .toList();
        List<String> actionCodes = actionByKey.values().stream()
                .map(GrantableAction::permissionActionCode)
                .distinct()
                .toList();
        Map<String, RoleAction> configuredByKey = new LinkedHashMap<>();
        roleActionDao.query(scopedChildCriteria(Criteria.of()
                                .eq("roleId", validRoleId)
                                .in("moduleAlias", moduleAliases)
                                .in("actionCode", actionCodes)),
                        ALL,
                        Sort.asc("moduleAlias"),
                        Sort.asc("actionCode"))
                .forEach(action -> configuredByKey.put(actionKey(action.getModuleAlias(), action.getActionCode()), action));

        LinkedHashMap<String, List<RolePermissionAction>> actionsByModule = new LinkedHashMap<>();
        actionByKey.values().forEach(action -> actionsByModule
                .computeIfAbsent(action.moduleAlias(), ignored -> new java.util.ArrayList<>())
                .add(RolePermissionAction.of(action, configuredByKey.get(actionKey(action.moduleAlias(), action.permissionActionCode())))));
        List<RolePermissionMatrix.Module> modules = actionsByModule.entrySet().stream()
                .map(entry -> new RolePermissionMatrix.Module(entry.getKey(), entry.getValue()))
                .toList();
        return new RolePermissionMatrix(validRoleId, modules);
    }

    @Override
    public void afterDelete(String id, Role role, int deleted) {
        roleGrantDao.query(scopedChildCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(binding -> roleGrantDao.deleteById(binding.getId()));
        roleActionDao.query(scopedChildCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(action -> roleActionDao.deleteById(action.getId()));
        removeRoleFromGroups(id);
    }

    private Role requireEnabledRole(String roleId) {
        Role role = requireEnabled(Preconditions.requireText(roleId, "roleId"), "role is not active: " + roleId);
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        role.setGrantSubjectTypes(normalizeGrantSubjectTypes(role.getGrantSubjectTypes(), role.getRoleKind()));
        return role;
    }

    public boolean canGrantTo(Role role, RoleGrantSubjectType subjectType) {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(subjectType, "subjectType must not be null");
        return parseGrantSubjectTypes(normalizeGrantSubjectTypes(role.getGrantSubjectTypes(), role.getRoleKind()))
                .contains(subjectType);
    }

    private Role requireConfigurableRole(String roleId) {
        Role role = requireEnabledRole(roleId);
        if (role.getRoleKind() == RoleKind.GROUP) {
            throw new PlatformException("role group cannot be granted directly: " + roleId);
        }
        return role;
    }

    private Role requireStandardRoleForActionGrant(String roleId) {
        Role role = requireConfigurableRole(roleId);
        if (role.getRoleKind() == RoleKind.WILDCARD_DATA_SCOPE) {
            throw new PlatformException("wildcard data scope role cannot be granted business action directly: " + roleId);
        }
        return role;
    }

    private void validateGroupMembers(String memberRoleIds) {
        for (String memberRoleId : parseRoleIds(memberRoleIds)) {
            Role member = select(memberRoleId);
            if (member == null) {
                throw new PlatformException("role group contains missing role: " + memberRoleId);
            }
            if (member.getRoleKind() != RoleKind.STANDARD) {
                throw new PlatformException("role group can only contain standard roles: " + memberRoleId);
            }
            if (!Boolean.TRUE.equals(member.getEnabled())) {
                throw new PlatformException("role group contains inactive role: " + memberRoleId);
            }
        }
    }

    private Set<String> expandGroupRoleIds(String memberRoleIds) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String memberRoleId : parseRoleIds(memberRoleIds)) {
            Role member = select(memberRoleId);
            if (member != null
                    && member.getRoleKind() == RoleKind.STANDARD
                    && Boolean.TRUE.equals(member.getEnabled())) {
                expanded.add(member.getId());
            }
        }
        return expanded;
    }

    private void appendEffectiveRoleGrants(java.util.List<EffectiveRoleGrant> effective,
                                           List<RoleGrant> grants,
                                           String organizationId,
                                           String departmentId,
                                           String employeePositionId) {
        if (grants == null || grants.isEmpty()) {
            return;
        }
        for (RoleGrant grant : grants) {
            appendEffectiveRoleGrant(effective, grant, organizationId, departmentId, employeePositionId);
        }
    }

    private void appendEffectiveRoleGrant(java.util.List<EffectiveRoleGrant> effective,
                                          RoleGrant grant,
                                          String organizationId,
                                          String departmentId,
                                          String employeePositionId) {
        if (grant == null || !Boolean.TRUE.equals(grant.getEnabled())) {
            return;
        }
        Role role = select(grant.getRoleId());
        if (role == null || !Boolean.TRUE.equals(role.getEnabled())) {
            return;
        }
        effective.add(new EffectiveRoleGrant(
                role.getId(),
                grant.getSubjectType(),
                grant.getSubjectId(),
                organizationId,
                departmentId,
                employeePositionId));
        if (role.getRoleKind() == RoleKind.GROUP) {
            for (String memberRoleId : expandGroupRoleIds(role.getMemberRoleIds())) {
                effective.add(new EffectiveRoleGrant(
                        memberRoleId,
                        grant.getSubjectType(),
                        grant.getSubjectId(),
                        organizationId,
                        departmentId,
                        employeePositionId));
            }
        }
    }

    private Map<String, List<EffectiveRoleGrant>> roleGrantsByRoleId(List<EffectiveRoleGrant> roleGrants) {
        LinkedHashMap<String, List<EffectiveRoleGrant>> byRoleId = new LinkedHashMap<>();
        roleGrants.stream()
                .filter(Objects::nonNull)
                .filter(grant -> grant.roleId() != null)
                .forEach(grant -> byRoleId
                        .computeIfAbsent(grant.roleId(), ignored -> new java.util.ArrayList<>())
                        .add(grant));
        return byRoleId;
    }

    private GrantResult grantRoleIfAbsent(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        Role role = requireEnabledRole(roleId);
        RoleGrantSubjectType validSubjectType = requireSubjectType(subjectType);
        String validSubjectId = requireSubjectId(subjectId);
        ensureRoleCanGrantTo(role, validSubjectType);
        ensureWildcardDataScopeRoleGrantsToAccount(role, validSubjectType);
        validateGrantSubject(validSubjectType, validSubjectId);
        ensureDataScopeRoleBindingValid(role.getId(), validSubjectType, validSubjectId);
        RoleGrant existing = findRoleGrant(role.getId(), validSubjectType, validSubjectId);
        if (existing != null) {
            return new GrantResult(existing.getId(), false);
        }

        RoleGrant grant = new RoleGrant();
        grant.setRoleId(role.getId());
        grant.setSubjectType(validSubjectType);
        grant.setSubjectId(validSubjectId);
        grant.setEnabled(true);
        prepareChildInsert(grant);
        return new GrantResult(roleGrantDao.insert(grant), true);
    }

    private void validateGrantSubject(RoleGrantSubjectType subjectType, String subjectId) {
        if (subjectType == RoleGrantSubjectType.USER_ACCOUNT && userAccountService != null) {
            userAccountService.requireEnabled(subjectId, "user account is not active: " + subjectId);
        } else if (subjectType == RoleGrantSubjectType.EMPLOYEE && employeeService != null) {
            employeeService.requireEnabled(subjectId, "employee is not active: " + subjectId);
        } else if (subjectType == RoleGrantSubjectType.EMPLOYEE_POSITION && employeePositionService != null) {
            employeePositionService.requireEnabled(subjectId, "employee position is not active: " + subjectId);
        }
    }

    private void ensureDataScopeRoleBindingValid(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        if (subjectType != RoleGrantSubjectType.USER_ACCOUNT) {
            return;
        }
        LinkedHashSet<String> roleIds = new LinkedHashSet<>(effectiveRoleIds(subjectId));
        roleIds.add(roleId);
        long wildcardDataScopeRoleCount = roleIds.stream()
                .map(this::select)
                .filter(Objects::nonNull)
                .filter(role -> role.getRoleKind() == RoleKind.WILDCARD_DATA_SCOPE)
                .count();
        if (wildcardDataScopeRoleCount > 1) {
            throw new PlatformException("user can bind at most one wildcard data scope role");
        }
    }

    private void ensureRoleCanGrantTo(Role role, RoleGrantSubjectType subjectType) {
        if (!canGrantTo(role, subjectType)) {
            throw new PlatformException("role cannot be granted to " + subjectType.getCode() + ": " + role.getId());
        }
    }

    private void ensureWildcardDataScopeRoleGrantsToAccount(Role role, RoleGrantSubjectType subjectType) {
        if (role.getRoleKind() == RoleKind.WILDCARD_DATA_SCOPE
                && subjectType != RoleGrantSubjectType.USER_ACCOUNT) {
            throw new PlatformException("wildcard data scope role can only be granted to user account: " + role.getId());
        }
    }

    private RoleGrant findRoleGrant(String roleId, RoleGrantSubjectType subjectType, String subjectId) {
        return roleGrantDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", Preconditions.requireText(roleId, "roleId"))
                        .eq("subjectType", requireSubjectType(subjectType))
                        .eq("subjectId", requireSubjectId(subjectId))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private RoleAction findRoleAction(String roleId, String moduleAlias, String actionCode) {
        return roleActionDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", Preconditions.requireText(roleId, "roleId"))
                        .eq("moduleAlias", requireModuleAlias(moduleAlias))
                        .eq("actionCode", requireActionCode(actionCode))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private void removeRoleFromGroups(String roleId) {
        List<Role> groups = list(Criteria.of().eq("roleKind", RoleKind.GROUP), ALL);
        for (Role group : groups) {
            Set<String> memberRoleIds = parseRoleIds(group.getMemberRoleIds());
            if (!memberRoleIds.remove(roleId)) {
                continue;
            }
            group.setMemberRoleIds(String.join(",", memberRoleIds));
            update(group);
        }
    }

    private RoleAction disabledActionView(String roleId, String moduleAlias, String actionCode) {
        RoleAction action = new RoleAction();
        action.setRoleId(roleId);
        action.setModuleAlias(moduleAlias);
        action.setActionCode(actionCode);
        action.setDataScopePolicy(DataScopePolicy.NONE);
        action.setEnabled(false);
        return action;
    }

    private String actionKey(String moduleAlias, String actionCode) {
        return moduleAlias + ":" + actionCode;
    }

    private DataScopePolicy normalizeDataScopePolicy(DataScopePolicy dataScopePolicy,
                                                     String scopeCondition,
                                                     String referenceFieldId) {
        DataScopePolicy policy = dataScopePolicy == null ? DataScopePolicy.NONE : dataScopePolicy;
        if (policy == DataScopePolicy.CUSTOM) {
            throw new PlatformException("custom data scope policy is not supported yet");
        }
        if (policy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            Preconditions.requireText(referenceFieldId, "referenceFieldId");
        }
        return policy;
    }

    private DataScopePolicy normalizeWildcardDataScopePolicy(DataScopePolicy dataScopePolicy) {
        DataScopePolicy policy = dataScopePolicy == null ? DataScopePolicy.NONE : dataScopePolicy;
        if (policy == DataScopePolicy.WILDCARD
                || policy == DataScopePolicy.CUSTOM
                || policy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            throw new PlatformException("wildcard data scope role only supports standard data scope policy");
        }
        return policy;
    }

    private void validateRoleActionDataScopePolicy(Role role, DataScopePolicy policy) {
        if (role.getRoleKind() == RoleKind.WILDCARD_DATA_SCOPE
                && (policy == DataScopePolicy.WILDCARD
                || policy == DataScopePolicy.CUSTOM
                || policy == DataScopePolicy.REFERENCE_DEPENDENCY)) {
            throw new PlatformException("wildcard data scope role only supports standard data scope policy");
        }
    }

    private TenantScopePolicy normalizeTenantScopePolicy(TenantScopePolicy tenantScopePolicy) {
        return tenantScopePolicy == null ? TenantScopePolicy.CURRENT_TENANT : tenantScopePolicy;
    }

    private String normalizeRoleIdCsv(String value) {
        Set<String> ids = parseRoleIds(value);
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    private String normalizeGrantSubjectTypes(String value, RoleKind roleKind) {
        Set<RoleGrantSubjectType> types = parseGrantSubjectTypes(value);
        if (types.isEmpty() || shouldUseKindDefaultGrantSubjectTypes(types, roleKind)) {
            types.clear();
            types.add(defaultGrantSubjectType(roleKind));
        }
        return types.stream()
                .map(RoleGrantSubjectType::getCode)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private boolean shouldUseKindDefaultGrantSubjectTypes(Set<RoleGrantSubjectType> types, RoleKind roleKind) {
        return roleKind == RoleKind.POSITION_TEMPLATE
                && types.size() == 1
                && types.contains(RoleGrantSubjectType.USER_ACCOUNT);
    }

    private RoleGrantSubjectType defaultGrantSubjectType(RoleKind roleKind) {
        return roleKind == RoleKind.POSITION_TEMPLATE
                ? RoleGrantSubjectType.EMPLOYEE_POSITION
                : RoleGrantSubjectType.USER_ACCOUNT;
    }

    private Set<RoleGrantSubjectType> parseGrantSubjectTypes(String value) {
        LinkedHashSet<RoleGrantSubjectType> types = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return types;
        }
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(RoleGrantSubjectType::fromCode)
                .forEach(types::add);
        return types;
    }

    private Set<String> parseRoleIds(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .forEach(ids::add);
        return ids;
    }

    private String requireModuleAlias(String moduleAlias) {
        return PlatformAliasRules.requireModuleAlias(moduleAlias);
    }

    private String requireActionCode(String actionCode) {
        return Preconditions.requireText(actionCode, "actionCode");
    }

    private RoleGrantSubjectType requireSubjectType(RoleGrantSubjectType subjectType) {
        return Objects.requireNonNull(subjectType, "subjectType must not be null");
    }

    private String requireSubjectId(String subjectId) {
        return Preconditions.requireText(subjectId, "subjectId");
    }

    private String permissionActionCode(String actionCode) {
        return PlatformAction.permissionActionCodeOf(requireActionCode(actionCode));
    }

    private String resolveGrantablePermissionActionCode(String moduleAlias, String actionCode) {
        return requireActionCode(grantVerifier.resolveGrantablePermissionActionCode(moduleAlias, requireActionCode(actionCode)));
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void prepareChildInsert(net.ximatai.muyun.spring.common.model.contract.EntityContract entity) {
        requireActiveTenantMutationContext();
        EntityLifecycle.prepareInsert(entity, Instant.now());
    }

    private void prepareChildUpdate(net.ximatai.muyun.spring.common.model.contract.EntityContract entity) {
        EntityLifecycle.prepareUpdate(entity, Instant.now());
    }

    private Criteria scopedChildCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        TenantContext.currentTenantId().ifPresent(tenantId -> scoped.eq("tenantId", tenantId));
        return scoped;
    }

    public record ActionGrantCommand(
            String moduleAlias,
            String actionCode,
            DataScopePolicy dataScopePolicy,
            TenantScopePolicy tenantScopePolicy,
            String scopeCondition,
            String referenceFieldId,
            String referenceActionCode
    ) {
    }

    public record ActionRevokeCommand(
            String moduleAlias,
            String actionCode
    ) {
    }

    private record GrantResult(String grantId, boolean created) {
    }
}
