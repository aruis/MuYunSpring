package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
