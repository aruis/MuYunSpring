package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionKind;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DynamicRecordWebControllerTest {
    private static final String MODULE = "sales.contract";
    private static final String ENTITY = "contract";
    private final ObjectMapper objectMapper = new ObjectMapper();

    private DynamicRecordService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DynamicRecordService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new DynamicRecordWebController(service))
                .build();
    }

    @Test
    void shouldExposeModuleDescriptor() throws Exception {
        when(service.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(module()));

        mvc.perform(post("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.entities[0].entityAlias").value(ENTITY));
    }

    @Test
    void shouldNotCaptureRootFileLikePath() throws Exception {
        mvc.perform(get("/openapi.json"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    void shouldAllowStaticControllerToTakeOverSameAliasUrl() throws Exception {
        MockMvc takeoverMvc = MockMvcBuilders
                .standaloneSetup(new DynamicRecordWebController(service), new StaticContractController())
                .build();

        takeoverMvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("static"));
        verifyNoInteractions(service);

        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.select(MODULE, ENTITY, "contract-1")).thenReturn(record);

        takeoverMvc.perform(post("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"));
    }

    @Test
    void shouldCreateAndUpdateMainEntityThroughAliasRootContract() throws Exception {
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.create(eq(MODULE), eq(ENTITY), any(DynamicRecord.class))).thenReturn("contract-1");
        when(service.update(eq(MODULE), eq(ENTITY), any(DynamicRecord.class))).thenReturn(1);

        mvc.perform(post("/{moduleAlias}/insert", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("code", "C-001", "amount", 12)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("contract-1"));

        ArgumentCaptor<DynamicRecord> createRecord = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(service).create(eq(MODULE), eq(ENTITY), createRecord.capture());
        assertThat(createRecord.getValue().getValue("code")).isEqualTo("C-001");
        assertThat(createRecord.getValue().getValue("amount")).isEqualTo(12);

        mvc.perform(post("/{moduleAlias}/update/{recordId}", MODULE, "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of("version", 3, "values", Map.of("amount", BigDecimal.TEN)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        ArgumentCaptor<DynamicRecord> updateRecord = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(service).update(eq(MODULE), eq(ENTITY), updateRecord.capture());
        assertThat(updateRecord.getValue().getId()).isEqualTo("contract-1");
        assertThat(updateRecord.getValue().getVersion()).isEqualTo(3);
        assertThat(updateRecord.getValue().getValue("amount")).isEqualTo(10);
    }

    @Test
    void shouldBuildMainEntityQueryCriteriaAndReturnPageResponse() throws Exception {
        Criteria criteria = Criteria.of().eq("code", "C-001");
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.queryCriteria(eq(MODULE), eq(ENTITY), any())).thenReturn(criteria);
        when(service.page(eq(MODULE), eq(ENTITY), eq(criteria), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(record), 1, PageRequest.of(2, 30)));

        mvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "conditions", List.of(Map.of(
                                        "fieldName", "code",
                                        "operator", "EQ",
                                        "values", List.of("C-001")
                                )),
                                "page", Map.of("pageNum", 2, "pageSize", 30),
                                "sorts", List.of(Map.of("field", "amount", "desc", true))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("contract-1"))
                .andExpect(jsonPath("$.records[0].values.code").value("C-001"))
                .andExpect(jsonPath("$.total").value(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DynamicQueryCondition>> conditions = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        ArgumentCaptor<Sort[]> sorts = ArgumentCaptor.forClass(Sort[].class);
        verify(service).queryCriteria(eq(MODULE), eq(ENTITY), conditions.capture());
        verify(service).page(eq(MODULE), eq(ENTITY), eq(criteria), page.capture(), sorts.capture());
        assertThat(conditions.getValue().getFirst().operator()).isEqualTo(DynamicQueryOperator.EQ);
        assertThat(conditions.getValue().getFirst().values()).isEqualTo(List.of("C-001"));
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
        assertThat(sorts.getValue()[0].getField()).isEqualTo("amount");
    }

    @Test
    void shouldExposeMainEntityViewDeleteAndActionsThroughAliasRootContract() throws Exception {
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.select(MODULE, ENTITY, "contract-1")).thenReturn(record);
        when(service.delete(MODULE, ENTITY, "contract-1")).thenReturn(1);
        when(service.actions(MODULE)).thenReturn(List.of(
                action("export", EntityActionLevel.LIST),
                action("submit", EntityActionLevel.RECORD),
                action("archive", EntityActionLevel.BATCH)
        ));

        mvc.perform(post("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.values.code").value("C-001"));

        mvc.perform(post("/{moduleAlias}/delete/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(post("/{moduleAlias}/actions", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("export"))
                .andExpect(jsonPath("$[1].code").value("submit"))
                .andExpect(jsonPath("$[2].code").value("archive"));

        verify(service, times(2)).mainEntityAlias(MODULE);
        verify(service).select(MODULE, ENTITY, "contract-1");
        verify(service).delete(MODULE, ENTITY, "contract-1");
        verify(service).actions(MODULE);
    }

    @Test
    void shouldExposeRecordActionAvailabilityWithoutListAndBatchActions() throws Exception {
        DynamicActionDescriptor export = action("export", EntityActionLevel.LIST);
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        DynamicActionDescriptor preview = action("preview", EntityActionLevel.ANY);
        DynamicActionDescriptor archive = action("archive", EntityActionLevel.BATCH);
        DynamicRecord existing = new DynamicRecord(entity()).setValue("code", "C-001");
        existing.setId("contract-1");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.select(MODULE, ENTITY, "contract-1")).thenReturn(existing);
        when(service.actions(MODULE)).thenReturn(List.of(export, submit, preview, archive));
        when(service.actionAvailability(eq(MODULE), eq("submit"), any(DynamicRecord.class)))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "只有草稿合同可以提交"));
        when(service.actionAvailability(eq(MODULE), eq("preview"), any(DynamicRecord.class)))
                .thenReturn(DynamicActionAvailability.available("preview"));

        mvc.perform(post("/{moduleAlias}/view/{recordId}/actions", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action.code").value("submit"))
                .andExpect(jsonPath("$[0].available").value(false))
                .andExpect(jsonPath("$[0].message").value("只有草稿合同可以提交"))
                .andExpect(jsonPath("$[1].action.code").value("preview"))
                .andExpect(jsonPath("$[1].available").value(true))
                .andExpect(jsonPath("$[2]").doesNotExist());

        ArgumentCaptor<DynamicRecord> record = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(service).actionAvailability(eq(MODULE), eq("submit"), record.capture());
        assertThat(record.getValue().getId()).isEqualTo("contract-1");
    }

    @Test
    void shouldRejectRecordActionAvailabilityWhenRecordDoesNotExist() throws Exception {
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);

        mvc.perform(post("/{moduleAlias}/view/{recordId}/actions", MODULE, "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic record does not exist: missing"));
    }

    @Test
    void shouldExecuteActionWithRecordPayload() throws Exception {
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        when(service.action(MODULE, "submit")).thenReturn(submit);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "submit", submit,
                                "contract-1", "trace-1", "tenant-1", false,
                                DynamicActionAvailability.available("submit")),
                        "ok",
                        new DynamicActionResultBody(DynamicActionResultType.VALUE, "ok", null, true, null)));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "record", Map.of("values", Map.of("code", "C-001")),
                                "conditions", List.of(Map.of(
                                        "fieldName", "code",
                                        "operator", "EQ",
                                        "values", List.of("C-001")
                                )),
                                "payload", Map.of("comment", "submit")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.context.actionCode").value("submit"))
                .andExpect(jsonPath("$.context.recordId").value("contract-1"))
                .andExpect(jsonPath("$.context.traceId").value("trace-1"))
                .andExpect(jsonPath("$.context.entityAlias").doesNotExist())
                .andExpect(jsonPath("$.context.action").doesNotExist())
                .andExpect(jsonPath("$.body.value").value("ok"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        ArgumentCaptor<DynamicActionExecutionRequest> request = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq("submit"), request.capture());
        assertThat(request.getValue().recordId()).isEqualTo("contract-1");
        assertThat(request.getValue().record().getValue("code")).isEqualTo("C-001");
        assertThat(request.getValue().queryConditions().iterator().next().fieldName()).isEqualTo("code");
        assertThat(request.getValue().payload()).containsEntry("comment", "submit");
    }

    @Test
    void shouldRejectActionRecordIdMismatch() throws Exception {
        when(service.action(MODULE, "submit")).thenReturn(action("submit", EntityActionLevel.RECORD));
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "record", Map.of("id", "contract-2", "values", Map.of("code", "C-001"))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("action request recordId must match record.id"));
    }

    @Test
    void shouldRejectActionsThatWouldExposeInternalCriteria() throws Exception {
        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "queryCriteria")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic web action is not exposed: queryCriteria"));

        Criteria criteria = Criteria.of().eq("code", "C-001");
        when(service.action(MODULE, "customCriteria")).thenReturn(action("customCriteria", EntityActionLevel.ANY));
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("customCriteria"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, criteria, DynamicActionResultBody.of(criteria)));
        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "customCriteria")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic web response does not expose internal Criteria"));
    }

    @Test
    void shouldExecuteListAndBatchActionsThroughStaticLikePaths() throws Exception {
        when(service.action(MODULE, "export")).thenReturn(action("export", EntityActionLevel.LIST));
        when(service.action(MODULE, "archive")).thenReturn(action("archive", EntityActionLevel.BATCH));
        when(service.action(MODULE, "refreshSelected")).thenReturn(action("refreshSelected", EntityActionLevel.ANY));
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("export"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, "ok",
                        new DynamicActionResultBody(DynamicActionResultType.VALUE, "ok", null, false, null)));
        when(service.executeAction(eq(MODULE), eq("archive"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, 2,
                        new DynamicActionResultBody(DynamicActionResultType.COUNT, 2, null, true, null)));
        when(service.executeAction(eq(MODULE), eq("refreshSelected"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, "ok",
                        new DynamicActionResultBody(DynamicActionResultType.VALUE, "ok", null, true, null)));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "export")
                        .contentType("application/json")
                        .content(json(Map.of("payload", Map.of("format", "xlsx")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.value").value("ok"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "archive")
                        .contentType("application/json")
                        .content(json(Map.of("ids", List.of("contract-1", "contract-2")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.value").value(2));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "refreshSelected")
                        .contentType("application/json")
                        .content(json(Map.of("ids", List.of("contract-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.value").value("ok"));

        ArgumentCaptor<DynamicActionExecutionRequest> batchRequest = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq("archive"), batchRequest.capture());
        assertThat(batchRequest.getValue().ids()).containsExactly("contract-1", "contract-2");
    }

    @Test
    void shouldRejectActionPathWhenLevelDoesNotMatch() throws Exception {
        when(service.action(MODULE, "submit")).thenReturn(action("submit", EntityActionLevel.RECORD));
        when(service.action(MODULE, "export")).thenReturn(action("export", EntityActionLevel.LIST));
        when(service.action(MODULE, "archive")).thenReturn(action("archive", EntityActionLevel.BATCH));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "submit")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support list path: submit"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "export", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support record path: export"));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "archive")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support list path: archive"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "export")
                        .contentType("application/json")
                        .content(json(Map.of("ids", List.of("contract-1")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support batch path: export"));
    }

    @Test
    void shouldRejectOldActionsExecutionPath() throws Exception {
        mvc.perform(post("/{moduleAlias}/actions/{actionCode}", MODULE, "submit")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action path is reserved: actions"));
    }

    @Test
    void shouldResolveMainEntityReferenceWithoutEntityLevelPath() throws Exception {
        Criteria criteria = Criteria.of().eq("customerType", "VIP");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.queryCriteria(eq(MODULE), eq(ENTITY), any())).thenReturn(criteria);
        when(service.resolveFieldReference(eq(MODULE), eq(ENTITY), eq("customerId"), any(DynamicReferenceResolveRequest.class)))
                .thenReturn(new DynamicReferenceResolveResponse(
                        DynamicReferenceResolveStatus.OK,
                        DynamicReferenceResolveMode.QUERY,
                        List.of(),
                        List.of(),
                        0,
                        20,
                        0
                ));

        mvc.perform(post("/{moduleAlias}/references/{fieldName}/resolve", MODULE, "customerId")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "mode", "QUERY",
                                "fuzzy", "ximatai",
                                "includeProjections", false,
                                "conditions", List.of(Map.of(
                                        "fieldName", "customerType",
                                        "operator", "EQ",
                                        "values", List.of("VIP")
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.mode").value("QUERY"));

        ArgumentCaptor<DynamicReferenceResolveRequest> request = ArgumentCaptor.forClass(DynamicReferenceResolveRequest.class);
        verify(service).resolveFieldReference(eq(MODULE), eq(ENTITY), eq("customerId"), request.capture());
        assertThat(request.getValue().fuzzy()).isEqualTo("ximatai");
        assertThat(request.getValue().criteria()).isSameAs(criteria);
        assertThat(request.getValue().includeProjections()).isFalse();
    }

    @Test
    void shouldNotExposeEntityLevelWebApi() throws Exception {
        mvc.perform(post("/{moduleAlias}/entities/{entityAlias}/records", MODULE, ENTITY)
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("code", "C-001")))))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnStableBadRequestBody() throws Exception {
        when(service.describe(MODULE)).thenThrow(new ModuleDefinitionException("unknown module alias: " + MODULE));

        mvc.perform(post("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unknown module alias: " + MODULE));
    }

    @Test
    void shouldReturnConflictForOptimisticLockFailure() throws Exception {
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.update(eq(MODULE), eq(ENTITY), any(DynamicRecord.class)))
                .thenThrow(new OptimisticLockException("record version conflict: contract-1"));

        mvc.perform(post("/{moduleAlias}/update/{recordId}", MODULE, "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("code", "C-001")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("record version conflict: contract-1"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private ModuleDefinition module() {
        return new ModuleDefinition(MODULE, "Contract", List.of(entity()));
    }

    private DynamicActionDescriptor action(String code, EntityActionLevel level) {
        return new DynamicActionDescriptor(code, DynamicActionKind.CUSTOM, "Submit", true,
                EntityActionStyle.PRIMARY, level, EntityActionCategory.CUSTOM,
                EntityActionAccessMode.AUTH_REQUIRED, true, false, null, false, null,
                EntityActionExecutorType.SERVICE, "submitExecutor");
    }

    private EntityDefinition entity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
        ));
    }

    @RestController
    @RequestMapping("/sales.contract")
    static class StaticContractController {
        @PostMapping("/query")
        Map<String, String> query() {
            return Map.of("source", "static");
        }
    }
}
