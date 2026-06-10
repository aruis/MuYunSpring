package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreference;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreferenceService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformPagePreferenceWebControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldExposeCurrentUserPagePreference() throws Exception {
        PlatformPagePreferenceService service = mock(PlatformPagePreferenceService.class);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new PlatformPagePreferenceWebController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
        PlatformPagePreference preference = preference("pref-1", "{\"columns\":[\"code\"]}");
        when(service.currentUserPreference("sales.contract", PlatformUiClientType.WEB, "list"))
                .thenReturn(preference);
        when(service.saveCurrentUserPreference(eq("sales.contract"), eq(PlatformUiClientType.WEB),
                eq("list"), eq("{\"columns\":[\"code\"]}")))
                .thenReturn(preference);

        mvc.perform(post("/platform.page-preference/{moduleAlias}", "sales.contract")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clientType": "WEB",
                                  "pageKey": "list",
                                  "preferenceJson": "{\\"columns\\":[\\"code\\"]}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-1"))
                .andExpect(jsonPath("$.preferenceJson").value("{\"columns\":[\"code\"]}"));

        mvc.perform(get("/platform.page-preference/{moduleAlias}", "sales.contract")
                        .param("clientType", "WEB")
                        .param("pageKey", "list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-1"));
    }

    private PlatformPagePreference preference(String id, String preferenceJson) {
        PlatformPagePreference preference = new PlatformPagePreference();
        preference.setId(id);
        preference.setUserId("user-1");
        preference.setModuleAlias("sales.contract");
        preference.setClientType("WEB");
        preference.setPageKey("list");
        preference.setPreferenceJson(preferenceJson);
        return preference;
    }
}
