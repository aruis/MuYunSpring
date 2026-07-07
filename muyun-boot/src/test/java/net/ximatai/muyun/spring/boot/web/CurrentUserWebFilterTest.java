package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUserWebFilterTest {
    @Test
    void shouldRejectBusinessRequestsWhenPasswordChangeIsRequired() throws Exception {
        MockMvc mvc = restrictedMvc();

        mvc.perform(get("/business"))
                .andExpect(status().isUnauthorized())
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
