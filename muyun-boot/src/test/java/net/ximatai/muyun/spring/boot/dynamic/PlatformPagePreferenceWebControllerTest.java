package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreference;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreferenceService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformPagePreferenceWebControllerTest {
    @Test
    void shouldDeclarePagePreferenceRoutes() throws Exception {
        assertThat(PlatformPagePreferenceWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.page-preference/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}");

        Method preference = PlatformPagePreferenceWebController.class.getMethod(
                "preference", String.class, PlatformUiClientType.class, String.class);
        assertThat(preference.getAnnotation(GET.class)).isNotNull();
        assertPathParam(preference, 0, "moduleAlias");
        assertThat(preference.getParameters()[1].getAnnotation(QueryParam.class).value()).isEqualTo("clientType");
        assertThat(preference.getParameters()[1].getAnnotation(DefaultValue.class).value()).isEqualTo("WEB");
        assertThat(preference.getParameters()[2].getAnnotation(QueryParam.class).value()).isEqualTo("pageKey");

        Method savePreference = PlatformPagePreferenceWebController.class.getMethod(
                "savePreference", String.class, PlatformPagePreferenceRequest.class);
        assertThat(savePreference.getAnnotation(POST.class)).isNotNull();
        assertPathParam(savePreference, 0, "moduleAlias");
    }

    @Test
    void shouldExposeCurrentUserPagePreference() {
        PlatformPagePreferenceService service = mock(PlatformPagePreferenceService.class);
        PlatformPagePreferenceWebController controller = new PlatformPagePreferenceWebController(service);
        PlatformPagePreference preference = preference("pref-1", "{\"columns\":[\"code\"]}");
        when(service.currentUserPreference("sales.contract", PlatformUiClientType.WEB, "list"))
                .thenReturn(preference);
        when(service.saveCurrentUserPreference(eq("sales.contract"), eq(PlatformUiClientType.WEB),
                eq("list"), eq("{\"columns\":[\"code\"]}")))
                .thenReturn(preference);

        PlatformPagePreference saved = controller.savePreference("sales.contract",
                new PlatformPagePreferenceRequest(PlatformUiClientType.WEB, "list", "{\"columns\":[\"code\"]}"));
        PlatformPagePreference queried = controller.preference("sales.contract", PlatformUiClientType.WEB, "list");

        assertThat(saved.getId()).isEqualTo("pref-1");
        assertThat(saved.getPreferenceJson()).isEqualTo("{\"columns\":[\"code\"]}");
        assertThat(queried.getId()).isEqualTo("pref-1");
        verify(service).saveCurrentUserPreference(
                "sales.contract", PlatformUiClientType.WEB, "list", "{\"columns\":[\"code\"]}");
        verify(service).currentUserPreference("sales.contract", PlatformUiClientType.WEB, "list");
    }

    @Test
    void shouldRejectBlankPreferenceJson() {
        PlatformPagePreferenceService service = mock(PlatformPagePreferenceService.class);
        PlatformPagePreferenceWebController controller = new PlatformPagePreferenceWebController(service);

        assertThatThrownBy(() -> controller.savePreference("sales.contract",
                new PlatformPagePreferenceRequest(PlatformUiClientType.WEB, "list", " ")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("page preference preferenceJson must not be blank");
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

    private void assertPathParam(Method method, int parameterIndex, String name) {
        assertThat(method.getParameters()[parameterIndex].getAnnotation(PathParam.class).value()).isEqualTo(name);
    }
}
