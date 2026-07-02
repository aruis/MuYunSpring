package net.ximatai.muyun.spring.boot.workflow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersion;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowConfigurationWebControllerTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareDefinitionAndVersionRoutesWithNestedCrudContract() throws Exception {
        assertThat(WorkflowDefinitionWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module/{moduleAlias}/workflow-definitions");
        assertThat(WorkflowVersionWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module/{moduleAlias}/workflow-definitions/{definitionId}/versions");
        assertRoute(NestedCrudWebSupport.class.getMethod("insert", HttpServletRequest.class,
                        net.ximatai.muyun.spring.common.model.contract.EntityContract.class),
                POST.class, "/insert");
        assertRoute(NestedCrudWebSupport.class.getMethod("update", HttpServletRequest.class, String.class,
                        net.ximatai.muyun.spring.common.model.contract.EntityContract.class),
                POST.class, "/update/{id}");
        assertRoute(NestedCrudWebSupport.class.getMethod("delete", HttpServletRequest.class, String.class),
                POST.class, "/delete/{id}");
        assertRoute(NestedSortableCrudWebSupport.class.getMethod("sort", HttpServletRequest.class, String.class,
                        net.ximatai.muyun.spring.boot.web.SortWebRequest.class),
                POST.class, "/sort/{id}");

        assertActionEndpoint(WorkflowDefinitionWebController.class.getMethod("insert",
                        HttpServletRequest.class, WorkflowDefinition.class),
                PlatformAction.CREATE);
        assertActionEndpoint(WorkflowVersionWebController.class.getMethod("insert",
                        HttpServletRequest.class, WorkflowVersion.class),
                PlatformAction.CREATE);
        Method publish = WorkflowDefinitionWebController.class.getMethod("publish",
                HttpServletRequest.class, String.class, String.class);
        CustomActionEndpoint endpoint = publish.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo("publishWorkflowDefinition");
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(endpoint.recordIdPathVariable()).isEqualTo("definitionId");
    }

    @Test
    void shouldBindWorkflowDefinitionModuleFromPathAndForceDraftOnInsert() {
        WorkflowDefinitionService definitionService = mock(WorkflowDefinitionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        TestWorkflowDefinitionWebController controller = new TestWorkflowDefinitionWebController(
                moduleService, mock(WorkflowPublishFacade.class), definitionService);
        when(moduleService.resolveVisibleModule("sales.contract")).thenReturn(module("sales.contract"));
        WorkflowDefinition inserted = definition("def-1", "sales.contract", WorkflowDefinitionStatus.DRAFT);
        when(definitionService.insert(any(WorkflowDefinition.class))).thenReturn("def-1");
        when(definitionService.select("def-1")).thenReturn(inserted);

        WebRecordResponse<WorkflowDefinition> response = controller.insert(
                requestVars("sales.contract", "def-1"),
                definition(null, "other.module", WorkflowDefinitionStatus.PUBLISHED));

        assertThat(response.record().getApplicationAlias()).isEqualTo("sales");
        assertThat(response.record().getModuleAlias()).isEqualTo("sales.contract");
        ArgumentCaptor<WorkflowDefinition> captor = ArgumentCaptor.forClass(WorkflowDefinition.class);
        verify(definitionService).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("sales");
        assertThat(captor.getValue().getModuleAlias()).isEqualTo("sales.contract");
        assertThat(captor.getValue().getDefinitionStatus()).isEqualTo(WorkflowDefinitionStatus.DRAFT);
        assertThat(captor.getValue().getCurrentVersionNo()).isNull();
    }

    @Test
    void shouldRejectDefinitionOutsideModuleOrPublishedDefinitionMutation() {
        WorkflowDefinitionService definitionService = mock(WorkflowDefinitionService.class);
        PlatformModuleService moduleService = mock(PlatformModuleService.class);
        TestWorkflowDefinitionWebController controller = new TestWorkflowDefinitionWebController(
                moduleService, mock(WorkflowPublishFacade.class), definitionService);
        when(moduleService.resolveVisibleModule("sales.ghost")).thenReturn(null);
        when(definitionService.select("def-1")).thenReturn(
                definition("def-1", "sales.contract", WorkflowDefinitionStatus.PUBLISHED));

        assertThatThrownBy(() -> controller.insert(
                requestVars("sales.ghost", "def-1"),
                definition(null, "sales.ghost", WorkflowDefinitionStatus.DRAFT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("platform module not found");
        assertThatThrownBy(() -> controller.update(
                requestVars("sales.contract", "def-1"),
                "def-1",
                definition("def-1", "sales.contract", WorkflowDefinitionStatus.DRAFT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can only edit draft definitions");
    }

    @Test
    void shouldBindWorkflowVersionDefinitionAndForceDraftOnInsert() {
        WorkflowDefinitionService definitionService = mock(WorkflowDefinitionService.class);
        WorkflowVersionService versionService = mock(WorkflowVersionService.class);
        TestWorkflowVersionWebController controller = new TestWorkflowVersionWebController(
                definitionService, versionService);
        when(definitionService.select("def-1")).thenReturn(
                definition("def-1", "sales.contract", WorkflowDefinitionStatus.DRAFT));
        WorkflowVersion inserted = version("ver-1", "def-1", 1, WorkflowPublishStatus.DRAFT);
        when(versionService.insert(any(WorkflowVersion.class))).thenReturn("ver-1");
        when(versionService.select("ver-1")).thenReturn(inserted);

        WebRecordResponse<WorkflowVersion> response = controller.insert(
                requestVars("sales.contract", "def-1"),
                version(null, "other-def", 1, WorkflowPublishStatus.PUBLISHED));

        assertThat(response.record().getDefinitionId()).isEqualTo("def-1");
        assertThat(response.record().getPublishStatus()).isEqualTo(WorkflowPublishStatus.DRAFT);
        ArgumentCaptor<WorkflowVersion> captor = ArgumentCaptor.forClass(WorkflowVersion.class);
        verify(versionService).insert(captor.capture());
        assertThat(captor.getValue().getDefinitionId()).isEqualTo("def-1");
        assertThat(captor.getValue().getPublishStatus()).isEqualTo(WorkflowPublishStatus.DRAFT);
        assertThat(captor.getValue().getPublishedBy()).isNull();
        assertThat(captor.getValue().getPublishedAt()).isNull();
    }

    @Test
    void shouldPublishWorkflowVersionThroughFacadeWithCurrentUser() {
        WorkflowDefinitionService definitionService = mock(WorkflowDefinitionService.class);
        WorkflowPublishFacade publishFacade = mock(WorkflowPublishFacade.class);
        TestWorkflowDefinitionWebController controller = new TestWorkflowDefinitionWebController(
                mock(PlatformModuleService.class), publishFacade, definitionService);
        when(definitionService.select("def-1")).thenReturn(
                definition("def-1", "sales.contract", WorkflowDefinitionStatus.DRAFT));
        when(publishFacade.publish("def-1", "ver-1", "user-1"))
                .thenReturn(version("ver-1", "def-1", 1, WorkflowPublishStatus.PUBLISHED));

        WorkflowVersion response;
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            response = controller.publish(requestVars("sales.contract", "def-1"), "def-1", "ver-1");
        }

        assertThat(response.getPublishStatus()).isEqualTo(WorkflowPublishStatus.PUBLISHED);
        verify(publishFacade).publish("def-1", "ver-1", "user-1");
    }

    private void assertRoute(Method method, Class<?> httpMethod, String path) {
        assertThat(method.getAnnotation(httpMethod.asSubclass(Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
    }

    private void assertActionEndpoint(Method method, PlatformAction action) {
        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(action);
    }

    private WorkflowDefinition definition(String id, String moduleAlias, WorkflowDefinitionStatus status) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId(id);
        definition.setApplicationAlias(moduleAlias.substring(0, moduleAlias.indexOf('.')));
        definition.setModuleAlias(moduleAlias);
        definition.setAlias("approval");
        definition.setTitle("Approval");
        definition.setDefinitionStatus(status);
        definition.setCurrentVersionNo(3);
        return definition;
    }

    private PlatformModule module(String alias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setTitle(alias);
        return module;
    }

    private WorkflowVersion version(String id, String definitionId, int versionNo, WorkflowPublishStatus status) {
        WorkflowVersion version = new WorkflowVersion();
        version.setId(id);
        version.setDefinitionId(definitionId);
        version.setVersionNo(versionNo);
        version.setPublishStatus(status);
        version.setPublishedBy("other");
        version.setPublishedAt(java.time.Instant.parse("2026-06-01T00:00:00Z"));
        return version;
    }

    private HttpServletRequest requestVars(String moduleAlias, String definitionId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(NestedCrudWebSupport.PATH_VARIABLES_ATTRIBUTE))
                .thenReturn(Map.of("moduleAlias", moduleAlias, "definitionId", definitionId));
        return request;
    }

    private static final class TestWorkflowDefinitionWebController extends WorkflowDefinitionWebController {
        private TestWorkflowDefinitionWebController(PlatformModuleService moduleService,
                                                    WorkflowPublishFacade publishFacade,
                                                    WorkflowDefinitionService definitionService) {
            super(moduleService, publishFacade);
            this.service = definitionService;
        }
    }

    private static final class TestWorkflowVersionWebController extends WorkflowVersionWebController {
        private TestWorkflowVersionWebController(WorkflowDefinitionService definitionService,
                                                 WorkflowVersionService versionService) {
            super(definitionService);
            this.service = versionService;
        }
    }
}
