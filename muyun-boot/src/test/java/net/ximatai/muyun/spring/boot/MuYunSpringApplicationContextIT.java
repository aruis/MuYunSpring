package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSession;
import net.ximatai.muyun.spring.iam.user.UserSessionDao;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
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
    private TestRestTemplate restTemplate;

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
                TenantService.PLATFORM_TENANT_ID,
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
    void shouldLoadCurrentUserAndMenusThroughRealHttpLogin() {
        ResponseEntity<JsonNode> login = restTemplate.postForEntity("/iam.auth/login",
                Map.of(
                        "tenantId", TenantService.PLATFORM_TENANT_ID,
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

        HttpEntity<Void> bearerRequest = new HttpEntity<>(bearerHeaders(token));
        ResponseEntity<JsonNode> context = restTemplate.exchange(
                "/iam.auth/context", HttpMethod.GET, bearerRequest, JsonNode.class);
        assertThat(context.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(context.getBody()).isNotNull();
        assertThat(context.getBody().path("userId").asText())
                .isEqualTo(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);

        ResponseEntity<JsonNode> menus = restTemplate.exchange(
                "/platform.menu/mine", HttpMethod.GET, bearerRequest, JsonNode.class);
        assertThat(menus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(menus.getBody()).isNotNull();
        assertThat(menus.getBody().path("records").isArray()).isTrue();
        assertThat(menus.getBody().path("records")).isNotEmpty();

        ResponseEntity<Void> logout = restTemplate.exchange(
                "/iam.auth/logout", HttpMethod.POST, bearerRequest, Void.class);
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);
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
}
