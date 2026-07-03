package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckDetail;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckResult;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskDefinition;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskGuideDefinition;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskGuideType;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskOriginType;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskStatus;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskType;
import net.ximatai.muyun.spring.platform.ui.PlatformTaskCheckType;
import net.ximatai.muyun.spring.platform.ui.PlatformTaskCompletionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicModuleTaskWebControllerTest {
    private final PlatformModuleTaskCheckService taskCheckService = mock(PlatformModuleTaskCheckService.class);
    private final DynamicRecordService recordService = mock(DynamicRecordService.class);
    private final TenantService activeTenantVerifier = mock(TenantService.class);
    private final DynamicModuleTaskWebController controller =
            new DynamicModuleTaskWebController(taskCheckService, recordService, activeTenantVerifier);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        DynamicWebRequest.clearRequestPath();
    }

    @Test
    void shouldDeclareModuleTaskRoutesAndActionMetadata() throws Exception {
        assertThat(DynamicModuleTaskWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}");
        assertActionRoute("checkTasks",
                new Class<?>[]{String.class, DynamicModuleTaskCheckRequest.class},
                POST.class,
                "/view/{id}/tasks/check",
                true);
        assertActionRoute("evaluateTasks",
                new Class<?>[]{String.class, DynamicModuleTaskCheckRequest.class},
                POST.class,
                "/view/{id}/tasks/evaluate",
                true);
        assertActionRoute("taskDefinitions",
                new Class<?>[]{},
                GET.class,
                "/tasks/definitions",
                false);
    }

    @Test
    void shouldCheckModuleTasksByCurrentRecordAndUiConfig() {
        stubTaskCheck();

        List<PlatformModuleTaskStatus> result = inModuleRequest(
                () -> controller.checkTasks("customer-1", new DynamicModuleTaskCheckRequest("ui-detail")));

        assertThat(result).singleElement().satisfies(task -> {
            assertThat(task.key()).isEqualTo("contracts");
            assertThat(task.status()).isEqualTo(PlatformTaskCompletionStatus.COMPLETE);
            assertThat(task.passed()).isTrue();
            assertThat(task.matchedCount()).isEqualTo(2);
            assertThat(task.expectedCount()).isEqualTo(2);
            assertThat(task.checks()).singleElement()
                    .extracting(PlatformModuleTaskCheckDetail::actualCount)
                    .isEqualTo(2L);
        });
        verifyRecordScope();
        verify(taskCheckService).check("crm.customer", "customer-1", "ui-detail");
    }

    @Test
    void shouldEvaluateModuleTasksWithSummary() {
        stubTaskCheck();

        PlatformModuleTaskCheckResult result = inModuleRequest(
                () -> controller.evaluateTasks("customer-1", new DynamicModuleTaskCheckRequest("ui-detail")));

        assertThat(result.passed()).isTrue();
        assertThat(result.tasks()).singleElement().satisfies(task -> {
            assertThat(task.key()).isEqualTo("contracts");
            assertThat(task.checks()).singleElement()
                    .extracting(PlatformModuleTaskCheckDetail::expectedCount)
                    .isEqualTo(2);
        });
        verifyRecordScope();
    }

    @Test
    void shouldExposeModuleTaskDefinitions() {
        when(taskCheckService.definitions("crm.customer"))
                .thenReturn(List.of(new PlatformModuleTaskDefinition("crm.customer", "profile-ready",
                        "资料齐备", PlatformModuleTaskType.BUSINESS_COMPLETION,
                        PlatformModuleTaskOriginType.LOCAL_EDIT, "local-edit-basic", true, false,
                        true, 10, "/crm.customer/view/{id}",
                        List.of(new PlatformModuleTaskGuideDefinition("profile-ready",
                                PlatformModuleTaskGuideType.OPEN_FORM, "muyun.localEdit",
                                "/crm.customer/view/{id}", "crm.customer", "detail", "name", "补充资料")),
                        List.of())));

        List<PlatformModuleTaskDefinition> result = inModuleRequest(controller::taskDefinitions);

        assertThat(result).singleElement().satisfies(definition -> {
            assertThat(definition.taskCode()).isEqualTo("profile-ready");
            assertThat(definition.managed()).isTrue();
            assertThat(definition.guides()).singleElement()
                    .extracting(PlatformModuleTaskGuideDefinition::guideType)
                    .isEqualTo(PlatformModuleTaskGuideType.OPEN_FORM);
        });
        verify(activeTenantVerifier).verifyActiveTenant("tenant-a");
        verify(taskCheckService).definitions("crm.customer");
    }

    private void stubTaskCheck() {
        when(recordService.mainEntityAlias("crm.customer")).thenReturn("customer");
        when(taskCheckService.check("crm.customer", "customer-1", "ui-detail"))
                .thenReturn(PlatformModuleTaskCheckResult.of(List.of(new PlatformModuleTaskStatus(
                        "contracts",
                        "合同齐备",
                        PlatformTaskCheckType.ASSOCIATION_VIEW,
                        PlatformTaskCompletionStatus.COMPLETE,
                        true,
                        2L,
                        2,
                        List.of(new PlatformModuleTaskCheckDetail(
                                PlatformTaskCheckType.ASSOCIATION_VIEW, true, 2L, 2,
                                "/crm.customer/view/{id}/associations/contracts/query", null)),
                        List.of(),
                        "/crm.customer/view/{id}/associations/contracts/query",
                        null
                ))));
    }

    private void verifyRecordScope() {
        verify(activeTenantVerifier).verifyActiveTenant("tenant-a");
        verify(recordService).requireRecordActionScope(eq("crm.customer"), eq("customer"),
                any(ActionExecutionPolicy.class), eq(List.of("customer-1")), eq(Optional.empty()));
    }

    private <T> T inModuleRequest(java.util.function.Supplier<T> supplier) {
        DynamicWebRequest.useRequestPath("/crm.customer/tasks");
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            return supplier.get();
        }
    }

    private void assertActionRoute(String methodName,
                                   Class<?>[] parameterTypes,
                                   Class<?> httpMethod,
                                   String path,
                                   boolean recordPath) throws Exception {
        Method method = DynamicModuleTaskWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        assertThat(endpoint).isNotNull();
        assertThat(endpoint.value()).isEqualTo(PlatformAction.VIEW);
        if (recordPath) {
            assertThat(method.getParameters()[0].getAnnotation(PathParam.class).value()).isEqualTo("id");
        }
    }
}
