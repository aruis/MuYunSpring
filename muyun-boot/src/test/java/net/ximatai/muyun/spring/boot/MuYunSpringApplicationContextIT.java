package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSession;
import net.ximatai.muyun.spring.iam.user.UserSessionDao;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
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

import java.time.Instant;
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
        assertThat(menus.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(menus.getBody()).isNotNull();
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
                .containsEntry("id", "projection_user_alice")
                .containsEntry("username", "alice_projection")
                .containsEntry("employeeNo", "E-PROJ-001")
                .containsEntry("employeeTitle", "Alice Employee");
        assertThat(alice).containsEntry("passwordStatus", "ACTIVE");
        assertThat(alice).doesNotContainKeys("tenantId", "version", "deleted", "createdAt");
        @SuppressWarnings("unchecked")
        Map<String, Object> bob = (Map<String, Object>) firstPage.records().get(1);
        assertThat(bob)
                .containsEntry("id", "projection_user_bob")
                .containsEntry("username", "bob_projection");
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
        assertThat(charlie).containsEntry("username", "charlie_projection");
        assertThat(charlie.get("employeeNo")).isNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> dave = (Map<String, Object>) secondPage.records().get(1);
        assertThat(dave).containsEntry("username", "dave_projection");
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
                .containsEntry("username", "alice_projection")
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
                            .containsEntry("username", "alice_projection")
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
        jdbcTemplate.update("delete from iam_user where tenant_id = ?", tenantId);
        insertUser(tenantId, "projection_user_alice", "alice_projection");
        insertUser(tenantId, "projection_user_bob", "bob_projection");
        insertUser(tenantId, "projection_user_charlie", "charlie_projection");
        insertUser(tenantId, "projection_user_dave", "dave_projection");
        insertEmployee(tenantId, "projection_emp_alice", "E-PROJ-001", "Alice Employee", false);
        insertEmployee(tenantId, "projection_emp_charlie", "E-PROJ-003", "Charlie Employee", false);
        insertEmployee(tenantId, "projection_emp_dave", "E-PROJ-004", "Dave Employee", true);
        insertEmployeeAccount(tenantId, "projection_bind_alice", "projection_emp_alice",
                "projection_user_alice", false);
        insertEmployeeAccount(tenantId, "projection_bind_charlie", "projection_emp_charlie",
                "projection_user_charlie", true);
        insertEmployeeAccount(tenantId, "projection_bind_dave", "projection_emp_dave",
                "projection_user_dave", false);
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

    private void insertEmployee(String tenantId, String id, String employeeNo, String title, boolean deleted) {
        jdbcTemplate.update("""
                        insert into iam_employee (
                            id, tenant_id, title, organization_id, department_id, employee_no,
                            enabled, deleted
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, tenantId, title, "projection_org", "projection_dept", employeeNo,
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
