package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserWebFilterTest {
    @Test
    void shouldRejectBusinessRequestsWhenPasswordChangeIsRequired() throws Exception {
        MockMvc mvc = restrictedMvc();

        mvc.perform(get("/business"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(PlatformErrorCodes.PASSWORD_CHANGE_REQUIRED)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("password change required")));
    }

    @Test
    void shouldAllowPasswordChangeRequestWhenPasswordChangeIsRequired() throws Exception {
        MockMvc mvc = restrictedMvc();

        mvc.perform(post("/iam.auth/changeOwnPassword"))
                .andExpect(status().isOk())
                .andExpect(content().string("changed"));
    }

    @Test
    void shouldAllowContextRequestWhenPasswordChangeIsRequired() throws Exception {
        MockMvc mvc = restrictedMvc();

        mvc.perform(get("/iam.auth/context"))
                .andExpect(status().isOk())
                .andExpect(content().string("context"));
    }

    @Test
    void shouldRejectInvalidBearerTokenBeforeBusinessHandler() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new CurrentUserWebFilter(Optional::empty))
                .build();

        mvc.perform(get("/business").header("Authorization", "Bearer stale-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AUTH_REQUIRED")));
    }

    @Test
    void shouldKeepCorsHeadersWhenRejectingInvalidBearerToken() throws Exception {
        MuYunSpringCorsProperties properties = new MuYunSpringCorsProperties();
        FilterRegistrationBean<CorsFilter> cors = new MuYunSpringWebConfiguration(properties)
                .corsFilterRegistration();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(cors.getFilter(), new CurrentUserWebFilter(Optional::empty))
                .build();

        mvc.perform(get("/business")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Authorization", "Bearer stale-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AUTH_REQUIRED")));
    }

    private MockMvc restrictedMvc() {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("user-1", "alice", "tenant-a", "org-1", true))))
                .build();
    }

    @RestController
    private static class TestController {
        @GetMapping("/business")
        String business() {
            return "business";
        }

        @PostMapping("/iam.auth/changeOwnPassword")
        String changeOwnPassword() {
            return "changed";
        }

        @GetMapping("/iam.auth/context")
        String context() {
            return "context";
        }
    }
}
