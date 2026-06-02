package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        mvc.perform(get("/api/dynamic/modules/{moduleAlias}", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.entities[0].entityAlias").value(ENTITY));
    }

    @Test
    void shouldCreateAndUpdateRecordsThroughRuntimeService() throws Exception {
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.create(eq(MODULE), eq(ENTITY), any(DynamicRecord.class))).thenReturn("contract-1");
        when(service.update(eq(MODULE), eq(ENTITY), any(DynamicRecord.class))).thenReturn(1);

        mvc.perform(post("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/records", MODULE, ENTITY)
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("code", "C-001", "amount", 12)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("contract-1"));

        ArgumentCaptor<DynamicRecord> createRecord = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(service).create(eq(MODULE), eq(ENTITY), createRecord.capture());
        assertThat(createRecord.getValue().getValue("code")).isEqualTo("C-001");
        assertThat(createRecord.getValue().getValue("amount")).isEqualTo(12);

        mvc.perform(put("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/records/{recordId}",
                        MODULE, ENTITY, "contract-1")
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
    void shouldExposeStableRecordResponseWithoutRuntimeInternals() throws Exception {
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        record.setVersion(2);
        when(service.select(MODULE, ENTITY, "contract-1")).thenReturn(record);

        mvc.perform(get("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/records/{recordId}",
                        MODULE, ENTITY, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.values.code").value("C-001"))
                .andExpect(jsonPath("$.entity").doesNotExist())
                .andExpect(jsonPath("$.explicitFieldCodes").doesNotExist());
    }

    @Test
    void shouldBuildQueryCriteriaAndPageRequest() throws Exception {
        Criteria criteria = Criteria.of().eq("code", "C-001");
        when(service.queryCriteria(eq(MODULE), eq(ENTITY), any())).thenReturn(criteria);
        when(service.list(eq(MODULE), eq(ENTITY), eq(criteria), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(List.of());

        mvc.perform(post("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/query", MODULE, ENTITY)
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
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DynamicQueryCondition>> conditions = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        ArgumentCaptor<Sort[]> sorts = ArgumentCaptor.forClass(Sort[].class);
        verify(service).queryCriteria(eq(MODULE), eq(ENTITY), conditions.capture());
        verify(service).list(eq(MODULE), eq(ENTITY), eq(criteria), page.capture(), sorts.capture());
        assertThat(conditions.getValue().getFirst().operator()).isEqualTo(DynamicQueryOperator.EQ);
        assertThat(conditions.getValue().getFirst().values()).isEqualTo(List.of("C-001"));
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
        assertThat(sorts.getValue()[0].getField()).isEqualTo("amount");
    }

    @Test
    void shouldExecuteActionWithRecordPayload() throws Exception {
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.executeAction(eq(MODULE), eq(ENTITY), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, "ok",
                        new DynamicActionResultBody(DynamicActionResultType.VALUE, "ok", null, true, null)));

        mvc.perform(post("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/actions/{actionCode}",
                        MODULE, ENTITY, "submit")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "recordId", "contract-1",
                                "record", Map.of("values", Map.of("code", "C-001")),
                                "conditions", List.of(Map.of(
                                        "fieldName", "code",
                                        "operator", "EQ",
                                        "values", List.of("C-001")
                                )),
                                "payload", Map.of("comment", "submit")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.value").value("ok"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        ArgumentCaptor<DynamicActionExecutionRequest> request = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq(ENTITY), eq("submit"), request.capture());
        assertThat(request.getValue().recordId()).isEqualTo("contract-1");
        assertThat(request.getValue().record().getValue("code")).isEqualTo("C-001");
        assertThat(request.getValue().queryConditions().iterator().next().fieldName()).isEqualTo("code");
        assertThat(request.getValue().payload()).containsEntry("comment", "submit");
    }

    @Test
    void shouldRejectActionsThatWouldExposeInternalCriteria() throws Exception {
        mvc.perform(post("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/actions/{actionCode}",
                        MODULE, ENTITY, "queryCriteria")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic web action is not exposed: queryCriteria"));

        Criteria criteria = Criteria.of().eq("code", "C-001");
        when(service.executeAction(eq(MODULE), eq(ENTITY), eq("customCriteria"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, criteria, DynamicActionResultBody.of(criteria)));
        mvc.perform(post("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/actions/{actionCode}",
                        MODULE, ENTITY, "customCriteria")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic web response does not expose internal Criteria"));
    }

    @Test
    void shouldReturnStableBadRequestBody() throws Exception {
        when(service.describe(MODULE)).thenThrow(new ModuleDefinitionException("unknown module alias: " + MODULE));

        mvc.perform(get("/api/dynamic/modules/{moduleAlias}", MODULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unknown module alias: " + MODULE));
    }

    @Test
    void shouldReturnConflictForOptimisticLockFailure() throws Exception {
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.update(eq(MODULE), eq(ENTITY), any(DynamicRecord.class)))
                .thenThrow(new OptimisticLockException("record version conflict: contract-1"));

        mvc.perform(put("/api/dynamic/modules/{moduleAlias}/entities/{entityAlias}/records/{recordId}",
                        MODULE, ENTITY, "contract-1")
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

    private EntityDefinition entity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
        ));
    }
}
