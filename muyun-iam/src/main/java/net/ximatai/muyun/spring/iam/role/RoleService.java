package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.form.FormAbility;
import net.ximatai.muyun.spring.ability.form.FormDescriptor;
import net.ximatai.muyun.spring.ability.form.FormField;
import net.ximatai.muyun.spring.ability.form.FormValueType;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.BusinessPrincipal;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
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
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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
        ReferenceAbility<Role>,
        FormAbility<Role>,
        QueryAbility<Role> {
    public static final String MODULE_ALIAS = "iam.role";
    public static final String PLATFORM_SUPER_ADMIN_ROLE_ID = "platform.role.super_admin";
    public static final String PLATFORM_SUPER_ADMIN_ROLE_TITLE = "平台超级管理员";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final AccountRoleGrantDao accountRoleGrantDao;
    private final EmploymentRoleGrantDao employmentRoleGrantDao;
    private final RoleActionDao roleActionDao;
    private final RoleActionGrantVerifier grantVerifier;
    private final UserAccountService userAccountService;
    private final EmployeeService employeeService;
    private final EmployeePositionService employeePositionService;
    private final EmployeeAccountService employeeAccountService;
    private final OrganizationService organizationService;

    public RoleService(RoleDao roleDao,
                       AccountRoleGrantDao accountRoleGrantDao,
                       EmploymentRoleGrantDao employmentRoleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier) {
        this(roleDao, accountRoleGrantDao, employmentRoleGrantDao, roleActionDao, activeTenantVerifier,
                RoleActionGrantVerifier.platformActionsOnly(), null, null, null, null,
                (OrganizationService) null);
    }

    @Autowired
    public RoleService(RoleDao roleDao,
                       AccountRoleGrantDao accountRoleGrantDao,
                       EmploymentRoleGrantDao employmentRoleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService,
                       EmployeeAccountService employeeAccountService,
                       ObjectProvider<OrganizationService> organizationService) {
        this(roleDao, accountRoleGrantDao, employmentRoleGrantDao, roleActionDao, activeTenantVerifier,
                grantVerifier, userAccountService, employeeService, employeePositionService, employeeAccountService,
                organizationService == null ? null : organizationService.getIfAvailable());
    }

    public RoleService(RoleDao roleDao,
                       AccountRoleGrantDao accountRoleGrantDao,
                       EmploymentRoleGrantDao employmentRoleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService,
                       EmployeeAccountService employeeAccountService) {
        this(roleDao, accountRoleGrantDao, employmentRoleGrantDao, roleActionDao, activeTenantVerifier,
                grantVerifier, userAccountService, employeeService, employeePositionService, employeeAccountService,
                (OrganizationService) null);
    }

    public RoleService(RoleDao roleDao,
                       AccountRoleGrantDao accountRoleGrantDao,
                       EmploymentRoleGrantDao employmentRoleGrantDao,
                       RoleActionDao roleActionDao,
                       ActiveTenantVerifier activeTenantVerifier,
                       RoleActionGrantVerifier grantVerifier,
                       UserAccountService userAccountService,
                       EmployeeService employeeService,
                       EmployeePositionService employeePositionService,
                       EmployeeAccountService employeeAccountService,
                       OrganizationService organizationService) {
        super(MODULE_ALIAS, Role.class, roleDao, activeTenantVerifier);
        this.accountRoleGrantDao = Objects.requireNonNull(accountRoleGrantDao, "accountRoleGrantDao must not be null");
        this.employmentRoleGrantDao = Objects.requireNonNull(employmentRoleGrantDao,
                "employmentRoleGrantDao must not be null");
        this.roleActionDao = Objects.requireNonNull(roleActionDao, "roleActionDao must not be null");
        this.grantVerifier = Objects.requireNonNull(grantVerifier, "grantVerifier must not be null");
        this.userAccountService = userAccountService;
        this.employeeService = employeeService;
        this.employeePositionService = employeePositionService;
        this.employeeAccountService = employeeAccountService;
        this.organizationService = organizationService;
    }

    @Override
    public FormDescriptor formDescriptor() {
        return FormDescriptor.builder(MODULE_ALIAS)
                .title("角色")
                .field(FormField.of("assignmentType").withTitle("授权层级").asRequired())
                .field(FormField.of("roleKind").withTitle("角色类型").asRequired())
                .field(FormField.of("title").withTitle("角色名称").asRequired())
                .field(FormField.of("memberRoleIds").withTitle("成员角色"))
                .field(FormField.of("ownerScopeType").withTitle("定义归属").asRequired())
                .field(FormField.of("ownerScopeId").withTitle("归属对象"))
                .field(FormField.of("ownerScopeKey").withTitle("归属键").asReadOnly())
                .field(FormField.of("sharePolicy").withTitle("共享策略").asRequired())
                .field(FormField.of("builtIn", FormValueType.BOOLEAN).withTitle("内置角色").asReadOnly())
                .field(FormField.of("systemManaged", FormValueType.BOOLEAN).withTitle("系统托管").asReadOnly())
                .field(FormField.of("description", FormValueType.TEXT).withTitle("角色描述"))
                .build();
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("assignmentType", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("授权层级"))
                .field(QueryField.of("roleKind", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("角色类型"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("角色名称").withQuickSearch().withSortable())
                .field(QueryField.of("ownerScopeType", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("定义归属"))
                .field(QueryField.of("ownerScopeId", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("归属对象"))
                .field(QueryField.of("ownerScopeKey", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("归属键"))
                .field(QueryField.of("sharePolicy", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("共享策略"))
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("builtIn", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("内置角色"))
                .field(QueryField.of("systemManaged", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("系统托管"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间")
                        .withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间")
                        .withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("title"))
                .build();
    }

    @Override
    public void normalizeBeforeMutation(Role role) {
        if (role.getAssignmentType() == null) {
            role.setAssignmentType(RoleAssignmentType.EMPLOYMENT);
        }
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        if (role.getRoleKind() == RoleKind.GROUP || role.getRoleKind() == RoleKind.DATA_GRANT) {
            role.setAssignmentType(RoleAssignmentType.EMPLOYMENT);
        }
        normalizeOwnerAndSharePolicy(role);
        if (role.getBuiltIn() == null) {
            role.setBuiltIn(false);
        }
        if (role.getSystemManaged() == null) {
            role.setSystemManaged(false);
        }
        if (role.getRoleKind() == RoleKind.GROUP) {
            role.setMemberRoleIds(normalizeRoleIdCsv(role.getMemberRoleIds()));
            validateGroupMembers(role.getMemberRoleIds());
            validateGroupDataGrantUsage(role);
        } else {
            role.setMemberRoleIds(null);
        }
    }

    @Override
    public void beforePrepareInsert(Role role) {
        normalizeBeforeMutation(role);
        if (role.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM) {
            requirePlatformRoleSystemContext();
            return;
        }
        requireActiveTenantMutationContext();
    }

    @Override
    public void beforeInsert(Role role) {
        requireSystemManagedMutationAllowed(role, "create");
    }

    @Override
    public void beforeUpdate(Role role) {
        Role existing = role == null || role.getId() == null ? null : select(role.getId());
        if (existing != null && existing.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM) {
            requirePlatformRoleSystemContext();
        } else {
            requireActiveTenantMutationContext();
        }
        normalizeBeforeMutation(role);
        requireSystemManagedMutationAllowed(existing, "update");
        requireSystemManagedMutationAllowed(role, "update");
        requireStructuralFieldsUnchanged(existing, role);
    }

    @Override
    public void beforeDelete(String id) {
        Role role = select(id);
        if (role != null && role.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM) {
            requirePlatformRoleSystemContext();
        } else {
            requireActiveTenantMutationContext();
        }
        requireSystemManagedMutationAllowed(role, "delete");
    }

    public String grantAccountRole(String roleId,
                                   String userId,
                                   ManagementScopeType managementScopeType,
                                   String managementScopeId) {
        return grantAccountRoleIfAbsent(roleId, userId, managementScopeType, managementScopeId).grantId();
    }

    public int revokeAccountRole(String roleId,
                                 String userId,
                                 ManagementScopeType managementScopeType,
                                 String managementScopeId) {
        Role role = requireBindableRole(roleId);
        requireAccountRole(role);
        requireSystemManagedMutationAllowed(role, "revoke account role");
        AccountRoleGrant grant = findAccountRoleGrant(role.getId(), userId, managementScopeType, managementScopeId);
        return grant == null ? 0 : accountRoleGrantDao.deleteById(grant.getId());
    }

    public int deleteAccountRoleGrant(String roleId, String grantId) {
        Role role = requireEnabledRole(roleId);
        requireSystemManagedMutationAllowed(role, "delete account role grant");
        AccountRoleGrant grant = accountRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("id", Preconditions.requireText(grantId, "grantId"))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
        if (grant == null || !SortAbility.sameValue(role.getId(), grant.getRoleId())) {
            throw new PlatformException("account role grant does not belong to role: " + grantId);
        }
        return accountRoleGrantDao.deleteById(grant.getId());
    }

    public List<AccountRoleGrant> accountRoleGrants(String roleId) {
        Role role = requireEnabledRole(roleId);
        requireAccountRole(role);
        return accountRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("roleId", role.getId())
                        .eq("enabled", Boolean.TRUE)), ALL);
    }

    public List<String> userIds(String roleId) {
        return accountRoleGrants(roleId).stream()
                .map(AccountRoleGrant::getUserId)
                .distinct()
                .toList();
    }

    public String grantEmploymentRole(String roleId, String employeePositionId) {
        return grantEmploymentRoleIfAbsent(roleId, employeePositionId).grantId();
    }

    public int revokeEmploymentRole(String roleId, String employeePositionId) {
        Role role = requireBindableRole(roleId);
        requireEmploymentAssignableRole(role);
        requireSystemManagedMutationAllowed(role, "revoke employment role");
        EmploymentRoleGrant grant = findEmploymentRoleGrant(role.getId(), employeePositionId);
        return grant == null ? 0 : employmentRoleGrantDao.deleteById(grant.getId());
    }

    public int deleteEmploymentRoleGrant(String roleId, String grantId) {
        Role role = requireEnabledRole(roleId);
        requireSystemManagedMutationAllowed(role, "delete employment role grant");
        EmploymentRoleGrant grant = employmentRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("id", Preconditions.requireText(grantId, "grantId"))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
        if (grant == null || !SortAbility.sameValue(role.getId(), grant.getRoleId())) {
            throw new PlatformException("employment role grant does not belong to role: " + grantId);
        }
        return employmentRoleGrantDao.deleteById(grant.getId());
    }

    public List<EmploymentRoleGrant> employmentRoleGrants(String roleId) {
        Role role = requireEnabledRole(roleId);
        requireEmploymentAssignableRole(role);
        return employmentRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("roleId", role.getId())
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
        Role role = requireConfigurableRole(roleId);
        requireSystemManagedMutationAllowed(role, "grant action");

        String validModuleAlias = requireModuleAlias(moduleAlias);
        String requestedActionCode = requireActionCode(actionCode);
        String validActionCode = resolveGrantablePermissionActionCode(validModuleAlias, requestedActionCode);
        DataScopePolicy validDataScopePolicy = normalizeDataScopePolicy(role, dataScopePolicy, scopeCondition,
                referenceFieldId);

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
        prepareRoleActionInsert(role, roleAction);
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

    public int revokeAction(String roleId, String moduleAlias, String actionCode) {
        Role role = requireEnabledRole(roleId);
        requireSystemManagedMutationAllowed(role, "revoke action");
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

    public boolean hasActionPermission(BusinessPrincipal principal, String moduleAlias, String actionCode) {
        return !effectiveActionGrants(principal, moduleAlias, actionCode).isEmpty();
    }

    public List<RoleAction> effectiveActionGrants(String userId, String moduleAlias, String actionCode) {
        return effectiveActionGrantsWithContext(userId, moduleAlias, actionCode).stream()
                .map(EffectiveRoleActionGrant::actionGrant)
                .distinct()
                .toList();
    }

    public List<RoleAction> effectiveActionGrants(BusinessPrincipal principal, String moduleAlias, String actionCode) {
        return effectiveActionGrantsWithContext(principal, moduleAlias, actionCode).stream()
                .map(EffectiveRoleActionGrant::actionGrant)
                .distinct()
                .toList();
    }

    public List<EffectiveRoleActionGrant> effectiveActionGrantsWithContext(String userId,
                                                                           String moduleAlias,
                                                                           String actionCode) {
        return effectiveActionGrantsWithContext(effectiveRoleGrants(userId), moduleAlias, actionCode);
    }

    public List<EffectiveRoleActionGrant> effectiveActionGrantsWithContext(BusinessPrincipal principal,
                                                                           String moduleAlias,
                                                                           String actionCode) {
        return effectiveActionGrantsWithContext(effectiveRoleGrants(principal), moduleAlias, actionCode);
    }

    private List<EffectiveRoleActionGrant> effectiveActionGrantsWithContext(List<EffectiveRoleGrant> roleGrants,
                                                                            String moduleAlias,
                                                                            String actionCode) {
        if (roleGrants.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> roleIds = new LinkedHashSet<>();
        roleGrants.stream()
                .filter(grant -> grant.sourceType() != RoleAssignmentType.EMPLOYMENT
                        || !dataGrantRole(grant.roleId()))
                .map(EffectiveRoleGrant::roleId)
                .forEach(roleIds::add);
        if (roleIds.isEmpty()) {
            return List.of();
        }
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
        ArrayList<EffectiveRoleActionGrant> effective = new ArrayList<>();
        for (RoleAction actionGrant : actionGrants) {
            if (dataGrantRole(actionGrant.getRoleId())) {
                continue;
            }
            List<EffectiveRoleGrant> matchedRoleGrants = grantsByRoleId.get(actionGrant.getRoleId());
            if (matchedRoleGrants == null || matchedRoleGrants.isEmpty()) {
                continue;
            }
            matchedRoleGrants.forEach(roleGrant -> effective.add(new EffectiveRoleActionGrant(actionGrant, roleGrant)));
        }
        return List.copyOf(effective);
    }

    public RoleAction inheritedDataGrantAction(EffectiveRoleGrant roleGrant, String moduleAlias, String actionCode) {
        if (roleGrant == null || roleGrant.employeePositionId() == null) {
            return null;
        }
        String validModuleAlias = requireModuleAlias(moduleAlias);
        List<String> dataGrantRoleIds = effectiveDataGrantRoleIds(roleGrant.employeePositionId(), null);
        if (dataGrantRoleIds.isEmpty()) {
            return null;
        }
        List<RoleAction> grants = roleActionDao.query(Criteria.of()
                        .in("roleId", dataGrantRoleIds)
                        .eq("moduleAlias", validModuleAlias)
                        .eq("actionCode", permissionActionCode(actionCode))
                        .eq("enabled", Boolean.TRUE),
                ALL);
        if (grants.size() > 1) {
            throw new PlatformException("employment has more than one inherited data grant action: "
                    + roleGrant.employeePositionId());
        }
        return grants.stream().findFirst().orElse(null);
    }

    public Set<String> effectiveRoleIds(String userId) {
        LinkedHashSet<String> effective = new LinkedHashSet<>();
        effectiveRoleGrants(userId).stream()
                .map(EffectiveRoleGrant::roleId)
                .forEach(effective::add);
        return effective;
    }

    public Set<String> effectiveRoleIds(BusinessPrincipal principal) {
        LinkedHashSet<String> effective = new LinkedHashSet<>();
        effectiveRoleGrants(principal).stream()
                .map(EffectiveRoleGrant::roleId)
                .forEach(effective::add);
        return effective;
    }

    public List<EffectiveRoleGrant> effectiveRoleGrants(String userId) {
        String validUserId = Preconditions.requireText(userId, "userId");
        ArrayList<EffectiveRoleGrant> effective = new ArrayList<>();
        accountRoleGrantsForUser(validUserId).forEach(grant -> appendAccountRoleGrant(effective, grant));

        String employeeId = employeeAccountService == null ? null : employeeAccountService.employeeIdOfUser(validUserId);
        if (employeeId == null || employeeId.isBlank() || employeeService == null || employeePositionService == null) {
            return List.copyOf(effective);
        }
        Employee employee = employeeService.select(employeeId);
        if (employee == null || !Boolean.TRUE.equals(employee.getEnabled())) {
            return List.copyOf(effective);
        }
        for (EmployeePosition position : employeePositionService.positions(employee.getId())) {
            if (position == null || !Boolean.TRUE.equals(position.getEnabled())) {
                continue;
            }
            effectiveEmploymentRoleGrants(position.getId())
                    .forEach(grant -> appendEmploymentRoleGrant(effective, grant, position));
        }
        return List.copyOf(effective);
    }

    public List<EffectiveRoleGrant> effectiveRoleGrants(BusinessPrincipal principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        ArrayList<EffectiveRoleGrant> effective = new ArrayList<>();
        if (principal.userId() != null) {
            accountRoleGrantsForUser(principal.userId()).forEach(grant -> appendAccountRoleGrant(effective, grant));
        }
        if (principal.employeePositionId() != null) {
            EmployeePosition position = employeePositionService == null
                    ? null
                    : employeePositionService.select(principal.employeePositionId());
            if (isActivePrincipalPosition(principal, position)) {
                effectiveEmploymentRoleGrants(principal.employeePositionId())
                        .forEach(grant -> appendEmploymentRoleGrant(effective, grant, position));
            }
        }
        return List.copyOf(effective);
    }

    public List<RoleAction> alignedActions(String roleId, List<String> moduleAliases, List<String> actionCodes) {
        Preconditions.requireText(roleId, "roleId");
        if (moduleAliases == null || moduleAliases.isEmpty() || actionCodes == null || actionCodes.isEmpty()) {
            return List.of();
        }
        List<RoleAction> configured = roleActionDao.query(activeCriteria(Criteria.of()
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
        roleActionDao.query(activeCriteria(Criteria.of()
                                .eq("roleId", validRoleId)
                                .in("moduleAlias", moduleAliases)
                                .in("actionCode", actionCodes)),
                        ALL,
                        Sort.asc("moduleAlias"),
                        Sort.asc("actionCode"))
                .forEach(action -> configuredByKey.put(actionKey(action.getModuleAlias(), action.getActionCode()), action));

        LinkedHashMap<String, List<RolePermissionAction>> actionsByModule = new LinkedHashMap<>();
        actionByKey.values().forEach(action -> actionsByModule
                .computeIfAbsent(action.moduleAlias(), ignored -> new ArrayList<>())
                .add(RolePermissionAction.of(action,
                        configuredByKey.get(actionKey(action.moduleAlias(), action.permissionActionCode())))));
        List<RolePermissionMatrix.Module> modules = actionsByModule.entrySet().stream()
                .map(entry -> new RolePermissionMatrix.Module(entry.getKey(), entry.getValue()))
                .toList();
        return new RolePermissionMatrix(validRoleId, modules);
    }

    @Override
    public void afterDelete(String id, Role role, int deleted) {
        accountRoleGrantDao.query(activeCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(binding -> accountRoleGrantDao.deleteById(binding.getId()));
        employmentRoleGrantDao.query(activeCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(binding -> employmentRoleGrantDao.deleteById(binding.getId()));
        roleActionDao.query(activeCriteria(Criteria.of().eq("roleId", id)), ALL)
                .forEach(action -> roleActionDao.deleteById(action.getId()));
        removeRoleFromGroups(id);
    }

    private Role requireEnabledRole(String roleId) {
        Role role = requireEnabled(Preconditions.requireText(roleId, "roleId"), "role is not active: " + roleId);
        normalizeLoadedRoleDefaults(role);
        return role;
    }

    private Role requireBindableRole(String roleId) {
        String validRoleId = Preconditions.requireText(roleId, "roleId");
        Role role = select(validRoleId);
        if (role == null && TenantContext.currentTenantId().isPresent()) {
            role = selectPlatformSharedRole(validRoleId);
        }
        if (role == null || !Boolean.TRUE.equals(role.getEnabled())) {
            throw new PlatformException("role is not active: " + roleId);
        }
        normalizeLoadedRoleDefaults(role);
        if (role.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM
                && role.getSharePolicy() != RoleSharePolicy.PLATFORM
                && TenantContext.currentTenantId().isPresent()) {
            throw new PlatformException("platform private role cannot be bound by tenant: " + roleId);
        }
        return role;
    }

    private Role selectPlatformSharedRole(String roleId) {
        try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter("resolve platform shared role")) {
            return getDao().query(activeCriteria(Criteria.of()
                            .eq("id", Preconditions.requireText(roleId, "roleId"))
                            .eq("ownerScopeType", RoleOwnerScopeType.PLATFORM)
                            .eq("sharePolicy", RoleSharePolicy.PLATFORM)),
                    new PageRequest(0, 1)).stream().findFirst().orElse(null);
        }
    }

    private Role selectGrantedRole(String roleId) {
        if (roleId == null || roleId.isBlank()) {
            return null;
        }
        Role role = select(roleId);
        if (role == null && TenantContext.currentTenantId().isPresent()) {
            role = selectPlatformSharedRole(roleId);
        }
        if (role != null) {
            normalizeLoadedRoleDefaults(role);
        }
        return role;
    }

    private void normalizeLoadedRoleDefaults(Role role) {
        if (role.getAssignmentType() == null) {
            role.setAssignmentType(RoleAssignmentType.EMPLOYMENT);
        }
        if (role.getRoleKind() == null) {
            role.setRoleKind(RoleKind.STANDARD);
        }
        if (role.getOwnerScopeType() == null) {
            role.setOwnerScopeType(RoleOwnerScopeType.TENANT);
        }
        if (role.getSharePolicy() == null) {
            role.setSharePolicy(RoleSharePolicy.PRIVATE);
        }
        if (role.getOwnerScopeKey() == null || role.getOwnerScopeKey().isBlank()) {
            role.setOwnerScopeKey(ownerScopeKey(role.getOwnerScopeType(), role.getOwnerScopeId()));
        }
    }

    private Role requireConfigurableRole(String roleId) {
        Role role = requireEnabledRole(roleId);
        if (role.getRoleKind() == RoleKind.GROUP) {
            throw new PlatformException("role group cannot be granted actions directly: " + roleId);
        }
        return role;
    }

    private void requireAccountRole(Role role) {
        if (role.getAssignmentType() != RoleAssignmentType.ACCOUNT) {
            throw new PlatformException("role is not account role: " + role.getId());
        }
        if (role.getRoleKind() == RoleKind.GROUP || role.getRoleKind() == RoleKind.DATA_GRANT) {
            throw new PlatformException("role kind cannot be granted to account: " + role.getId());
        }
    }

    private void requireEmploymentAssignableRole(Role role) {
        if (role.getAssignmentType() != RoleAssignmentType.EMPLOYMENT) {
            throw new PlatformException("role is not employment role: " + role.getId());
        }
    }

    private void requireSystemManagedMutationAllowed(Role role, String operation) {
        if (role != null && Boolean.TRUE.equals(role.getSystemManaged()) && !CurrentUserContext.isSystem()) {
            throw new PlatformException("system managed role cannot be modified by " + operation + ": " + role.getId());
        }
    }

    private void requireStructuralFieldsUnchanged(Role existing, Role updated) {
        if (existing == null || updated == null) {
            return;
        }
        if (updated.getAssignmentType() != null && existing.getAssignmentType() != updated.getAssignmentType()) {
            throw new PlatformException("role assignment type cannot be changed after creation: " + existing.getId());
        }
        if (updated.getRoleKind() != null && existing.getRoleKind() != updated.getRoleKind()) {
            throw new PlatformException("role kind cannot be changed after creation: " + existing.getId());
        }
        if (updated.getOwnerScopeType() != null && existing.getOwnerScopeType() != updated.getOwnerScopeType()) {
            throw new PlatformException("role owner scope type cannot be changed after creation: " + existing.getId());
        }
        if (updated.getOwnerScopeId() != null && !Objects.equals(existing.getOwnerScopeId(), updated.getOwnerScopeId())) {
            throw new PlatformException("role owner scope id cannot be changed after creation: " + existing.getId());
        }
    }

    private void normalizeOwnerAndSharePolicy(Role role) {
        if (role.getOwnerScopeType() == null) {
            role.setOwnerScopeType(defaultOwnerScopeType());
        }
        role.setOwnerScopeId(normalizeOwnerScopeId(role.getOwnerScopeType(), role.getOwnerScopeId()));
        role.setOwnerScopeKey(ownerScopeKey(role.getOwnerScopeType(), role.getOwnerScopeId()));
        if (role.getSharePolicy() == null) {
            role.setSharePolicy(RoleSharePolicy.PRIVATE);
        }
        validateSharePolicy(role.getOwnerScopeType(), role.getSharePolicy(), role.getId());
        validateOwnerScope(role);
    }

    private RoleOwnerScopeType defaultOwnerScopeType() {
        return TenantContext.isSystem() ? RoleOwnerScopeType.PLATFORM : RoleOwnerScopeType.TENANT;
    }

    private void requirePlatformRoleSystemContext() {
        if (!TenantContext.isSystem()) {
            throw new PlatformException("platform role management requires system tenant context");
        }
    }

    private String normalizeOwnerScopeId(RoleOwnerScopeType ownerScopeType, String ownerScopeId) {
        if (ownerScopeType == RoleOwnerScopeType.PLATFORM) {
            return null;
        }
        if (ownerScopeType == RoleOwnerScopeType.TENANT) {
            String normalized = normalizeBlank(ownerScopeId);
            String currentTenantId = TenantContext.currentTenantId()
                    .orElseThrow(() -> new PlatformException("tenant role requires tenant owner scope id"));
            if (normalized != null && !Objects.equals(normalized, currentTenantId)) {
                throw new PlatformException("tenant role owner scope id must match current tenant: " + normalized);
            }
            return currentTenantId;
        }
        return Preconditions.requireText(ownerScopeId, "ownerScopeId");
    }

    private String ownerScopeKey(RoleOwnerScopeType ownerScopeType, String ownerScopeId) {
        if (ownerScopeType == RoleOwnerScopeType.PLATFORM) {
            return "platform";
        }
        if (ownerScopeType == RoleOwnerScopeType.TENANT) {
            return "tenant:" + Preconditions.requireText(ownerScopeId, "ownerScopeId");
        }
        return "organization:" + Preconditions.requireText(ownerScopeId, "ownerScopeId");
    }

    private void validateOwnerScope(Role role) {
        if (role.getOwnerScopeType() != RoleOwnerScopeType.ORGANIZATION || organizationService == null) {
            return;
        }
        Organization organization = organizationService.requireEnabled(
                role.getOwnerScopeId(),
                "role owner organization is not active: " + role.getOwnerScopeId());
        String currentTenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("organization role requires tenant context"));
        if (!Objects.equals(currentTenantId, organization.getTenantId())) {
            throw new PlatformException("role owner organization does not belong to current tenant: "
                    + role.getOwnerScopeId());
        }
    }

    private void validateSharePolicy(RoleOwnerScopeType ownerScopeType, RoleSharePolicy sharePolicy, String roleId) {
        if (ownerScopeType == RoleOwnerScopeType.PLATFORM) {
            if (sharePolicy == RoleSharePolicy.PRIVATE || sharePolicy == RoleSharePolicy.PLATFORM) {
                return;
            }
            throw new PlatformException("platform role only supports private or platform share policy: " + roleId);
        }
        if (ownerScopeType == RoleOwnerScopeType.TENANT) {
            if (sharePolicy == RoleSharePolicy.PRIVATE || sharePolicy == RoleSharePolicy.TENANT) {
                return;
            }
            throw new PlatformException("tenant role only supports private or tenant share policy: " + roleId);
        }
        if (sharePolicy == RoleSharePolicy.PRIVATE || sharePolicy == RoleSharePolicy.OWNER_AND_CHILDREN) {
            return;
        }
        throw new PlatformException("organization role only supports private or owner-and-children share policy: "
                + roleId);
    }

    private void validateGroupMembers(String memberRoleIds) {
        int dataGrantMembers = 0;
        for (String memberRoleId : parseRoleIds(memberRoleIds)) {
            Role member = selectGrantedRole(memberRoleId);
            if (member == null) {
                throw new PlatformException("role group contains missing role: " + memberRoleId);
            }
            if (member.getAssignmentType() != RoleAssignmentType.EMPLOYMENT) {
                throw new PlatformException("role group can only contain employment roles: " + memberRoleId);
            }
            if (member.getRoleKind() != RoleKind.STANDARD && member.getRoleKind() != RoleKind.DATA_GRANT) {
                throw new PlatformException("role group can only contain standard or data grant roles: " + memberRoleId);
            }
            if (!Boolean.TRUE.equals(member.getEnabled())) {
                throw new PlatformException("role group contains inactive role: " + memberRoleId);
            }
            if (member.getRoleKind() == RoleKind.DATA_GRANT) {
                dataGrantMembers++;
            }
        }
        if (dataGrantMembers > 1) {
            throw new PlatformException("role group can contain at most one data grant role");
        }
    }

    private Set<String> expandGroupRoleIds(String memberRoleIds) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        for (String memberRoleId : parseRoleIds(memberRoleIds)) {
            Role member = selectGrantedRole(memberRoleId);
            if (member != null
                    && member.getAssignmentType() == RoleAssignmentType.EMPLOYMENT
                    && (member.getRoleKind() == RoleKind.STANDARD || member.getRoleKind() == RoleKind.DATA_GRANT)
                    && Boolean.TRUE.equals(member.getEnabled())) {
                expanded.add(member.getId());
            }
        }
        return expanded;
    }

    private GrantResult grantAccountRoleIfAbsent(String roleId,
                                                 String userId,
                                                 ManagementScopeType managementScopeType,
                                                 String managementScopeId) {
        Role role = requireBindableRole(roleId);
        requireAccountRole(role);
        requireSystemManagedMutationAllowed(role, "grant account role");
        String validUserId = Preconditions.requireText(userId, "userId");
        if (userAccountService != null) {
            userAccountService.requireEnabled(validUserId, "user account is not active: " + validUserId);
        }
        ManagementScopeType validScopeType = normalizeManagementScopeType(managementScopeType);
        String validScopeId = normalizeManagementScopeId(validScopeType, managementScopeId);
        AccountRoleGrant existing = findAccountRoleGrant(role.getId(), validUserId, validScopeType, validScopeId);
        if (existing != null) {
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                existing.setEnabled(true);
                prepareChildUpdate(existing);
                accountRoleGrantDao.updateById(existing);
            }
            return new GrantResult(existing.getId(), false);
        }
        AccountRoleGrant grant = new AccountRoleGrant();
        grant.setRoleId(role.getId());
        grant.setUserId(validUserId);
        grant.setManagementScopeType(validScopeType);
        grant.setManagementScopeId(validScopeId);
        grant.setEnabled(true);
        prepareChildInsert(grant);
        return new GrantResult(accountRoleGrantDao.insert(grant), true);
    }

    private GrantResult grantEmploymentRoleIfAbsent(String roleId, String employeePositionId) {
        Role role = requireBindableRole(roleId);
        requireEmploymentAssignableRole(role);
        requireSystemManagedMutationAllowed(role, "grant employment role");
        String validEmployeePositionId = Preconditions.requireText(employeePositionId, "employeePositionId");
        if (employeePositionService != null) {
            employeePositionService.requireEnabled(validEmployeePositionId,
                    "employee position is not active: " + validEmployeePositionId);
        }
        ensureDataGrantUnique(validEmployeePositionId, role);
        EmploymentRoleGrant existing = findEmploymentRoleGrant(role.getId(), validEmployeePositionId);
        if (existing != null) {
            if (!Boolean.TRUE.equals(existing.getEnabled())) {
                existing.setEnabled(true);
                prepareChildUpdate(existing);
                employmentRoleGrantDao.updateById(existing);
            }
            return new GrantResult(existing.getId(), false);
        }
        EmploymentRoleGrant grant = new EmploymentRoleGrant();
        grant.setRoleId(role.getId());
        grant.setEmployeePositionId(validEmployeePositionId);
        grant.setEnabled(true);
        prepareChildInsert(grant);
        return new GrantResult(employmentRoleGrantDao.insert(grant), true);
    }

    private void ensureDataGrantUnique(String employeePositionId, Role newRole) {
        if (effectiveDataGrantRoleIds(employeePositionId, newRole).size() > 1) {
            throw new PlatformException("employment can have at most one data grant role: " + employeePositionId);
        }
    }

    private void validateGroupDataGrantUsage(Role group) {
        if (group == null || group.getId() == null || group.getId().isBlank()) {
            return;
        }
        List<EmploymentRoleGrant> grants = employmentRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("roleId", group.getId())
                        .eq("enabled", Boolean.TRUE)), ALL);
        for (EmploymentRoleGrant grant : grants) {
            if (grant == null || grant.getEmployeePositionId() == null) {
                continue;
            }
            ensureDataGrantUnique(grant.getEmployeePositionId(), group);
        }
    }

    private List<String> effectiveDataGrantRoleIds(String employeePositionId, Role extraRole) {
        LinkedHashSet<String> roleIds = effectiveEmploymentRoleGrants(employeePositionId).stream()
                .map(EmploymentRoleGrant::getRoleId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (extraRole != null && extraRole.getId() != null) {
            roleIds.add(extraRole.getId());
        }
        LinkedHashSet<String> dataGrantRoleIds = new LinkedHashSet<>();
        for (String roleId : roleIds) {
            Role role = extraRole != null && SortAbility.sameValue(extraRole.getId(), roleId)
                    ? extraRole
                    : selectGrantedRole(roleId);
            collectDataGrantRoleIds(dataGrantRoleIds, role);
        }
        return List.copyOf(dataGrantRoleIds);
    }

    private void collectDataGrantRoleIds(Set<String> dataGrantRoleIds, Role role) {
        if (role == null || !Boolean.TRUE.equals(role.getEnabled())) {
            return;
        }
        if (role.getRoleKind() == RoleKind.DATA_GRANT) {
            dataGrantRoleIds.add(role.getId());
            return;
        }
        if (role.getRoleKind() != RoleKind.GROUP) {
            return;
        }
        for (String memberRoleId : parseRoleIds(role.getMemberRoleIds())) {
            Role member = selectGrantedRole(memberRoleId);
            if (member != null && member.getRoleKind() == RoleKind.DATA_GRANT
                    && Boolean.TRUE.equals(member.getEnabled())) {
                dataGrantRoleIds.add(member.getId());
            }
        }
    }

    private List<AccountRoleGrant> accountRoleGrantsForUser(String userId) {
        return accountRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("userId", Preconditions.requireText(userId, "userId"))
                        .eq("enabled", Boolean.TRUE)), ALL);
    }

    private List<EmploymentRoleGrant> effectiveEmploymentRoleGrants(String employeePositionId) {
        return employmentRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("employeePositionId", Preconditions.requireText(employeePositionId, "employeePositionId"))
                        .eq("enabled", Boolean.TRUE)), ALL);
    }

    private void appendAccountRoleGrant(List<EffectiveRoleGrant> effective, AccountRoleGrant grant) {
        if (grant == null || !Boolean.TRUE.equals(grant.getEnabled())) {
            return;
        }
        Role role = selectGrantedRole(grant.getRoleId());
        if (role == null || role.getAssignmentType() != RoleAssignmentType.ACCOUNT
                || !Boolean.TRUE.equals(role.getEnabled())) {
            return;
        }
        effective.add(EffectiveRoleGrant.account(
                role.getId(),
                grant.getUserId(),
                grant.getManagementScopeType(),
                grant.getManagementScopeId()));
    }

    private void appendEmploymentRoleGrant(List<EffectiveRoleGrant> effective,
                                           EmploymentRoleGrant grant,
                                           EmployeePosition position) {
        if (grant == null || !Boolean.TRUE.equals(grant.getEnabled()) || position == null) {
            return;
        }
        Role role = selectGrantedRole(grant.getRoleId());
        if (role == null || role.getAssignmentType() != RoleAssignmentType.EMPLOYMENT
                || !Boolean.TRUE.equals(role.getEnabled())) {
            return;
        }
        effective.add(EffectiveRoleGrant.employment(
                role.getId(),
                grant.getEmployeePositionId(),
                position.getOrganizationId(),
                position.getDepartmentId()));
        if (role.getRoleKind() == RoleKind.GROUP) {
            for (String memberRoleId : expandGroupRoleIds(role.getMemberRoleIds())) {
                effective.add(EffectiveRoleGrant.employment(
                        memberRoleId,
                        grant.getEmployeePositionId(),
                        position.getOrganizationId(),
                        position.getDepartmentId()));
            }
        }
    }

    private AccountRoleGrant findAccountRoleGrant(String roleId,
                                                  String userId,
                                                  ManagementScopeType managementScopeType,
                                                  String managementScopeId) {
        Criteria criteria = activeCriteria(Criteria.of()
                .eq("roleId", Preconditions.requireText(roleId, "roleId"))
                .eq("userId", Preconditions.requireText(userId, "userId"))
                .eq("managementScopeType", normalizeManagementScopeType(managementScopeType)));
        String validScopeId = normalizeManagementScopeId(normalizeManagementScopeType(managementScopeType),
                managementScopeId);
        if (validScopeId == null) {
            criteria.isNull("managementScopeId");
        } else {
            criteria.eq("managementScopeId", validScopeId);
        }
        return accountRoleGrantDao.query(criteria, new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private EmploymentRoleGrant findEmploymentRoleGrant(String roleId, String employeePositionId) {
        return employmentRoleGrantDao.query(activeCriteria(Criteria.of()
                        .eq("roleId", Preconditions.requireText(roleId, "roleId"))
                        .eq("employeePositionId", Preconditions.requireText(employeePositionId, "employeePositionId"))),
                new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private RoleAction findRoleAction(String roleId, String moduleAlias, String actionCode) {
        return roleActionDao.query(activeCriteria(Criteria.of()
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

    private boolean dataGrantRole(String roleId) {
        Role role = selectGrantedRole(roleId);
        return role != null && role.getRoleKind() == RoleKind.DATA_GRANT;
    }

    private boolean isActivePrincipalPosition(BusinessPrincipal principal, EmployeePosition position) {
        return position != null
                && Boolean.TRUE.equals(position.getEnabled())
                && (principal.employeeId() == null || Objects.equals(principal.employeeId(), position.getEmployeeId()));
    }

    private Map<String, List<EffectiveRoleGrant>> roleGrantsByRoleId(List<EffectiveRoleGrant> roleGrants) {
        LinkedHashMap<String, List<EffectiveRoleGrant>> byRoleId = new LinkedHashMap<>();
        roleGrants.stream()
                .filter(Objects::nonNull)
                .filter(grant -> grant.roleId() != null)
                .forEach(grant -> byRoleId
                        .computeIfAbsent(grant.roleId(), ignored -> new ArrayList<>())
                        .add(grant));
        return byRoleId;
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

    private DataScopePolicy normalizeDataScopePolicy(Role role,
                                                     DataScopePolicy dataScopePolicy,
                                                     String scopeCondition,
                                                     String referenceFieldId) {
        DataScopePolicy policy = dataScopePolicy == null ? DataScopePolicy.NONE : dataScopePolicy;
        if (role.getAssignmentType() == RoleAssignmentType.ACCOUNT && policy != DataScopePolicy.NONE) {
            throw new PlatformException("account role action cannot configure data scope: " + role.getId());
        }
        if (role.getRoleKind() == RoleKind.DATA_GRANT) {
            if (policy == DataScopePolicy.NONE || policy == DataScopePolicy.INHERIT_DATA_GRANT) {
                throw new PlatformException("data grant role must configure concrete data scope: " + role.getId());
            }
        }
        if (policy == DataScopePolicy.CUSTOM) {
            throw new PlatformException("custom data scope policy is not supported yet");
        }
        if (policy == DataScopePolicy.REFERENCE_DEPENDENCY) {
            Preconditions.requireText(referenceFieldId, "referenceFieldId");
        }
        return policy;
    }

    private TenantScopePolicy normalizeTenantScopePolicy(TenantScopePolicy tenantScopePolicy) {
        return tenantScopePolicy == null ? TenantScopePolicy.CURRENT_TENANT : tenantScopePolicy;
    }

    private ManagementScopeType normalizeManagementScopeType(ManagementScopeType managementScopeType) {
        return managementScopeType == null ? ManagementScopeType.TENANT : managementScopeType;
    }

    private String normalizeManagementScopeId(ManagementScopeType managementScopeType, String managementScopeId) {
        if (managementScopeType == ManagementScopeType.PLATFORM) {
            return null;
        }
        return Preconditions.requireText(managementScopeId, "managementScopeId");
    }

    private String resolveGrantablePermissionActionCode(String moduleAlias, String actionCode) {
        String validModuleAlias = requireModuleAlias(moduleAlias);
        String requestedActionCode = requireActionCode(actionCode);
        return grantVerifier.resolveGrantablePermissionActionCode(validModuleAlias, requestedActionCode);
    }

    private String permissionActionCode(String actionCode) {
        return PlatformAction.fromCode(requireActionCode(actionCode))
                .map(action -> action.executionPolicy().permissionActionCode())
                .orElse(actionCode);
    }

    private String requireModuleAlias(String moduleAlias) {
        String valid = Preconditions.requireText(moduleAlias, "moduleAlias");
        try {
            PlatformAliasRules.requireModuleAlias(valid);
        } catch (IllegalArgumentException ex) {
            throw new PlatformException("invalid moduleAlias: " + valid);
        }
        return valid;
    }

    private String requireActionCode(String actionCode) {
        return Preconditions.requireText(actionCode, "actionCode");
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRoleIdCsv(String value) {
        return String.join(",", parseRoleIds(value));
    }

    private Set<String> parseRoleIds(String value) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return ids;
        }
        for (String item : value.split(",")) {
            if (item == null || item.isBlank()) {
                continue;
            }
            ids.add(item.trim());
        }
        return ids;
    }

    private void prepareChildInsert(EntityContract entity) {
        String tenantId = requireActiveTenantMutationContext();
        entity.setTenantId(tenantId);
        EntityLifecycle.prepareInsert(entity, Instant.now());
    }

    private void prepareRoleActionInsert(Role role, EntityContract entity) {
        if (role != null && role.getOwnerScopeType() == RoleOwnerScopeType.PLATFORM) {
            requirePlatformRoleSystemContext();
            entity.setTenantId(null);
            EntityLifecycle.prepareInsert(entity, Instant.now());
            return;
        }
        prepareChildInsert(entity);
    }

    private void prepareChildUpdate(EntityContract entity) {
        EntityLifecycle.prepareUpdate(entity, Instant.now());
    }

    public record ActionGrantCommand(String moduleAlias,
                                     String actionCode,
                                     DataScopePolicy dataScopePolicy,
                                     TenantScopePolicy tenantScopePolicy,
                                     String scopeCondition,
                                     String referenceFieldId,
                                     String referenceActionCode) {
    }

    public record ActionRevokeCommand(String moduleAlias, String actionCode) {
    }

    private record GrantResult(String grantId, boolean created) {
    }
}
