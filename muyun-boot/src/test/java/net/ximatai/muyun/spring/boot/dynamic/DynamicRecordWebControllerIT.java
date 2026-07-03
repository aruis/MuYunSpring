package net.ximatai.muyun.spring.boot.dynamic;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(DynamicRecordWebControllerIT.WebProfile.class)
class DynamicRecordWebControllerIT {
    private static final String MODULE = "sales.contract";
    private static final String ENTITY = "contract";

    @TestHTTPResource
    URI baseUri;

    @InjectMock
    DynamicRecordService recordService;

    @InjectMock
    CurrentUserProvider currentUserProvider;

    private HttpClient httpClient;

    @BeforeEach
    void setUpCurrentUser() {
        httpClient = HttpClient.newHttpClient();
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(1)));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(2)));
    }

    @Test
    void shouldBindStaticExactAliasRouteInRealQuarkusHttpContext() throws Exception {
        HttpResponse<String> staticResponse = post("/sales.contract/query");

        assertThat(staticResponse.statusCode()).isEqualTo(200);
    }

    @Test
    void shouldNotCaptureRootFileLikePathInRealMvcMapping() throws Exception {
        assertThat(get("/openapi.json").statusCode()).isEqualTo(404);
        verifyNoInteractions(recordService);
    }

    @Test
    void shouldKeepInheritedRecordActionRouteAsKnownQuarkusMigrationGap() throws Exception {
        assertThat(get("/sales.contract/actions/contract-1").statusCode()).isEqualTo(404);
    }

    @Test
    void shouldRejectPostForReadOnlyDynamicEndpointsInRealMvcMapping() throws Exception {
        assertThat(post("/sales.contract/describe").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return httpClient.send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return baseUri.resolve(path.substring(1));
    }

    @Alternative
    @Priority(1)
    @ApplicationScoped
    public static class NoopTenantService extends TenantService {
        public NoopTenantService() {
            super(mock(TenantDao.class));
        }

        @Override
        public void verifyActiveTenant(String tenantId) {
        }
    }

    @Alternative
    @Priority(1)
    @ApplicationScoped
    public static class TestBeans {
        @Produces
        @Dependent
        PlatformModuleActionService moduleActionService() {
            return mock(PlatformModuleActionService.class);
        }
    }

    public static class WebProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_dynamic_web_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.repository-schema-mode", "NONE");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.exclude-types", String.join(",",
                    "net.ximatai.muyun.spring.boot.iam.IamWebControllerIT$TestBeans",
                    "net.ximatai.muyun.spring.platform.module.PlatformModuleActionService"
            ));
            config.put("quarkus.arc.remove-unused-beans", "false");
            return config;
        }
    }
}
