package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.web.PlatformWebExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformModuleRuntimeActionWebControllerTest {
    @Test
    void shouldExposeRecordActionAvailabilityByModuleAliasAndRecordId() throws Exception {
        PlatformRecordActionAvailabilityService service = mock(PlatformRecordActionAvailabilityService.class);
        when(service.recordActions("iam.user", "platform.user.super_admin"))
                .thenReturn(new PlatformRecordActionAvailability(
                        "platform.user.super_admin",
                        List.of(new PlatformRecordActionAvailability.Action(
                                "resetPassword",
                                false,
                                "cannot administrate current user's password"
                        ))
                ));
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new PlatformModuleRuntimeActionWebController(service))
                .setControllerAdvice(new PlatformWebExceptionHandler())
                .build();

        mvc.perform(get("/{moduleAlias}/actions/{recordId}", "iam.user", "platform.user.super_admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordId").value("platform.user.super_admin"))
                .andExpect(jsonPath("$.actions[0].actionCode").value("resetPassword"))
                .andExpect(jsonPath("$.actions[0].available").value(false))
                .andExpect(jsonPath("$.actions[0].reason").value("cannot administrate current user's password"));

        verify(service).recordActions("iam.user", "platform.user.super_admin");
    }
}
