package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformModuleRuntimeContextWebControllerTest {
    @Test
    void shouldDeclareRuntimeContextRouteAndActionMetadata() throws Exception {
        assertThat(PlatformModuleRuntimeContextWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module/{moduleAlias}/context");

        Method method = PlatformModuleRuntimeContextWebController.class.getMethod("context", String.class);
        assertThat(method.getAnnotation(GET.class)).isNotNull();
        assertThat(method.getParameters()[0].getAnnotation(PathParam.class).value()).isEqualTo("moduleAlias");

        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(PlatformAction.MENU);
    }

    @Test
    void shouldExposeRuntimeContextByDottedModuleAlias() {
        PlatformModuleRuntimeContextService service = mock(PlatformModuleRuntimeContextService.class);
        PlatformModuleRuntimeContext expected = new PlatformModuleRuntimeContext(
                "iam.organization",
                "组织管理",
                ModuleKind.STATIC,
                ModuleEntryType.ROUTE,
                "/iam/organizations",
                null,
                "organization",
                Set.of(EntityCapability.CRUD, EntityCapability.TREE),
                Set.of("crud", "tree"),
                List.of(),
                ModuleUiDefinition.builder("iam.organization")
                        .listView(list -> list.field("title", field -> field.label("组织名称")))
                        .build()
        );
        when(service.context("iam.organization")).thenReturn(expected);

        PlatformModuleRuntimeContext actual = new PlatformModuleRuntimeContextWebController(service)
                .context("iam.organization");

        assertThat(actual.moduleAlias()).isEqualTo("iam.organization");
        assertThat(actual.entryRoute()).isEqualTo("/iam/organizations");
        assertThat(actual.abilities()).contains("tree");
        assertThat(actual.uiDescriptor().schemaVersion()).isEqualTo(ResolvedModuleUiDescriptor.SCHEMA_VERSION);
        assertThat(actual.uiDescriptor().moduleAlias()).isEqualTo("iam.organization");
        assertThat(actual.uiDescriptor().views()).singleElement().satisfies(view -> {
            assertThat(view.viewCode()).isEqualTo("default_list");
            assertThat(view.fields()).singleElement().satisfies(field -> {
                assertThat(field.fieldRef().fieldName()).isEqualTo("title");
                assertThat(field.label()).isEqualTo("组织名称");
            });
        });
        verify(service).context("iam.organization");
    }

    @Test
    void shouldPropagateRuntimeContextNotFound() {
        PlatformModuleRuntimeContextService service = mock(PlatformModuleRuntimeContextService.class);
        when(service.context("iam.ghost")).thenThrow(new PlatformException(
                PlatformErrorCodes.RESOURCE_NOT_FOUND,
                404,
                "module runtime context not found: iam.ghost"
        ));

        assertThatThrownBy(() -> new PlatformModuleRuntimeContextWebController(service).context("iam.ghost"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("module runtime context not found: iam.ghost")
                .extracting("code", "httpStatus")
                .containsExactly(PlatformErrorCodes.RESOURCE_NOT_FOUND, 404);
    }
}
