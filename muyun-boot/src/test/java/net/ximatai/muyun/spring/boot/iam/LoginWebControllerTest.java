package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.boot.web.PlatformWebExceptionHandler;
import net.ximatai.muyun.spring.common.exception.AuthenticationFailedException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginWebControllerTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldExposeCurrentUserContext() throws Exception {
        LoginWebController controller = new LoginWebController(mock(UserSessionService.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("user-1", "Alice", "tenant-a", "org-1"))))
                .build();

        mvc.perform(get("/iam.auth/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.username").value("Alice"))
                .andExpect(jsonPath("$.tenantId").value("tenant-a"))
                .andExpect(jsonPath("$.organizationId").value("org-1"));
    }

    @Test
    void shouldReturnUnauthorizedWhenCurrentUserContextIsMissing() throws Exception {
        LoginWebController controller = new LoginWebController(mock(UserSessionService.class));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PlatformWebExceptionHandler())
                .build();

        mvc.perform(get("/iam.auth/context"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("current user context is not available"));
    }

    @Test
    void shouldReturnUnauthorizedWhenLoginCredentialsAreInvalid() throws Exception {
        UserSessionService userSessionService = mock(UserSessionService.class);
        when(userSessionService.login(anyString(), anyString(), anyString()))
                .thenThrow(new AuthenticationFailedException("invalid username or password"));
        LoginWebController controller = new LoginWebController(userSessionService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PlatformWebExceptionHandler())
                .build();

        mvc.perform(post("/iam.auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"tenantId":"tenant-a","username":"alice","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("invalid username or password"));
    }

    @Test
    void shouldReturnBadRequestWhenLoginRequestIsMalformed() throws Exception {
        UserSessionService userSessionService = mock(UserSessionService.class);
        when(userSessionService.login(isNull(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("tenantId must not be null"));
        LoginWebController controller = new LoginWebController(userSessionService);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PlatformWebExceptionHandler())
                .build();

        mvc.perform(post("/iam.auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"secret1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("tenantId must not be null"));
    }
}
