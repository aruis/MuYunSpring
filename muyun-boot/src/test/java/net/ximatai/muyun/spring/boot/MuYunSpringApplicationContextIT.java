package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.boot.web.endpoint.RegisteredWebEndpointCatalog;
import net.ximatai.muyun.spring.boot.web.endpoint.ResolvedWebEndpoint;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;
import net.ximatai.muyun.spring.platform.deletion.DeletionEntry;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinFacade;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinItem;
import net.ximatai.muyun.spring.platform.deletion.RestoreEntryResult;
import net.ximatai.muyun.spring.platform.deletion.RestoreReport;
import net.ximatai.muyun.spring.platform.deletion.StaticDeletionRecoveryResourceResolver;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantApplicationService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSession;
import net.ximatai.muyun.spring.iam.user.UserSessionDao;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(
        classes = MuYunSpringApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class MuYunSpringApplicationContextIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private UserSessionDao userSessionDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    @Autowired
    private StaticModuleDefinitionCatalog staticModuleDefinitionCatalog;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantApplicationService tenantApplicationService;

    @Autowired
    private StaticDeletionRecoveryResourceResolver staticDeletionRecoveryResourceResolver;

    @Autowired
    private RecycleBinFacade recycleBinFacade;

    @Autowired
    private RegisteredWebEndpointCatalog registeredWebEndpointCatalog;

    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUpRestTemplate() {
        restTemplate = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://localhost:" + port));
    }

    @DynamicPropertySource
    static void applicationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    @Test
    void shouldLoadApplicationContextWithRealDatabase() {
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().moduleAlias().equals("platform.application")))
                .extracting(endpoint -> endpoint.definition().endpointId())
                .contains("platform.application.enable.enable", "platform.application.enable.disable",
                        "platform.application.sort.sort", "platform.application.recycleBin.query",
                        "platform.application.recycleBin.restore");
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().moduleAlias().equals("platform.application")))
                .filteredOn(endpoint -> !endpoint.definition().abilityCode().equals("controller"))
                .allSatisfy(endpoint -> assertThat(endpoint.definition().source())
                        .isEqualTo(ResolvedWebEndpoint.Source.STATIC_ABILITY));
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().moduleAlias().equals("platform.application")))
                .filteredOn(endpoint -> endpoint.definition().abilityCode().equals("controller"))
                .isNotEmpty()
                .allSatisfy(endpoint -> assertThat(endpoint.definition().source())
                        .isEqualTo(ResolvedWebEndpoint.Source.STATIC_EXPLICIT));
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().moduleAlias().equals("iam.organization"))
                .filter(endpoint -> endpoint.definition().source() == ResolvedWebEndpoint.Source.STATIC_ABILITY))
                .extracting(endpoint -> endpoint.definition().endpointId())
                .contains("iam.organization.enable.enable", "iam.organization.enable.disable",
                        "iam.organization.tree.tree", "iam.organization.tree.subtree",
                        "iam.organization.tree.sort");
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().moduleAlias().equals("platform.module"))
                .filter(endpoint -> endpoint.definition().source() == ResolvedWebEndpoint.Source.STATIC_ABILITY))
                .extracting(endpoint -> endpoint.definition().action())
                .doesNotContain(net.ximatai.muyun.spring.common.platform.PlatformAction.TREE,
                        net.ximatai.muyun.spring.common.platform.PlatformAction.SORT);
        assertThat(registeredWebEndpointCatalog.endpoints().stream()
                .filter(endpoint -> endpoint.definition().abilityCode().startsWith("item.")))
                .extracting(endpoint -> endpoint.definition().executionPolicy().actionCode())
                .contains("item_enable", "item_disable", "item_tree", "item_sort");
    }

    @Test
    void shouldDeleteTenantWithItsRequiredIamApplicationThroughParentCascade() {
        String tenantId = "tenant_delete_cascade_it";
        try (TenantContext.Scope ignored = TenantContext.system("integration test tenant deletion")) {
            Tenant tenant = new Tenant();
            tenant.setAlias(tenantId);
            tenant.setTitle("Tenant delete cascade integration test");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);

            assertThat(tenantApplicationService.isApplicationOpened(tenantId, "iam")).isTrue();
            assertThat(tenantService.delete(tenantId, tenant.getVersion())).isEqualTo(1);
            assertThat(tenantApplicationService.isApplicationOpened(tenantId, "iam")).isFalse();
        }
    }

    @Test
    void shouldRegisterTenantApplicationAsStaticRecoveryResource() {
        DeletionEntry entry = new DeletionEntry();
        entry.setResourceModuleAlias(TenantApplicationService.MODULE_ALIAS);
        entry.setResourceEntityAlias("tenant_application");

        assertThat(tenantApplicationService).isInstanceOf(DeletionRecoveryAbility.class);
        assertThat(staticDeletionRecoveryResourceResolver.supports(entry)).isTrue();
        assertThat(staticDeletionRecoveryResourceResolver.resolve(entry)).contains(tenantApplicationService);
    }

    @Test
    void shouldRestoreTenantWithItsRequiredIamApplicationThroughRecycleBin() {
        String tenantId = "tenant_restore_cascade_it";
        try (TenantContext.Scope ignored = TenantContext.system("integration test tenant restoration")) {
            Tenant tenant = new Tenant();
            tenant.setAlias(tenantId);
            tenant.setTitle("Tenant restore cascade integration test");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);

            assertThat(tenantApplicationService.isApplicationOpened(tenantId, "iam")).isTrue();
            assertThat(tenantService.delete(tenantId, tenant.getVersion())).isEqualTo(1);
            assertThat(tenantService.select(tenantId)).isNull();
            assertThat(tenantApplicationService.isApplicationOpened(tenantId, "iam")).isFalse();

            RecycleBinItem<Tenant> recycleBinItem = recycleBinFacade.list(tenantService, ALL).stream()
                    .filter(item -> tenantId.equals(item.record().getId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(recycleBinItem.restorable()).isTrue();

            RestoreReport report = recycleBinFacade.restore(
                    tenantService, recycleBinItem.sourceDeleteOperationId());

            assertThat(report.entries()).hasSize(2);
            assertThat(report.entries()).allMatch(entry -> entry.status() == RestoreEntryResult.Status.RESTORED);
            assertThat(report.entries()).extracting(RestoreEntryResult::moduleAlias)
                    .containsExactlyInAnyOrder(TenantService.MODULE_ALIAS, TenantApplicationService.MODULE_ALIAS);
            assertThat(tenantService.select(tenantId)).isNotNull();
            assertThat(tenantApplicationService.isApplicationOpened(tenantId, "iam")).isTrue();
        }
    }

    @Test
    void shouldPersistAndRevokeLoginSessionWithRealDatabase() {
        assertThat(columnExists("iam_user_session", "max_expires_at")).isTrue();

        LoginResult login = userSessionService.login(
                null,
                UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                "admin123");

        List<UserSession> sessions = userSessionDao.query(Criteria.of()
                        .eq("userId", UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID),
                ALL);
        UserSession session = onlyActiveSession(sessions);
        assertThat(session.getTokenHash()).hasSize(64);
        assertThat(session.getTokenHash()).isNotEqualTo(login.token());
        assertThat(session.getIssuedAt()).isEqualTo(login.issuedAt());
        assertThat(session.getExpiresAt()).isAfter(login.issuedAt());
        assertThat(session.getMaxExpiresAt()).isAfter(session.getExpiresAt());
        assertThat(session.getLastSeenAt()).isEqualTo(login.issuedAt());
        assertThat(session.getRevokedAt()).isNull();

        assertThat(userSessionService.currentUser(login.token()))
                .contains(login.currentUser());

        userSessionService.logout(login.token());

        UserSession revoked = userSessionDao.query(Criteria.of().eq("id", session.getId()), new PageRequest(0, 1))
                .getFirst();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getRevokedReason()).isEqualTo("logout");
        assertThat(revoked.getRevokedAt()).isAfterOrEqualTo(Instant.now().minusSeconds(30));
        assertThat(userSessionService.currentUser(login.token())).isEmpty();
    }

    @Test
    void shouldLoadCurrentUserAndRestrictMenusUntilPasswordChangeThroughRealHttpLogin() {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity("/iam.auth/login",
                Map.of(
                        "username", UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                        "password", "admin123"
                ),
                JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode loginBody = login.getBody();
        assertThat(loginBody).isNotNull();
        String token = loginBody.path("token").asText();
        assertThat(token).isNotBlank();
        assertThat(loginBody.path("currentUser").path("userId").asText())
                .isEqualTo(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
        assertThat(loginBody.path("currentUser").path("system").asBoolean()).isTrue();
        assertThat(loginBody.path("currentUser").has("tenantId")).isFalse();

        HttpEntity<Void> bearerRequest = new HttpEntity<>(bearerHeaders(token));
        ResponseEntity<JsonNode> context = restTemplate.exchange(
                "/iam.auth/context", HttpMethod.GET, bearerRequest, JsonNode.class);
        assertThat(context.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(context.getBody()).isNotNull();
        assertThat(context.getBody().path("userId").asText())
                .isEqualTo(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);

        ResponseEntity<JsonNode> menus = restTemplate.exchange(
                "/platform.menu/mine", HttpMethod.GET, bearerRequest, JsonNode.class);
        assertThat(menus.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(menus.getBody()).isNotNull();
        assertThat(menus.getBody().path("code").asText()).isEqualTo("PASSWORD_CHANGE_REQUIRED");
        assertThat(menus.getBody().path("message").asText()).isEqualTo("password change required");

        ResponseEntity<Void> logout = restTemplate.exchange(
                "/iam.auth/logout", HttpMethod.POST, bearerRequest, Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldQueryUserListWithBoundEmployeeProjectionThroughRealDatabase() {
        String tenantId = "tenant_projection_it";
        seedUserEmployeeProjectionRecords(tenantId);
        assertThat(staticModuleDefinitionCatalog.find(UserAccountService.MODULE_ALIAS))
                .get()
                .satisfies(definition -> {
                    assertThat(definition.entities()).isNotEmpty();
                    assertThat(definition.projectionJoins()).isEmpty();
                });
        assertThat(staticRecordReadProjectionService.supportsDefaultListQuery(
                UserAccountService.MODULE_ALIAS, userAccountService)).isTrue();

        WebPageResponse<?> firstPage = staticRecordReadProjectionService.queryDefaultList(
                UserAccountService.MODULE_ALIAS,
                Criteria.of().eq("tenantId", tenantId).eq("deleted", Boolean.FALSE),
                PageRequest.of(1, 2),
                userAccountService,
                Sort.asc("username")
        ).orElseThrow();

        assertThat(firstPage.total()).isEqualTo(4);
        assertThat(firstPage.records()).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> alice = (Map<String, Object>) firstPage.records().get(0);
        assertThat(alice)
                .containsEntry("id", projectionUserId(tenantId, "alice"))
                .containsEntry("username", projectionUsername(tenantId, "alice"))
                .containsEntry("employeeNo", "E-PROJ-001")
                .containsEntry("employeeTitle", "Alice Employee")
                .containsEntry("version", 0)
                .containsEntry("deletedAt", null);
        assertThat(alice).containsEntry("passwordStatus", "ACTIVE");
        assertThat(alice).doesNotContainKeys("tenantId", "deleted", "createdAt");
        @SuppressWarnings("unchecked")
        Map<String, Object> bob = (Map<String, Object>) firstPage.records().get(1);
        assertThat(bob)
                .containsEntry("id", projectionUserId(tenantId, "bob"))
                .containsEntry("username", projectionUsername(tenantId, "bob"));
        assertThat(bob.get("employeeNo")).isNull();
        assertThat(bob.get("employeeTitle")).isNull();

        WebPageResponse<?> secondPage = staticRecordReadProjectionService.queryDefaultList(
                UserAccountService.MODULE_ALIAS,
                Criteria.of().eq("tenantId", tenantId).eq("deleted", Boolean.FALSE),
                PageRequest.of(2, 2),
                userAccountService,
                Sort.asc("username")
        ).orElseThrow();

        assertThat(secondPage.total()).isEqualTo(4);
        assertThat(secondPage.records()).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> charlie = (Map<String, Object>) secondPage.records().get(0);
        assertThat(charlie).containsEntry("username", projectionUsername(tenantId, "charlie"));
        assertThat(charlie.get("employeeNo")).isNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> dave = (Map<String, Object>) secondPage.records().get(1);
        assertThat(dave).containsEntry("username", projectionUsername(tenantId, "dave"));
        assertThat(dave.get("employeeNo")).isNull();

        WebPageResponse<?> sortedByEmployeeTitle = staticRecordReadProjectionService.queryDefaultList(
                UserAccountService.MODULE_ALIAS,
                Criteria.of().eq("tenantId", tenantId).eq("deleted", Boolean.FALSE),
                PageRequest.of(1, 4),
                userAccountService,
                Sort.asc("employeeTitle")
        ).orElseThrow();

        assertThat(sortedByEmployeeTitle.records()).hasSize(4);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstByEmployeeTitle = (Map<String, Object>) sortedByEmployeeTitle.records().getFirst();
        assertThat(firstByEmployeeTitle)
                .containsEntry("username", projectionUsername(tenantId, "alice"))
                .containsEntry("employeeTitle", "Alice Employee");

        WebPageResponse<?> filteredByEmployeeNo = staticRecordReadProjectionService.queryDefaultList(
                UserAccountService.MODULE_ALIAS,
                Criteria.of()
                        .eq("tenantId", tenantId)
                        .eq("deleted", Boolean.FALSE)
                        .eq("employeeNo", "E-PROJ-001"),
                PageRequest.of(1, 20),
                userAccountService,
                Sort.asc("username")
        ).orElseThrow();

        assertThat(filteredByEmployeeNo.records()).singleElement()
                .satisfies(record -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> output = (Map<String, Object>) record;
                    assertThat(output)
                            .containsEntry("username", projectionUsername(tenantId, "alice"))
                            .containsEntry("employeeNo", "E-PROJ-001");
                });
        assertThatThrownBy(() -> staticRecordReadProjectionService.queryDefaultList(
                UserAccountService.MODULE_ALIAS,
                Criteria.of()
                        .eq("tenantId", tenantId)
                        .eq("deleted", Boolean.FALSE)
                        .eq("employeeTitle", "Alice Employee"),
                PageRequest.of(1, 20),
                userAccountService
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projection query field is not projected: employeeTitle");

        String token = issueSuperAdminSessionToken();
        HttpHeaders headers = bearerHeaders(token);
        WebQueryRequest httpFilterRequest = new WebQueryRequest(
                new WebPageRequest(1, 20),
                List.of(
                        new WebQueryCondition("tenantId", "EQ", List.of(tenantId)),
                        new WebQueryCondition("employeeNo", "EQ", List.of("E-PROJ-001"))
                ),
                List.of(new WebSort("employeeTitle", false))
        );
        ResponseEntity<JsonNode> httpFiltered = restTemplate.exchange(
                "/iam.user/query", HttpMethod.POST, new HttpEntity<>(httpFilterRequest, headers), JsonNode.class);

        assertThat(httpFiltered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(httpFiltered.getBody()).isNotNull();
        assertThat(httpFiltered.getBody().path("records")).hasSize(1);
        JsonNode httpAlice = httpFiltered.getBody().path("records").get(0);
        assertThat(httpAlice.path("username").asText()).isEqualTo(projectionUsername(tenantId, "alice"));
        assertThat(httpAlice.path("employeeNo").asText()).isEqualTo("E-PROJ-001");
        assertThat(httpAlice.path("employeeTitle").asText()).isEqualTo("Alice Employee");

        ResponseEntity<JsonNode> querySchema = restTemplate.exchange(
                "/iam.user/query/schema", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(querySchema.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(querySchema.getBody()).isNotNull();
        JsonNode schemaFields = querySchema.getBody().path("fields");
        JsonNode employeeNoSchema = fieldSchema(schemaFields, "employeeNo");
        assertThat(employeeNoSchema.path("operators")).isNotEmpty();
        assertThat(employeeNoSchema.path("sortable").asBoolean()).isTrue();
        JsonNode employeeTitleSchema = fieldSchema(schemaFields, "employeeTitle");
        assertThat(employeeTitleSchema.path("operators")).isEmpty();
        assertThat(employeeTitleSchema.path("sortable").asBoolean()).isTrue();

        WebPageResponse<?> employeePage = staticRecordReadProjectionService.queryDefaultList(
                EmployeeService.MODULE_ALIAS,
                Criteria.of().eq("tenantId", tenantId).eq("deleted", Boolean.FALSE),
                PageRequest.of(1, 20),
                employeeService,
                Sort.asc("employeeNo")
        ).orElseThrow();

        assertThat(employeePage.records()).hasSize(2);
        @SuppressWarnings("unchecked")
        Map<String, Object> employeeAlice = (Map<String, Object>) employeePage.records().get(0);
        assertThat(employeeAlice)
                .containsEntry("employeeNo", "E-PROJ-001")
                .containsEntry("username", projectionUsername(tenantId, "alice"))
                .containsEntry("accountBound", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> employeeCharlie = (Map<String, Object>) employeePage.records().get(1);
        assertThat(employeeCharlie)
                .containsEntry("employeeNo", "E-PROJ-003")
                .containsEntry("accountBound", false);
        assertThat(employeeCharlie.get("username")).isNull();

        WebPageResponse<?> unboundEmployees = staticRecordReadProjectionService.queryDefaultList(
                EmployeeService.MODULE_ALIAS,
                Criteria.of()
                        .eq("tenantId", tenantId)
                        .eq("deleted", Boolean.FALSE)
                        .eq("accountBound", Boolean.FALSE),
                PageRequest.of(1, 20),
                employeeService,
                Sort.asc("employeeNo")
        ).orElseThrow();

        assertThat(unboundEmployees.records()).singleElement()
                .satisfies(record -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> output = (Map<String, Object>) record;
                    assertThat(output)
                            .containsEntry("employeeNo", "E-PROJ-003")
                            .containsEntry("accountBound", false);
                });

        ResponseEntity<JsonNode> employeeQuerySchema = restTemplate.exchange(
                "/iam.employee/query/schema", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

        assertThat(employeeQuerySchema.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(employeeQuerySchema.getBody()).isNotNull();
        JsonNode employeeSchemaFields = employeeQuerySchema.getBody().path("fields");
        JsonNode usernameSchema = fieldSchema(employeeSchemaFields, "username");
        assertThat(usernameSchema.path("operators")).isNotEmpty();
        JsonNode accountBoundSchema = fieldSchema(employeeSchemaFields, "accountBound");
        assertThat(accountBoundSchema.path("valueType").asText()).isEqualTo("BOOLEAN");
        assertThat(accountBoundSchema.path("operators")).isNotEmpty();

        WebQueryRequest httpRejectRequest = new WebQueryRequest(
                new WebPageRequest(1, 20),
                List.of(
                        new WebQueryCondition("tenantId", "EQ", List.of(tenantId)),
                        new WebQueryCondition("employeeTitle", "EQ", List.of("Alice Employee"))
                ),
                List.of()
        );
        ResponseEntity<JsonNode> httpRejected = restTemplate.exchange(
                "/iam.user/query", HttpMethod.POST, new HttpEntity<>(httpRejectRequest, headers), JsonNode.class);

        assertThat(httpRejected.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(httpRejected.getBody()).isNotNull();
        assertThat(httpRejected.getBody().path("message").asText())
                .contains("query operator is not supported by iam.user: employeeTitle.EQ");
    }

    @Test
    void shouldQueryUserSelectorWithEmployeeOrganizationAndDepartmentProjectionThroughRealDatabase() {
        String tenantId = "tenant_selector_projection_it";
        seedUserEmployeeProjectionRecords(tenantId);
        String token = issueSuperAdminSessionToken();
        HttpHeaders headers = bearerHeaders(token);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                "/iam.user/selector/query",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "keyword", projectionUsername(tenantId, "alice"),
                        "page", Map.of("pageNum", 1, "pageSize", 20)
                ), headers),
                JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("total").asLong()).isEqualTo(1);
        JsonNode records = response.getBody().path("records");
        assertThat(records).hasSize(1);
        JsonNode alice = records.get(0);
        assertThat(alice.path("id").asText()).isEqualTo(projectionUserId(tenantId, "alice"));
        assertThat(alice.path("username").asText()).isEqualTo(projectionUsername(tenantId, "alice"));
        assertThat(alice.path("employeeId").asText()).isEqualTo(projectionEmployeeId(tenantId, "alice"));
        assertThat(alice.path("employeeNo").asText()).isEqualTo("E-PROJ-001");
        assertThat(alice.path("employeeTitle").asText()).isEqualTo("Alice Employee");
        assertThat(alice.path("organizationId").asText()).isEqualTo(projectionOrganizationId(tenantId));
        assertThat(alice.path("organizationTitle").asText()).isEqualTo("Projection Organization");
        assertThat(alice.path("departmentId").asText()).isEqualTo(projectionDepartmentId(tenantId));
        assertThat(alice.path("departmentTitle").asText()).isEqualTo("Projection Department");
    }

    private JsonNode fieldSchema(JsonNode fields, String name) {
        for (JsonNode field : fields) {
            if (name.equals(field.path("name").asText())) {
                return field;
            }
        }
        throw new AssertionError("missing query schema field: " + name);
    }

    private String issueSuperAdminSessionToken() {
        String token = "projection-http-token-" + System.nanoTime();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        jdbcTemplate.update("""
                        insert into iam_user_session (
                            id, user_id, username, token_hash, issued_at, expires_at,
                            max_expires_at, last_seen_at, password_change_required, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                "proj_http_" + Long.toUnsignedString(System.nanoTime(), 36),
                UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID,
                UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                tokenHash(token),
                Timestamp.from(now),
                Timestamp.from(now.plus(1, ChronoUnit.HOURS)),
                Timestamp.from(now.plus(1, ChronoUnit.DAYS)),
                Timestamp.from(now),
                Boolean.FALSE,
                Boolean.FALSE);
        return token;
    }

    private String tokenHash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private UserSession onlyActiveSession(List<UserSession> sessions) {
        List<UserSession> activeSessions = sessions.stream()
                .filter(session -> session.getRevokedAt() == null)
                .toList();
        assertThat(activeSessions).hasSize(1);
        return activeSessions.getFirst();
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = current_schema()
                          and table_name = ?
                          and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName);
        return count != null && count > 0;
    }

    private void seedUserEmployeeProjectionRecords(String tenantId) {
        jdbcTemplate.update("delete from iam_employee_account where tenant_id = ?", tenantId);
        jdbcTemplate.update("delete from iam_employee where tenant_id = ?", tenantId);
        jdbcTemplate.update("delete from iam_department where tenant_id = ?", tenantId);
        jdbcTemplate.update("delete from iam_organization where tenant_id = ?", tenantId);
        jdbcTemplate.update("delete from iam_user where tenant_id = ?", tenantId);
        String organizationId = projectionOrganizationId(tenantId);
        String departmentId = projectionDepartmentId(tenantId);
        insertOrganization(tenantId, organizationId, "PROJ-ORG", "Projection Organization");
        insertDepartment(tenantId, departmentId, organizationId, "PROJ-DEPT", "Projection Department");
        insertUser(tenantId, projectionUserId(tenantId, "alice"), projectionUsername(tenantId, "alice"));
        insertUser(tenantId, projectionUserId(tenantId, "bob"), projectionUsername(tenantId, "bob"));
        insertUser(tenantId, projectionUserId(tenantId, "charlie"), projectionUsername(tenantId, "charlie"));
        insertUser(tenantId, projectionUserId(tenantId, "dave"), projectionUsername(tenantId, "dave"));
        insertEmployee(tenantId, projectionEmployeeId(tenantId, "alice"), organizationId, departmentId,
                "E-PROJ-001", "Alice Employee", false);
        insertEmployee(tenantId, projectionEmployeeId(tenantId, "charlie"), organizationId, departmentId,
                "E-PROJ-003", "Charlie Employee", false);
        insertEmployee(tenantId, projectionEmployeeId(tenantId, "dave"), organizationId, departmentId,
                "E-PROJ-004", "Dave Employee", true);
        insertEmployeeAccount(tenantId, projectionEmployeeAccountId(tenantId, "alice"),
                projectionEmployeeId(tenantId, "alice"), projectionUserId(tenantId, "alice"), false);
        insertEmployeeAccount(tenantId, projectionEmployeeAccountId(tenantId, "charlie"),
                projectionEmployeeId(tenantId, "charlie"), projectionUserId(tenantId, "charlie"), true);
        insertEmployeeAccount(tenantId, projectionEmployeeAccountId(tenantId, "dave"),
                projectionEmployeeId(tenantId, "dave"), projectionUserId(tenantId, "dave"), false);
    }

    private void insertUser(String tenantId, String id, String username) {
        jdbcTemplate.update("""
                        insert into iam_user (
                            id, tenant_id, title, username, password_hash, password_status,
                            enabled, deleted, auth_user_id, auth_module_alias
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, username, username, "test-password-hash", "ACTIVE",
                Boolean.TRUE, Boolean.FALSE, id, UserAccountService.MODULE_ALIAS);
    }

    private void insertOrganization(String tenantId, String id, String code, String title) {
        jdbcTemplate.update("""
                        insert into iam_organization (
                            id, tenant_id, title, code, enabled, deleted
                        ) values (?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, title, code, Boolean.TRUE, Boolean.FALSE);
    }

    private void insertDepartment(String tenantId, String id, String organizationId, String code, String title) {
        jdbcTemplate.update("""
                        insert into iam_department (
                            id, tenant_id, title, organization_id, code, enabled, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, title, organizationId, code, Boolean.TRUE, Boolean.FALSE);
    }

    private String projectionOrganizationId(String tenantId) {
        return projectionId("org", tenantId, "main");
    }

    private String projectionDepartmentId(String tenantId) {
        return projectionId("dept", tenantId, "main");
    }

    private String projectionUserId(String tenantId, String code) {
        return projectionId("user", tenantId, code);
    }

    private String projectionEmployeeId(String tenantId, String code) {
        return projectionId("emp", tenantId, code);
    }

    private String projectionEmployeeAccountId(String tenantId, String code) {
        return projectionId("bind", tenantId, code);
    }

    private String projectionUsername(String tenantId, String code) {
        return code + "_projection_" + projectionTenantSuffix(tenantId);
    }

    private String projectionId(String prefix, String tenantId, String code) {
        return "projection_" + prefix + "_" + projectionTenantSuffix(tenantId) + "_" + code;
    }

    private String projectionTenantSuffix(String tenantId) {
        return Integer.toUnsignedString(tenantId.hashCode(), 36);
    }

    private void insertEmployee(String tenantId,
                                String id,
                                String organizationId,
                                String departmentId,
                                String employeeNo,
                                String title,
                                boolean deleted) {
        jdbcTemplate.update("""
                        insert into iam_employee (
                            id, tenant_id, title, organization_id, department_id, employee_no,
                            enabled, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, title, organizationId, departmentId, employeeNo,
                Boolean.TRUE, deleted);
    }

    private void insertEmployeeAccount(String tenantId, String id, String employeeId, String userId, boolean deleted) {
        jdbcTemplate.update("""
                        insert into iam_employee_account (
                            id, tenant_id, employee_id, user_id, deleted
                        ) values (?, ?, ?, ?, ?)
                        """,
                id, tenantId, employeeId, userId, deleted);
    }
}
