package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.platform.PostgresQuarkusTestResource;
import net.ximatai.muyun.spring.iam.user.LoginResult;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSession;
import net.ximatai.muyun.spring.iam.user.UserSessionDao;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(MuYunSpringApplicationContextIT.PostgresProfile.class)
@QuarkusTestResource(value = PostgresQuarkusTestResource.class, restrictToAnnotatedClass = true)
class MuYunSpringApplicationContextIT {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    @Inject
    Config config;

    @Inject
    UserSessionService userSessionService;

    @Inject
    UserSessionDao userSessionDao;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @TestHTTPResource
    URI baseUri;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        assumeTrue(
                config.getOptionalValue("muyun.test.postgres.enabled", Boolean.class).orElse(false),
                "PostgreSQL integration test is disabled; run with -Pmuyun.postgres.it.required=true to enable it"
        );
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    void shouldLoadApplicationContextWithRealDatabase() {
    }

    @Test
    void shouldPersistAndRevokeLoginSessionWithRealDatabase() throws Exception {
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
    void shouldLoadCurrentUserAndMenusThroughRealHttpLogin() throws Exception {
        HttpResponse<String> login = post("/iam.auth/login", Map.of(
                "username", UserAccountService.PLATFORM_SUPER_ADMIN_USERNAME,
                "password", "admin123"
        ), null);
        assertThat(login.statusCode()).isEqualTo(200);
        JsonNode loginBody = objectMapper.readTree(login.body());
        assertThat(loginBody).isNotNull();
        String token = loginBody.path("token").asText();
        assertThat(token).isNotBlank();
        assertThat(loginBody.path("currentUser").path("userId").asText())
                .isEqualTo(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
        assertThat(loginBody.path("currentUser").path("system").asBoolean()).isTrue();
        assertThat(loginBody.path("currentUser").has("tenantId")).isFalse();

        HttpResponse<String> context = get("/iam.auth/context", token);
        assertThat(context.statusCode()).isEqualTo(200);
        JsonNode contextBody = objectMapper.readTree(context.body());
        assertThat(contextBody).isNotNull();
        assertThat(contextBody.path("userId").asText())
                .isEqualTo(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);

        HttpResponse<String> menus = get("/platform.menu/mine", token);
        assertThat(menus.statusCode()).isEqualTo(200);
        JsonNode menusBody = objectMapper.readTree(menus.body());
        assertThat(menusBody).isNotNull();
        assertThat(menusBody.path("records").isArray()).isTrue();
        assertThat(menusBody.path("records")).isNotEmpty();

        HttpResponse<String> logout = post("/iam.auth/logout", Map.of(), token);
        assertThat(logout.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
                .GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Object body, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private UserSession onlyActiveSession(List<UserSession> sessions) {
        List<UserSession> activeSessions = sessions.stream()
                .filter(session -> session.getRevokedAt() == null)
                .toList();
        assertThat(activeSessions).hasSize(1);
        return activeSessions.getFirst();
    }

    private boolean columnExists(String tableName, String columnName) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                        select count(*)
                        from information_schema.columns
                        where table_schema = current_schema()
                          and table_name = ?
                          and column_name = ?
                        """)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public static class PostgresProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_platform_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.default-schema", "public");
            config.put("muyun.database.install-postgres-plugins", "true");
            config.put("muyun.database.repository-schema-mode", "ENSURE");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.exclude-types", "net.ximatai.muyun.spring.boot.web.CrudWebFormSchemaTest$*");
            config.put("quarkus.arc.remove-unused-beans", "false");
            if (Boolean.getBoolean("muyun.postgres.it.required")) {
                config.put("muyun.platform-bootstrap.enabled", "true");
                return config;
            }
            config.put("muyun.test.postgres.enabled", "false");
            config.put("muyun.platform-bootstrap.enabled", "false");
            config.put("muyun.database.repository-schema-mode", "NONE");
            return config;
        }
    }
}
