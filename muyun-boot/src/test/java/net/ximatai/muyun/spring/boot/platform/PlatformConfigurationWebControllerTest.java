package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebTreeNode;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRefreshResult;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.runtime.PlatformDynamicRuntimeRefreshService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformConfigurationWebControllerTest {
    private final PlatformModuleService moduleService = mock(PlatformModuleService.class);
    private final PlatformDynamicRuntimeRefreshService refreshService =
            mock(PlatformDynamicRuntimeRefreshService.class);
    private final PlatformModuleWebController controller = new PlatformModuleWebController(refreshService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDeclarePlatformModuleRoutesAndActionMetadata() throws Exception {
        assertThat(PlatformModuleWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module");
        PlatformStaticModule module = PlatformModuleWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.alias()).isEqualTo(PlatformModuleService.MODULE_ALIAS);
        PlatformMenu menu = PlatformModuleWebController.class.getAnnotation(PlatformMenu.class);
        assertThat(menu.parent()).isEqualTo(PlatformMenuGroups.CONFIG);

        assertActionRoute("tree", new Class<?>[]{String.class, boolean.class},
                GET.class, "/tree/{applicationAlias}", PlatformAction.TREE);
        assertActionRoute("treeChildren", new Class<?>[]{String.class, String.class, boolean.class, boolean.class},
                GET.class, "/tree/{applicationAlias}/{parentId}", PlatformAction.TREE);
        assertCustomRoute("refreshRuntime", new Class<?>[]{String.class},
                "/{moduleAlias}/runtime/refresh", "refreshDynamicRuntime");
        assertCustomRoute("executeRefreshRuntime", new Class<?>[]{String.class},
                "/{moduleAlias}/runtime/execute-refresh", "executeRefreshDynamicRuntime");
        assertCustomRoute("previewRefreshRuntime", new Class<?>[]{String.class},
                "/{moduleAlias}/runtime/preview-refresh", "previewRefreshDynamicRuntime");
    }

    @Test
    void shouldExposeApplicationScopedModuleTree() throws Exception {
        setService(controller, moduleService);
        PlatformModule root = module("platform.sales", "platform", null);
        PlatformModule child = module("platform.sales.order", "platform", "platform.sales");
        when(moduleService.rootModules("platform")).thenReturn(List.of(root));
        when(moduleService.children("platform", "platform.sales")).thenReturn(List.of(child));
        when(moduleService.children("platform", "platform.sales.order")).thenReturn(List.of());

        WebListResponse<?> response = inTenant(() -> controller.tree("platform", false));

        assertThat(response.records()).singleElement().isInstanceOfSatisfying(WebTreeNode.class, node -> {
            assertThat(((PlatformModule) node.record()).getId()).isEqualTo("platform.sales");
            assertThat(node.children()).singleElement().isInstanceOfSatisfying(WebTreeNode.class, childNode -> {
                WebTreeNode<?> typedChild = (WebTreeNode<?>) childNode;
                assertThat(((PlatformModule) typedChild.record()).getId()).isEqualTo("platform.sales.order");
            });
        });
        verify(moduleService).rootModules("platform");
    }

    @Test
    void shouldExposeFlatModuleTreeChildrenWithOptionalSelf() throws Exception {
        setService(controller, moduleService);
        PlatformModule root = module("platform.sales", "platform", TreeAbility.ROOT_ID);
        PlatformModule child = module("platform.sales.order", "platform", "platform.sales");
        when(moduleService.select("platform.sales")).thenReturn(root);
        when(moduleService.children("platform", "platform.sales")).thenReturn(List.of(child));
        when(moduleService.children("platform", "platform.sales.order")).thenReturn(List.of());

        WebListResponse<?> response = inTenant(() -> controller.treeChildren(
                "platform", "platform.sales", true, true));

        assertThat(response.records()).extracting(record -> ((PlatformModule) record).getId())
                .containsExactly("platform.sales", "platform.sales.order");
    }

    @Test
    void shouldRejectTreeChildrenWhenParentBelongsToOtherApplication() throws Exception {
        setService(controller, moduleService);
        when(moduleService.select("platform.sales")).thenReturn(module("platform.sales", "crm", TreeAbility.ROOT_ID));

        assertThatThrownBy(() -> inTenant(() -> controller.treeChildren(
                "platform", "platform.sales", false, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("module parent must belong to application: platform");
    }

    @Test
    void shouldRefreshDynamicRuntimeThroughModuleConfigurationEndpoints() throws Exception {
        setService(controller, moduleService);
        when(refreshService.refresh("crm.contract")).thenReturn(runtimeRefreshResult(false));
        when(refreshService.executeRefresh("crm.contract")).thenReturn(runtimeRefreshResult(false));
        when(refreshService.previewRefresh("crm.contract")).thenReturn(runtimeRefreshResult(true));

        assertThat(inTenant(() -> controller.refreshRuntime("crm.contract")).dryRun()).isFalse();
        assertThat(inTenant(() -> controller.executeRefreshRuntime("crm.contract")).dryRun()).isFalse();
        assertThat(inTenant(() -> controller.previewRefreshRuntime("crm.contract")).dryRun()).isTrue();

        verify(refreshService).refresh("crm.contract");
        verify(refreshService).executeRefresh("crm.contract");
        verify(refreshService).previewRefresh("crm.contract");
    }

    private DynamicModuleRefreshResult runtimeRefreshResult(boolean dryRun) {
        return new DynamicModuleRefreshResult(
                new ModuleDefinition("crm.contract", "Contract", List.of()),
                Map.of(),
                dryRun);
    }

    private PlatformModule module(String id, String applicationAlias, String parentId) {
        PlatformModule module = new PlatformModule();
        module.setId(id);
        module.setApplicationAlias(applicationAlias);
        module.setParentId(parentId);
        return module;
    }

    private void assertActionRoute(String methodName,
                                   Class<?>[] parameterTypes,
                                   Class<?> httpMethod,
                                   String path,
                                   PlatformAction action) throws Exception {
        Method method = PlatformModuleWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        assertThat(method.getAnnotation(ActionEndpoint.class).value()).isEqualTo(action);
    }

    private void assertCustomRoute(String methodName,
                                   Class<?>[] parameterTypes,
                                   String path,
                                   String actionCode) throws Exception {
        Method method = PlatformModuleWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(PlatformActionLevel.RECORD);
        assertThat(endpoint.recordIdPathVariable()).isEqualTo("moduleAlias");
    }

    private <T> T inTenant(Supplier<T> supplier) {
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            return supplier.get();
        }
    }

    private void setService(PlatformModuleWebController target, PlatformModuleService service)
            throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(target, service);
    }
}
