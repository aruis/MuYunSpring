package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SortDirection;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.ReferenceWeb;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.web.PlatformWebPathRules;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiDocument;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFormulaPreviewResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveStatus;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewItem;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewService;
import net.ximatai.muyun.spring.platform.code.CodeFieldRole;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckResult;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateCheckService;
import net.ximatai.muyun.spring.platform.duplicate.RecordDuplicateMatch;
import net.ximatai.muyun.spring.platform.generation.ReferenceRecordGenerationFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DynamicRecordWebControllerTest {
    private static final String MODULE = "sales.contract";
    private static final String ENTITY = "contract";

    private DynamicRecordService recordService;
    private DynamicEntityOperations mainEntity;
    private TenantService tenantService;
    private CodeBusinessPreviewService codeBusinessPreviewService;
    private RecordDuplicateCheckService duplicateCheckService;
    private DynamicRecordWebController controller;

    @BeforeEach
    void setUp() {
        recordService = mock(DynamicRecordService.class);
        mainEntity = mock(DynamicEntityOperations.class);
        tenantService = mock(TenantService.class);
        codeBusinessPreviewService = mock(CodeBusinessPreviewService.class);
        duplicateCheckService = mock(RecordDuplicateCheckService.class);
        ReferenceRecordGenerationFacade generationFacade = mock(ReferenceRecordGenerationFacade.class);
        controller = new DynamicRecordWebController(recordService, tenantService,
                codeBusinessPreviewService, generationFacade,
                null, null, null, null, null, duplicateCheckService, null);

        when(recordService.mainEntity(MODULE)).thenReturn(mainEntity);
        when(recordService.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(recordService.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(mainEntity.newRecord()).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(mainEntity.describe()).thenReturn(DynamicModuleDescriptor.from(module()).entities().get(0));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(1)));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(2)));
    }

    @AfterEach
    void tearDown() {
        DynamicWebRequest.clearRequestPath();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareDynamicRecordRoutesWithJaxRsAnnotations() throws Exception {
        assertThat(DynamicRecordWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}");
        assertRoute(DynamicRecordWebController.class.getMethod("describeModule", String.class),
                GET.class, "/describe", null);
        assertRoute(DynamicRecordWebController.class.getMethod("openApi", String.class),
                GET.class, "/openapi", null);
        assertRoute(DynamicRecordWebController.class.getMethod("query", WebQueryRequest.class),
                POST.class, "/query", PlatformAction.QUERY);
        assertRoute(DynamicRecordWebController.class.getMethod("querySummary", WebQueryRequest.class),
                POST.class, "/query/summary", PlatformAction.QUERY);
        assertRoute(DynamicRecordWebController.class.getMethod("insert", DynamicRecord.class),
                CrudWeb.class.getMethod("insert", net.ximatai.muyun.spring.common.model.contract.EntityContract.class),
                POST.class, "/insert", PlatformAction.CREATE);
        assertRoute(DynamicRecordWebController.class.getMethod("update", String.class, DynamicRecord.class),
                CrudWeb.class.getMethod("update", String.class,
                        net.ximatai.muyun.spring.common.model.contract.EntityContract.class),
                POST.class, "/update/{id}", PlatformAction.UPDATE);
        assertRoute(DynamicRecordWebController.class.getMethod("previewCode", String.class, DynamicRecord.class),
                POST.class, "/code/preview", PlatformAction.CREATE);
        assertRoute(DynamicRecordWebController.class.getMethod("previewFormula",
                        String.class, DynamicFormulaPreviewRequest.class),
                POST.class, "/formula/preview", PlatformAction.CREATE);
        assertRoute(DynamicRecordWebController.class.getMethod("dynamicActions", String.class),
                GET.class, "/actions", null);
        assertRoute(DynamicRecordWebController.class.getMethod("dynamicRecordActions", String.class, String.class),
                GET.class, "/actions/{recordId}", null);
        assertRoute(DynamicRecordWebController.class.getMethod("executeDynamicListAction",
                        String.class, String.class, DynamicWebActionRequest.class),
                POST.class, "/" + PlatformWebPathRules.ACTION_CODE_PATH,
                null);
        assertRoute(DynamicRecordWebController.class.getMethod("executeDynamicBatchAction",
                        String.class, String.class, DynamicWebActionRequest.class),
                POST.class, "/" + PlatformWebPathRules.ACTION_CODE_PATH + "/batch",
                null);
        assertRoute(DynamicRecordWebController.class.getMethod("executeDynamicRecordAction",
                        String.class, String.class, String.class, DynamicWebActionRequest.class),
                POST.class, "/" + PlatformWebPathRules.ACTION_CODE_PATH + "/{recordId}",
                null);
        assertRoute(DynamicRecordWebController.class.getMethod("reference",
                        String.class, DynamicWebReferenceRequest.class),
                ReferenceWeb.class.getMethod("reference", String.class, Object.class),
                POST.class, "/references/{fieldName}/resolve", PlatformAction.REFERENCE);
        assertRoute(DynamicRecordWebController.class.getMethod("checkDuplicate",
                        String.class, DynamicWebDuplicateCheckRequest.class),
                POST.class, "/{actionCode}/duplicate/check", null);
    }

    @Test
    void shouldExposePermissionScopedModuleDescriptorAndOpenApi() {
        when(recordService.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(actionModule()));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(recordService.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));

        DynamicModuleDescriptor descriptor;
        DynamicOpenApiDocument openApi;
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            descriptor = controller.describeModule(MODULE);
            openApi = controller.openApi(MODULE);
        }

        assertThat(descriptor.moduleAlias()).isEqualTo(MODULE);
        assertThat(descriptor.actions()).extracting(DynamicActionDescriptor::code)
                .doesNotContain("submit")
                .contains("view");
        assertThat(descriptor.entities()).singleElement()
                .satisfies(entity -> assertThat(entity.actions()).extracting(DynamicActionDescriptor::code)
                        .doesNotContain("submit")
                        .contains("view"));
        assertThat(openApi.moduleAlias()).isEqualTo(MODULE);
        assertThat(openApi.operations()).extracting(operation -> operation.path())
                .doesNotContain("/" + MODULE + "/submit/{recordId}")
                .contains("/" + MODULE + "/view/{id}");
        verify(tenantService, org.mockito.Mockito.times(2)).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldQueryMainEntityWithPageAndSortContract() {
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        Criteria criteria = Criteria.of().eq("code", "C-001");
        when(mainEntity.queryCriteria(any())).thenReturn(criteria);
        when(mainEntity.pageQuery(eq(criteria), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(record), 1, PageRequest.of(2, 30)));

        WebPageResponse<DynamicRecord> response = inDynamicRequest(() -> controller.query(new WebQueryRequest(
                new WebPageRequest(2, 30),
                List.of(new WebQueryCondition("code", "EQ", List.of("C-001"))),
                List.of(new WebSort("amount", true))
        )));

        assertThat(response.records()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getId()).isEqualTo("contract-1");
                    assertThat(item.getValue("code")).isEqualTo("C-001");
                });
        assertThat(response.total()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DynamicQueryCondition>> conditions = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        ArgumentCaptor<Sort[]> sorts = ArgumentCaptor.forClass(Sort[].class);
        verify(mainEntity).queryCriteria(conditions.capture());
        verify(mainEntity).pageQuery(eq(criteria), page.capture(), sorts.capture());
        assertThat(conditions.getValue()).singleElement()
                .satisfies(condition -> {
                    assertThat(condition.fieldName()).isEqualTo("code");
                    assertThat(condition.values()).isEqualTo(List.of("C-001"));
                });
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
        assertThat(sorts.getValue()).singleElement()
                .satisfies(sort -> {
                    assertThat(sort.getField()).isEqualTo("amount");
                    assertThat(sort.getDirection()).isEqualTo(SortDirection.DESC);
                });
        verify(tenantService).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldPreviewBusinessCodesWithoutPersistingRecord() {
        when(codeBusinessPreviewService.preview(eq(MODULE), eq(ENTITY), any(), eq(null), eq(null), eq(null)))
                .thenReturn(List.of(new CodeBusinessPreviewItem(
                        "rule-1",
                        "field-1",
                        "code",
                        CodeFieldRole.PRIMARY,
                        "SO-A0001",
                        null,
                        "2026-06-08T10:00:00"
                )));
        DynamicRecord draft = new DynamicRecord(entity()).setValue("code", "draft");

        List<CodeBusinessPreviewItem> response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            response = controller.previewCode(MODULE, draft);
        }

        assertThat(response).singleElement()
                .satisfies(item -> {
                    assertThat(item.fieldName()).isEqualTo("code");
                    assertThat(item.value()).isEqualTo("SO-A0001");
                });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> context = ArgumentCaptor.forClass(Map.class);
        verify(codeBusinessPreviewService).preview(eq(MODULE), eq(ENTITY), context.capture(), eq(null), eq(null), eq(null));
        assertThat(context.getValue()).containsEntry("code", "draft");
        verifyNoInteractions(mainEntity);
        verify(tenantService).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldPreviewFormulaThroughCreateActionEndpoint() throws Exception {
        DynamicRecord calculated = new DynamicRecord(entity()).setValue("amount", BigDecimal.valueOf(30));
        when(recordService.previewFormula(eq(MODULE), eq(ENTITY), any(DynamicRecord.class)))
                .thenReturn(new DynamicFormulaPreviewResult(calculated, null, List.of("amount")));
        DynamicFormulaPreviewRequest request = new DynamicFormulaPreviewRequest(new DynamicRecordPayload(
                null,
                null,
                Map.of("code", "draft", "amount", 15),
                Map.of()
        ));

        DynamicFormulaPreviewResponse response;
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            response = controller.previewFormula(MODULE, request);
        }

        assertThat(response.record().values()).containsEntry("amount", BigDecimal.valueOf(30));
        assertThat(response.changedFields()).containsExactly("amount");
        ArgumentCaptor<DynamicRecord> record = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(recordService).previewFormula(eq(MODULE), eq(ENTITY), record.capture());
        assertThat(record.getValue().getValue("code")).isEqualTo("draft");
        assertThat(record.getValue().getValue("amount")).isEqualTo(15);
        Method method = DynamicRecordWebController.class.getDeclaredMethod("previewFormula",
                String.class, DynamicFormulaPreviewRequest.class);
        assertThat(method.getAnnotation(ActionEndpoint.class).value()).isEqualTo(PlatformAction.CREATE);
    }

    @Test
    void shouldExecuteDynamicRecordActionWithPathRecordId() {
        DynamicActionDescriptor action = action("submit", EntityActionLevel.RECORD);
        when(recordService.action(MODULE, "submit")).thenReturn(action);
        when(recordService.actionEntityAlias(MODULE, "submit")).thenReturn(ENTITY);
        when(recordService.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(new DynamicActionExecutionContext(
                        MODULE, ENTITY, "submit", action, "contract-1", "trace-1", "tenant_a",
                        false, DynamicActionAvailability.available("submit")
                ), null, DynamicActionResultBody.notice("submitted")));

        DynamicWebActionExecutionResponse response = inDynamicRequest(() ->
                controller.executeRecordAction("submit", "contract-1", DynamicWebActionRequest.empty()));

        assertThat(response.context().moduleAlias()).isEqualTo(MODULE);
        assertThat(response.context().recordId()).isEqualTo("contract-1");
        assertThat(response.body().message()).isEqualTo("submitted");
        ArgumentCaptor<DynamicActionExecutionRequest> request = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(recordService).executeAction(eq(MODULE), eq("submit"), request.capture());
        assertThat(request.getValue().recordId()).isEqualTo("contract-1");
    }

    @Test
    void shouldResolveReferenceWithRequestContract() {
        DynamicReferenceResolveResponse expected = new DynamicReferenceResolveResponse(
                DynamicReferenceResolveStatus.OK,
                DynamicReferenceResolveMode.QUERY,
                List.of(),
                List.of(),
                0,
                20,
                0
        );
        when(recordService.reference(MODULE, ENTITY, "customerId"))
                .thenReturn(reference("customerId", null));
        when(recordService.resolveFieldReference(eq(MODULE), eq(ENTITY), eq("customerId"),
                any(DynamicReferenceResolveRequest.class))).thenReturn(expected);

        DynamicReferenceResolveResponse response = inDynamicRequest(() ->
                controller.resolveReference("customerId", new DynamicWebReferenceRequest(
                        DynamicReferenceResolveMode.QUERY,
                        DynamicReferenceMatchMode.AUTO,
                        "Acme",
                        List.of(),
                        List.of(),
                        null,
                        WebPageRequest.DEFAULT,
                        true,
                        Map.of("code", "C-001"),
                        null,
                        null,
                        null,
                        Map.of()
                )));

        assertThat(response).isSameAs(expected);
        ArgumentCaptor<DynamicReferenceResolveRequest> request = ArgumentCaptor.forClass(DynamicReferenceResolveRequest.class);
        verify(recordService).resolveFieldReference(eq(MODULE), eq(ENTITY), eq("customerId"), request.capture());
        assertThat(request.getValue().mode()).isEqualTo(DynamicReferenceResolveMode.QUERY);
        assertThat(request.getValue().matchMode()).isEqualTo(DynamicReferenceMatchMode.AUTO);
        assertThat(request.getValue().fuzzy()).isEqualTo("Acme");
        assertThat(request.getValue().formValues()).containsEntry("code", "C-001");
    }

    @Test
    void shouldExposeDuplicateCheckThroughActionScopedPathWithoutStaticEndpoint() throws Exception {
        when(recordService.action(MODULE, "duplicate_contract"))
                .thenReturn(action("duplicate_contract", EntityActionLevel.RECORD));
        when(duplicateCheckService.check(eq(MODULE), eq("duplicate_contract"), eq("contract-1"), any()))
                .thenReturn(new RecordDuplicateCheckResult(
                        "rule-1",
                        "duplicate_contract",
                        List.of("code"),
                        true,
                        List.of(new RecordDuplicateMatch("contract-2", 5, Map.of("code", "C-001")))));

        RecordDuplicateCheckResult result = inDynamicRequest(() -> controller.checkDuplicate(
                "duplicate_contract",
                new DynamicWebDuplicateCheckRequest("contract-1", Map.of("code", "C-001"))
        ));

        assertThat(result.duplicated()).isTrue();
        assertThat(result.matches()).singleElement()
                .satisfies(match -> assertThat(match.recordId()).isEqualTo("contract-2"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> values = ArgumentCaptor.forClass(Map.class);
        verify(duplicateCheckService).check(eq(MODULE), eq("duplicate_contract"), eq("contract-1"), values.capture());
        assertThat(values.getValue()).containsEntry("code", "C-001");
        Method method = DynamicRecordWebController.class.getMethod(
                "checkDuplicate", String.class, DynamicWebDuplicateCheckRequest.class);
        assertThat(method.getAnnotation(ActionEndpoint.class)).isNull();
    }

    private <T> T inDynamicRequest(java.util.function.Supplier<T> action) {
        DynamicWebRequest.useRequestPath("/" + MODULE + "/query");
        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            return action.get();
        }
    }

    private void assertRoute(Method method, Class<?> httpMethod, String path, PlatformAction action) {
        assertRoute(method, method, httpMethod, path, action);
    }

    private void assertRoute(Method method, Method routeContract, Class<?> httpMethod, String path, PlatformAction action) {
        assertThat(routeContract.getAnnotation(httpMethod.asSubclass(Annotation.class))).isNotNull();
        assertThat(routeContract.getAnnotation(Path.class).value()).isEqualTo(path);
        ActionEndpoint endpoint = method.getAnnotation(ActionEndpoint.class);
        if (action == null) {
            assertThat(endpoint).isNull();
        } else {
            assertThat(endpoint).isNotNull();
            assertThat(endpoint.value()).isEqualTo(action);
        }
    }

    private ModuleDefinition module() {
        return new ModuleDefinition(MODULE, "Contract", List.of(entity()));
    }

    private ModuleDefinition actionModule() {
        return new ModuleDefinition(
                MODULE,
                "Contract",
                List.of(entity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        EntityActionDefinition.enabled(ENTITY, "view", "View"),
                        new EntityActionDefinition(ENTITY, "submit", "Submit", true, EntityActionLevel.RECORD,
                                EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                                true, false, null, null, null,
                                EntityActionExecutorType.SERVICE, "submitExecutor")
                )
        );
    }

    private EntityDefinition entity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                FieldDefinition.of("signedDate", FieldType.DATE, "Signed Date").column("signed_date")
        ));
    }

    private DynamicActionDescriptor action(String code, EntityActionLevel level) {
        return new DynamicActionDescriptor(code, "Submit", true, level, EntityActionCategory.CUSTOM,
                EntityActionAccessMode.AUTH_REQUIRED, true, false, null, false, null,
                EntityActionExecutorType.SERVICE, "submitExecutor").withPermission(MODULE);
    }

    private DynamicReferenceDescriptor reference(String sourceField, String queryTemplateId) {
        return new DynamicReferenceDescriptor(
                ENTITY,
                sourceField,
                "crm.customer",
                "customer",
                ReferenceCardinality.ONE,
                true,
                null,
                List.of(),
                "id",
                "title",
                null,
                queryTemplateId,
                Set.of(),
                List.of(),
                List.of()
        );
    }
}
