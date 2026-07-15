package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.ActionResultResponseAdvice;
import net.ximatai.muyun.spring.boot.web.ActionEndpointContextResolver;
import net.ximatai.muyun.spring.boot.web.ActionEndpointInterceptor;
import net.ximatai.muyun.spring.boot.web.BusinessMutationInterceptor;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccountWebControllerTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDelegatePasswordChangeToService() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserAccountWebController controller = new UserAccountWebController();
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(1);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.changePassword("user-1",
                    new UserAccountWebController.ChangePasswordRequest("secret2"))).isEqualTo(1);
        }

        verify(userAccountService).changePassword("user-1", "secret2");
    }

    @Test
    void shouldDelegatePasswordResetToService() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserAccountWebController controller = new UserAccountWebController();
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.resetPassword("user-1")).thenReturn(
                new UserAccountService.PasswordResetResult(1, "temp-secret", Instant.parse("2026-07-08T00:00:00Z")));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            UserAccountWebController.ResetPasswordResponse response = controller.resetPassword("user-1");

            assertThat(response.count()).isEqualTo(1);
            assertThat(response.temporaryPassword()).isEqualTo("temp-secret");
        }

        verify(userAccountService).resetPassword("user-1");
    }

    @Test
    void shouldDelegateForceLogoutToService() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserAccountWebController controller = new UserAccountWebController();
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.forceLogout("user-1")).thenReturn(1);

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            assertThat(controller.forceLogout("user-1")).isEqualTo(1);
        }

        verify(userAccountService).forceLogout("user-1");
    }

    @Test
    void shouldWrapPasswordBusinessMutationResults() throws Exception {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserAccountWebController controller = new UserAccountWebController();
        ReflectionTestUtils.setField(controller, "service", userAccountService);
        when(userAccountService.changePassword("user-1", "secret2")).thenReturn(1);
        when(userAccountService.resetPassword("user-1")).thenReturn(
                new UserAccountService.PasswordResetResult(1, "temp-secret", null));
        when(userAccountService.forceLogout("user-1")).thenReturn(1);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new ActionEndpointInterceptor(new AllowAllActionExecutionPolicyService(),
                        new ActionEndpointContextResolver()))
                .addInterceptors(new BusinessMutationInterceptor())
                .setControllerAdvice(new ActionResultResponseAdvice(UserAccountWebControllerTest::moduleAlias,
                        new com.fasterxml.jackson.databind.ObjectMapper()))
                .build();
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            mvc.perform(post("/iam.user/changePassword/user-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password":"secret2"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(1))
                    .andExpect(jsonPath("$.message.code").value("iam.user.password-changed"))
                    .andExpect(jsonPath("$.message.text").value("密码已修改"))
                    .andExpect(jsonPath("$.changes[?(@.type == 'record-updated' && @.moduleAlias == 'iam.user' && @.recordId == 'user-1')]")
                            .exists());

            mvc.perform(post("/iam.user/resetPassword/user-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.count").value(1))
                    .andExpect(jsonPath("$.data.temporaryPassword").value("temp-secret"))
                    .andExpect(jsonPath("$.message.code").value("iam.user.password-reset"))
                    .andExpect(jsonPath("$.message.text").value("密码已重置"))
                    .andExpect(jsonPath("$.changes[?(@.type == 'record-updated' && @.moduleAlias == 'iam.user' && @.recordId == 'user-1')]")
                            .exists());

            mvc.perform(post("/iam.user/forceLogout/user-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(1))
                    .andExpect(jsonPath("$.message.code").value("iam.user.force-logout"))
                    .andExpect(jsonPath("$.message.text").value("用户已下线"))
                    .andExpect(jsonPath("$.changes[?(@.type == 'record-updated' && @.moduleAlias == 'iam.user' && @.recordId == 'user-1')]")
                            .exists());
        }
    }

    private static String moduleAlias(Class<?> moduleType) {
        try {
            Object value = moduleType.getField("MODULE_ALIAS").get(null);
            if (value instanceof String alias) {
                return alias;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to a clear test failure.
        }
        throw new IllegalArgumentException("missing MODULE_ALIAS: " + moduleType.getName());
    }
}
