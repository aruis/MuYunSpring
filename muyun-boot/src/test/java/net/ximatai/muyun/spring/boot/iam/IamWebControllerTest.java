package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryCriteria;
import net.ximatai.muyun.spring.boot.web.WebQueryGroupOperator;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionDao;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionCategoryDao;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import net.ximatai.muyun.spring.iam.position.PositionDao;
import net.ximatai.muyun.spring.iam.position.PositionService;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrant;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrant;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleKind;
import net.ximatai.muyun.spring.iam.role.RolePermissionAction;
import net.ximatai.muyun.spring.iam.role.RolePermissionMatrix;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.PasswordHashingService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountDao;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuOpenMode;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IamWebControllerTest {
    private static final String TENANT = "tenant_a";

    private TenantDao tenantDao;
    private OrganizationDao organizationDao;
    private PositionDao positionDao;
    private PositionCategoryDao positionCategoryDao;
    private EmployeePositionDao employeePositionDao;
    private UserAccountDao userAccountDao;
    private RoleService roleService;
    private RoleGrantableActionResolver grantableActionResolver;
    private TenantWebController tenantController;
    private OrganizationWebController organizationController;
    private PositionWebController positionController;
    private UserAccountWebController userAccountController;
    private RoleWebController roleController;

    @BeforeEach
    void setUp() {
        tenantDao = mock(TenantDao.class);
        organizationDao = mock(OrganizationDao.class);
        positionDao = mock(PositionDao.class);
        positionCategoryDao = mock(PositionCategoryDao.class);
        employeePositionDao = mock(EmployeePositionDao.class);
        userAccountDao = mock(UserAccountDao.class);
        roleService = mock(RoleService.class);
        grantableActionResolver = mock(RoleGrantableActionResolver.class);

        TenantService tenantService = new TenantService(tenantDao);
        OrganizationService organizationService = new OrganizationService(organizationDao, tenantService);
        PositionCategoryService positionCategoryService = new PositionCategoryService(
                positionCategoryDao, tenantService, positionDao);
        PositionService positionService = new PositionService(positionDao, tenantService, positionCategoryService,
                employeePositionDao);
        UserAccountService userAccountService = new UserAccountService(
                userAccountDao, tenantService, new PasswordHashingService());

        tenantController = setService(new TenantWebController(), tenantService);
        organizationController = setService(new OrganizationWebController(), organizationService);
        positionController = setService(new PositionWebController(), positionService);
        userAccountController = setService(new UserAccountWebController(null), userAccountService);
        roleController = setService(new RoleWebController(grantableActionResolver), roleService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldQueryAndCreateTenantThroughSystemManagedWebContract() {
        when(tenantDao.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(tenant(TENANT, "Tenant A")), 1, PageRequest.of(1, 20)));
        when(tenantDao.insert(any())).thenAnswer(invocation -> {
            assertThat(TenantContext.isSystem()).isTrue();
            return invocation.<Tenant>getArgument(0).getAlias();
        });
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(tenant("tenant_b", "Tenant B")));

        WebPageResponse<Tenant> page = tenantController.query(null);
        WebRecordResponse<Tenant> created = tenantController.insert(tenant("tenant_b", "Tenant B"));

        assertThat(page.records().getFirst().getAlias()).isEqualTo(TENANT);
        assertThat(page.pageNum()).isEqualTo(1);
        assertThat(created.record().getAlias()).isEqualTo("tenant_b");
    }

    @Test
    void shouldRejectUnsupportedStaticQuerySurfacesInsteadOfIgnoringThem() {
        assertThatThrownBy(() -> tenantController.query(new WebQueryRequest(
                null,
                List.of(new WebQueryCondition("title", "EQ", List.of("Tenant A"))),
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query conditions are not supported by iam.tenant");

        assertThatThrownBy(() -> tenantController.query(new WebQueryRequest(
                null,
                null,
                List.of(),
                new WebQueryCriteria(WebQueryGroupOperator.OR,
                        List.of(new WebQueryCondition("title", "EQ", List.of("Tenant A"))), List.of()),
                java.util.Map.of(),
                List.of(),
                null,
                null,
                java.util.Map.of(),
                null,
                null,
                List.of(),
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query criteria are not supported by iam.tenant");
    }

    @Test
    void shouldQueryPositionsByCategoryConditionAndSupportUnpagedQuery() {
        tenantScope();
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant(TENANT, "Tenant A")));
        when(positionDao.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(position("pos-1", "category-1", "DEV", "Developer")), 1,
                        PageRequest.of(1, 20)));
        when(positionDao.list(any(Criteria.class), any(Sort[].class)))
                .thenReturn(List.of(position("pos-2", "category-1", "QA", "Tester")));

        WebQueryRequest filtered = new WebQueryRequest(null,
                List.of(new WebQueryCondition("categoryId", "EQ", List.of("category-1"))), List.of());
        WebPageResponse<Position> page = positionController.query(filtered);
        WebPageResponse<Position> unpaged = positionController.query(new WebQueryRequest(
                null, true, filtered.conditions(), null, java.util.Map.of(), List.of(),
                null, null, java.util.Map.of(), null, null, List.of(), null));

        assertThat(page.records().getFirst().getId()).isEqualTo("pos-1");
        assertThat(unpaged.records().getFirst().getId()).isEqualTo("pos-2");
        ArgumentCaptor<Criteria> criteriaCaptor = ArgumentCaptor.captor();
        verify(positionDao).pageQuery(criteriaCaptor.capture(), any(PageRequest.class), any(Sort[].class));
        assertThat(containsCondition(criteriaCaptor.getValue(), "categoryId", "category-1")).isTrue();
    }

    @Test
    void shouldExposeOrganizationTreeAndCreateUnderTenantScope() {
        tenantScope();
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant(TENANT, "Tenant A")));
        Organization root = organization("org-1", "HQ", "Headquarters");
        root.setTenantId(TENANT);
        root.setParentId(TreeAbility.ROOT_ID);
        Organization child = organization("org-2", "BR", "Branch");
        child.setTenantId(TENANT);
        child.setParentId("org-1");
        when(organizationDao.count(any(Criteria.class))).thenReturn(1L);
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(root), List.of(child));
        when(organizationDao.list(any(Criteria.class), any()))
                .thenReturn(List.of(root), List.of(child), List.of());
        when(organizationDao.insert(any())).thenAnswer(invocation -> {
            Organization incoming = invocation.getArgument(0);
            assertThat(TenantContext.currentTenantId()).contains(TENANT);
            assertThat(incoming.getTenantId()).isEqualTo(TENANT);
            return "org-3";
        });
        when(organizationDao.query(any(Criteria.class), any(PageRequest.class)))
                .thenReturn(List.of(root), List.of(child), List.of(organization("org-3", "NEW", "New Org")));

        WebListResponse<?> tree = organizationController.tree(false);
        WebRecordResponse<Organization> created = organizationController.insert(organization(null, "NEW", "New Org"));

        assertThat(tree.records()).hasSize(1);
        assertThat(created.record().getId()).isEqualTo("org-3");
    }

    @Test
    void shouldRequireActiveTenantForOrganizationAccess() {
        tenantScope();
        doThrow(new PlatformException("Tenant is not active: " + TENANT))
                .when(tenantDao).query(any(Criteria.class), any(PageRequest.class));

        assertThatThrownBy(() -> organizationController.tree(false))
                .isInstanceOf(PlatformException.class)
                .hasMessage("Tenant is not active: " + TENANT);

        TenantContext.clear();
        assertThatThrownBy(() -> organizationController.tree(false))
                .isInstanceOf(PlatformException.class)
                .hasMessage("iam.organization requires tenant context");
    }

    @Test
    void shouldCreateAndUpdateUserWithoutExposingPasswordMaterial() {
        tenantScope();
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(tenant(TENANT, "Tenant A")));
        when(userAccountDao.insert(any())).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            assertThat(user.getTenantId()).isEqualTo(TENANT);
            assertThat(user.getPasswordHash()).startsWith("pbkdf2$");
            assertThat(user.getPasswordHash()).isNotEqualTo("client-supplied-hash");
            return "user-1";
        });
        UserAccount saved = user("user-1", "alice", "Alice");
        when(userAccountDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(), List.of(saved));

        UserAccount input = user(null, "alice", "Alice");
        input.setPasswordHash("client-supplied-hash");
        input.setPassword("secret2");
        WebRecordResponse<UserAccount> created = userAccountController.insert(input);

        assertThat(created.record().getId()).isEqualTo("user-1");
        assertThat(created.record().getPasswordHash()).isNull();
        assertThat(created.record().getPassword()).isNull();

        UserAccount existing = user("user-1", "alice", "Alice");
        existing.setTenantId(TENANT);
        existing.setVersion(3);
        existing.setPasswordHash("pbkdf2$existing-hash");
        when(userAccountDao.count(any(Criteria.class))).thenReturn(1L);
        when(userAccountDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(existing));
        when(userAccountDao.updateByIdAndVersion(any(UserAccount.class), any())).thenAnswer(invocation -> {
            UserAccount updated = invocation.getArgument(0);
            assertThat(updated.getPasswordHash()).isEqualTo("pbkdf2$existing-hash");
            return 1;
        });

        UserAccount update = user(null, "alice", "Alice Updated");
        update.setPasswordHash("client-supplied-hash");
        update.setPassword("new-plain-password");
        WebRecordResponse<UserAccount> updated = userAccountController.update("user-1", update);

        assertThat(updated.record().getPasswordHash()).isEqualTo("pbkdf2$existing-hash");
        assertThat(updated.record().getPassword()).isNull();
    }

    @Test
    void shouldCreateRoleWithCodeTitleEnumAndDisableTenantThroughSystemContext() {
        tenantScope();
        Role saved = new Role();
        saved.setId("role-1");
        saved.setTitle("Data Grant Role");
        saved.setRoleKind(RoleKind.DATA_GRANT);
        when(roleService.insert(any())).thenAnswer(invocation -> {
            Role incoming = invocation.getArgument(0);
            assertThat(incoming.getRoleKind()).isEqualTo(RoleKind.DATA_GRANT);
            return "role-1";
        });
        when(roleService.select("role-1")).thenReturn(saved);
        WebRecordResponse<Role> role = roleController.insert(saved);
        assertThat(role.record().getRoleKind()).isEqualTo(RoleKind.DATA_GRANT);

        Tenant existing = tenant(TENANT, "Tenant A");
        existing.setVersion(2);
        when(tenantDao.query(any(Criteria.class), any(PageRequest.class))).thenReturn(List.of(existing));
        when(tenantDao.updateByIdAndVersion(any(Tenant.class), any())).thenAnswer(invocation -> {
            assertThat(TenantContext.isSystem()).isTrue();
            return 1;
        });
        WebCountResponse disabled = tenantController.disable(TENANT);
        assertThat(disabled.count()).isEqualTo(1);
    }

    @Test
    void shouldExposeRoleGrantAndPermissionMatrixEndpoints() {
        tenantScope();
        AccountRoleGrant accountGrant = accountRoleGrant("grant-1", "role-1", "user-2",
                ManagementScopeType.TENANT, TENANT);
        EmploymentRoleGrant employmentGrant = employmentRoleGrant("grant-2", "role-2", "position-1");
        when(roleService.grantAccountRole("role-1", "user-2", ManagementScopeType.TENANT, TENANT))
                .thenReturn("grant-1");
        when(roleService.accountRoleGrants("role-1")).thenReturn(List.of(accountGrant));
        when(roleService.deleteAccountRoleGrant("role-1", "grant-1")).thenReturn(1);
        when(roleService.grantEmploymentRole("role-2", "position-1")).thenReturn("grant-2");
        when(roleService.employmentRoleGrants("role-2")).thenReturn(List.of(employmentGrant));
        when(roleService.deleteEmploymentRoleGrant("role-2", "grant-2")).thenReturn(1);
        when(roleService.grantAction("role-1", "sales.contract", "query",
                DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT,
                null, null, null)).thenReturn(1);
        when(roleService.revokeAction("role-1", "sales.contract", "query")).thenReturn(1);

        assertThat(roleController.grantAccountRole("role-1",
                new RoleWebController.AccountRoleGrantRequest("user-2", ManagementScopeType.TENANT, TENANT)))
                .isEqualTo("grant-1");
        assertThat(roleController.accountRoleGrants("role-1").getFirst().getUserId()).isEqualTo("user-2");
        assertThat(roleController.deleteAccountRoleGrant("role-1", "grant-1").count()).isEqualTo(1);
        assertThat(roleController.grantEmploymentRole("role-2",
                new RoleWebController.EmploymentRoleGrantRequest("position-1"))).isEqualTo("grant-2");
        assertThat(roleController.employmentRoleGrants("role-2").getFirst().getEmployeePositionId())
                .isEqualTo("position-1");
        assertThat(roleController.deleteEmploymentRoleGrant("role-2", "grant-2").count()).isEqualTo(1);
        assertThat(roleController.grantAction("role-1", new RoleWebController.GrantActionRequest(
                "sales.contract", "query", DataScopePolicy.OWNER, TenantScopePolicy.CURRENT_TENANT,
                null, null, null)).count()).isEqualTo(1);
        assertThat(roleController.revokeAction("role-1",
                new RoleWebController.RevokeActionRequest("sales.contract", "query")).count()).isEqualTo(1);

        when(roleService.grantActions(any(), any())).thenReturn(2);
        when(roleService.revokeActions(any(), any())).thenReturn(1);
        assertThat(roleController.grantActions("role-1", new RoleWebController.GrantActionsRequest(List.of(
                new RoleWebController.GrantActionRequest("sales.contract", "query", null, null, null, null, null),
                new RoleWebController.GrantActionRequest("sales.order", "menu", null, null, null, null, null)
        ))).count()).isEqualTo(2);
        assertThat(roleController.revokeActions("role-1", new RoleWebController.RevokeActionsRequest(List.of(
                new RoleWebController.RevokeActionRequest("sales.contract", "query")
        ))).count()).isEqualTo(1);
    }

    @Test
    void shouldExposeRolePermissionAndMenuMatrix() {
        tenantScope();
        List<GrantableAction> grantableActions = List.of(
                new GrantableAction("sales.contract", "query", "view", "Query", true, true));
        when(grantableActionResolver.resolve(List.of("sales.contract"))).thenReturn(grantableActions);
        when(roleService.permissionMatrix("role-1", grantableActions)).thenReturn(new RolePermissionMatrix(
                "role-1",
                List.of(new RolePermissionMatrix.Module(
                        "sales.contract",
                        List.of(new RolePermissionAction("sales.contract", "query", "view", "Query",
                                true, true, true, DataScopePolicy.OWNER,
                                TenantScopePolicy.CURRENT_TENANT, null, null, null))
                ))
        ));
        RolePermissionMatrix matrix = roleController.permissionMatrix("role-1",
                new RoleWebController.PermissionMatrixRequest(List.of("sales.contract")));
        assertThat(matrix.modules().getFirst().actions().getFirst().granted()).isTrue();

        MenuService menuService = mock(MenuService.class);
        RoleWebController menuController = setService(new RoleWebController(grantableActionResolver, provider(menuService)),
                roleService);
        Menu group = menu("group-1", "scheme-1", null);
        Menu contract = menu("menu-1", "scheme-1", "sales.contract");
        when(menuService.rootMenus("scheme-1")).thenReturn(List.of(group));
        when(menuService.children("scheme-1", "group-1")).thenReturn(List.of(contract));
        when(menuService.children("scheme-1", "menu-1")).thenReturn(List.of());
        when(roleService.permissionMatrix(any(), any())).thenReturn(new RolePermissionMatrix(
                "role-1",
                List.of(new RolePermissionMatrix.Module(
                        "sales.contract",
                        List.of(new RolePermissionAction("sales.contract", "menu", "menu", "Menu",
                                true, false, true, DataScopePolicy.NONE,
                                TenantScopePolicy.CURRENT_TENANT, null, null, null))
                ))
        ));

        WebListResponse<RoleWebController.RoleMenuNode> menuMatrix = menuController.menuMatrix("role-1", "scheme-1");
        assertThat(menuMatrix.records().getFirst().children().getFirst().granted()).isTrue();
    }

    @Test
    void shouldExposeUserSelectorQuery() {
        tenantScope();
        RoleService selectorRoleService = mock(RoleService.class);
        RecordingUserAccountService userAccountService = new RecordingUserAccountService();
        UserAccountWebController controller = setService(
                new UserAccountWebController(null, provider(selectorRoleService)), userAccountService);
        UserAccount alice = user("user-2", "alice", "Alice");
        alice.setOrganizationId("org-1");
        when(selectorRoleService.userIds("role-1")).thenReturn(List.of("user-2"));
        userAccountService.result = PageResult.of(List.of(alice), 1, PageRequest.of(1, 20));

        WebPageResponse<UserAccountWebController.UserSelectorItem> response = controller.selector(
                new UserAccountWebController.UserSelectorRequest("org-1", "role-1", "ali", null, null));

        assertThat(response.records().getFirst().username()).isEqualTo("alice");
        verify(selectorRoleService).userIds("role-1");
        assertThat(userAccountService.scopedPolicies)
                .extracting(ActionExecutionPolicy::actionCode)
                .containsExactly("userSelector");
        assertThat(userAccountService.scopedPolicies.getFirst().requiresDataScope()).isTrue();
        assertThat(userAccountService.queriedCriteria).isSameAs(userAccountService.scopedCriteria);
        assertThat(containsCondition(userAccountService.baseCriteria, "enabled", Boolean.TRUE)).isTrue();
    }

    private void tenantScope() {
        TenantContext.setTenantId(TENANT);
    }

    private Tenant tenant(String alias, String title) {
        Tenant tenant = new Tenant();
        tenant.setAlias(alias);
        tenant.setTitle(title);
        tenant.setEnabled(Boolean.TRUE);
        tenant.setSortOrder(1);
        return tenant;
    }

    private Organization organization(String id, String code, String title) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setCode(code);
        organization.setTitle(title);
        organization.setEnabled(Boolean.TRUE);
        organization.setSortOrder(1);
        return organization;
    }

    private UserAccount user(String id, String username, String title) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername(username);
        user.setTitle(title);
        user.setEnabled(Boolean.TRUE);
        user.setSortOrder(1);
        return user;
    }

    private Position position(String id, String categoryId, String code, String title) {
        Position position = new Position();
        position.setId(id);
        position.setCategoryId(categoryId);
        position.setCode(code);
        position.setTitle(title);
        position.setEnabled(Boolean.TRUE);
        position.setSortOrder(1);
        return position;
    }

    private AccountRoleGrant accountRoleGrant(String id,
                                              String roleId,
                                              String userId,
                                              ManagementScopeType scopeType,
                                              String scopeId) {
        AccountRoleGrant grant = new AccountRoleGrant();
        grant.setId(id);
        grant.setRoleId(roleId);
        grant.setUserId(userId);
        grant.setManagementScopeType(scopeType);
        grant.setManagementScopeId(scopeId);
        grant.setEnabled(Boolean.TRUE);
        return grant;
    }

    private EmploymentRoleGrant employmentRoleGrant(String id, String roleId, String employeePositionId) {
        EmploymentRoleGrant grant = new EmploymentRoleGrant();
        grant.setId(id);
        grant.setRoleId(roleId);
        grant.setEmployeePositionId(employeePositionId);
        grant.setEnabled(Boolean.TRUE);
        return grant;
    }

    private Menu menu(String id, String schemeId, String moduleAlias) {
        Menu menu = new Menu();
        menu.setId(id);
        menu.setSchemeId(schemeId);
        if (moduleAlias != null && !moduleAlias.isBlank()) {
            menu.setOpenMode(MenuOpenMode.TAB);
        }
        menu.setModuleAlias(moduleAlias);
        menu.setTitle(id);
        menu.setEnabled(Boolean.TRUE);
        return menu;
    }

    private <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return value;
            }
        };
    }

    private <C> C setService(C controller, Object service) {
        try {
            Field field = net.ximatai.muyun.spring.boot.web.WebSupport.class.getDeclaredField("service");
            field.setAccessible(true);
            field.set(controller, service);
            return controller;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Cannot inject test service", ex);
        }
    }

    private boolean containsCondition(Criteria criteria, String fieldName, Object value) {
        return criteria.getClauses().stream()
                .anyMatch(clause -> fieldName.equals(clause.getField())
                        && clause.getValues().contains(value));
    }

    private static final class RecordingUserAccountService extends UserAccountService {
        private final List<ActionExecutionPolicy> scopedPolicies = new java.util.ArrayList<>();
        private Criteria baseCriteria;
        private Criteria scopedCriteria;
        private Criteria queriedCriteria;
        private PageResult<UserAccount> result = PageResult.of(List.of(), 0, PageRequest.of(1, 20));

        private RecordingUserAccountService() {
            super(mock(UserAccountDao.class), mock(ActiveTenantVerifier.class), new PasswordHashingService());
        }

        @Override
        public DataScopeCriteriaResult readScopeByPolicy(ActionExecutionPolicy policy, Criteria criteria) {
            scopedPolicies.add(policy);
            baseCriteria = criteria;
            scopedCriteria = Criteria.of().eq("authUserId", "user-1");
            scopedCriteria.andGroup(criteria.getRoot());
            return DataScopeCriteriaResult.restricted(scopedCriteria);
        }

        @Override
        public PageResult<UserAccount> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            queriedCriteria = criteria;
            return result;
        }
    }
}
