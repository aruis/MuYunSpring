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

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final RoleUserDao roleUserDao;
    private final RoleActionDao roleActionDao;
    private final RoleActionGrantVerifier grantVerifier;

    public RoleService(RoleDao roleDao,
                       RoleUserDao roleUserDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier) {
        this(roleDao, roleUserDao, roleActionDao, activeTenantVerifier,
                RoleActionGrantVerifier.platformActionsOnly());
    }

    public RoleService(RoleDao roleDao,
                       RoleUserDao roleUserDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier) {
        super(MODULE_ALIAS, Role.class, roleDao, activeTenantVerifier);
        this.roleUserDao = Objects.requireNonNull(roleUserDao, "roleUserDao must not be null");
        this.roleActionDao = Objects.requireNonNull(roleActionDao, "roleActionDao must not be null");
        this.grantVerifier = Objects.requireNonNull(grantVerifier, "grantVerifier must not be null");
    }

    @Override
    public void normalizeBeforeMutation(Role role) {
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        if (role.getPublicRole() == null) {
            role.setPublicRole(false);
        }
        if (role.getRoleKind() != RoleKind.GROUP) {
            role.setMemberRoleIds(null);
        } else {
            role.setMemberRoleIds(normalizeRoleIdCsv(role.getMemberRoleIds()));
            validateGroupMembers(role.getMemberRoleIds());
        }
    }

    public String bindUser(String roleId, String userId) {
        Role role = requireEnabledRole(roleId);
        String validUserId = Preconditions.requireText(userId, "userId");
        ensureDataScopeRoleBindingValid(role.getId(), validUserId);
        RoleUser existing = findRoleUser(role.getId(), validUserId);
        if (existing != null) {
            return existing.getId();
        }

        RoleUser binding = new RoleUser();
        binding.setRoleId(role.getId());
        binding.setUserId(validUserId);
        prepareChildInsert(binding);
        return roleUserDao.insert(binding);
    }

    public int bindUsers(String roleId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        Role role = requireEnabledRole(roleId);
        int changed = 0;
        for (String userId : userIds.stream().filter(Objects::nonNull).distinct().toList()) {
            String validUserId = Preconditions.requireText(userId, "userId");
            ensureDataScopeRoleBindingValid(role.getId(), validUserId);
            if (findRoleUser(role.getId(), validUserId) != null) {
                continue;
            }
            RoleUser binding = new RoleUser();
            binding.setRoleId(role.getId());
            binding.setUserId(validUserId);
            prepareChildInsert(binding);
            roleUserDao.insert(binding);
            changed++;
        }
        return changed;
    }

    public int unbindUser(String roleId, String userId) {
        Role role = requireEnabledRole(roleId);
        RoleUser binding = findRoleUser(role.getId(), userId);
        if (binding == null) {
            return 0;
        }
        return roleUserDao.deleteById(binding.getId());
    }

    public int unbindUsers(String roleId, List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        Role role = requireEnabledRole(roleId);
        int changed = 0;
        for (String userId : userIds.stream().filter(Objects::nonNull).distinct().toList()) {
            RoleUser binding = findRoleUser(role.getId(), userId);
            if (binding != null) {
                changed += roleUserDao.deleteById(binding.getId());
            }
        }
        return changed;
    }

    public List<String> userIds(String roleId) {
        Role role = requireEnabledRole(roleId);
        return roleUserDao.query(scopedChildCriteria(Criteria.of().eq("roleId", role.getId())), ALL)
                .stream()
                .map(RoleUser::getUserId)
                .distinct()
                .toList();
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
        requireConfigurableRole(roleId);

        String validModuleAlias = requireModuleAlias(moduleAlias);
        String requestedActionCode = requireActionCode(actionCode);
        grantVerifier.requireGrantable(validModuleAlias, requestedActionCode);
        String validActionCode = permissionActionCode(requestedActionCode);

        RoleAction roleAction = findRoleAction(roleId, validModuleAlias, validActionCode);
        boolean exists = roleAction != null;
        if (!exists) {
            roleAction = new RoleAction();
            roleAction.setRoleId(roleId);
            roleAction.setModuleAlias(validModuleAlias);
            roleAction.setActionCode(validActionCode);
        }
        roleAction.setDataScopePolicy(normalizeDataScopePolicy(dataScopePolicy, scopeCondition, referenceFieldId));
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

    public int revokeAction(String roleId, String moduleAlias, String actionCode) {
        RoleAction roleAction = findRoleAction(roleId, moduleAlias, permissionActionCode(actionCode));
        if (roleAction == null) {
            return 0;
        }
        roleAction.setEnabled(false);
        prepareChildUpdate(roleAction);
        return roleActionDao.updateById(roleAction);
    }

    public boolean hasActionPermission(String userId, String moduleAlias, String actionCode) {
        return !effectiveActionGrants(userId, moduleAlias, actionCode).isEmpty();
    }

    public List<RoleAction> effectiveActionGrants(String userId, String moduleAlias, String actionCode) {
        Set<String> roleIds = effectiveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        String permissionActionCode = permissionActionCode(actionCode);
        return roleActionDao.query(Criteria.of()
                        .in("roleId", List.copyOf(roleIds))
                        .eq("moduleAlias", requireModuleAlias(moduleAlias))
                        .eq("actionCode", permissionActionCode)
                        .eq("enabled", Boolean.TRUE),
                ALL);
    }

    public Set<String> effectiveRoleIds(String userId) {
        String validUserId = Preconditions.requireText(userId, "userId");
        List<RoleUser> bindings = roleUserDao.query(scopedChildCriteria(Criteria.of().eq("userId", validUserId)), ALL);
        LinkedHashSet<String> effective = new LinkedHashSet<>();
        for (RoleUser binding : bindings) {
            Role role = select(binding.getRoleId());
            if (role == null || !Boolean.TRUE.equals(role.getEnabled())) {
                continue;
            }
            effective.add(role.getId());
            if (role.getRoleKind() == RoleKind.GROUP) {
                effective.addAll(expandGroupRoleIds(role.getMemberRoleIds()));
            }
        }
        return effective;
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
        roleUserDao.query(scopedChildCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(binding -> roleUserDao.deleteById(binding.getId()));
        roleActionDao.query(scopedChildCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(action -> roleActionDao.deleteById(action.getId()));
        removeRoleFromGroups(id);
    }

    private Role requireEnabledRole(String roleId) {
        Role role = requireEnabled(Preconditions.requireText(roleId, "roleId"), "role is not active: " + roleId);
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        return role;
    }

    private Role requireConfigurableRole(String roleId) {
        Role role = requireEnabledRole(roleId);
        if (role.getRoleKind() == RoleKind.GROUP) {
            throw new PlatformException("role group cannot be granted directly: " + roleId);
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

    private void ensureDataScopeRoleBindingValid(String roleId, String userId) {
        LinkedHashSet<String> roleIds = new LinkedHashSet<>(effectiveRoleIds(userId));
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

    private RoleUser findRoleUser(String roleId, String userId) {
        return roleUserDao.query(scopedChildCriteria(Criteria.of()
                        .eq("roleId", Preconditions.requireText(roleId, "roleId"))
                        .eq("userId", Preconditions.requireText(userId, "userId"))),
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
            Preconditions.requireText(scopeCondition, "scopeCondition");
        }
        if (policy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            Preconditions.requireText(referenceFieldId, "referenceFieldId");
        }
        return policy;
    }

    private TenantScopePolicy normalizeTenantScopePolicy(TenantScopePolicy tenantScopePolicy) {
        return tenantScopePolicy == null ? TenantScopePolicy.CURRENT_TENANT : tenantScopePolicy;
    }

    private String normalizeRoleIdCsv(String value) {
        Set<String> ids = parseRoleIds(value);
        return ids.isEmpty() ? null : String.join(",", ids);
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

    private String permissionActionCode(String actionCode) {
        return PlatformAction.permissionActionCodeOf(requireActionCode(actionCode));
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
}
