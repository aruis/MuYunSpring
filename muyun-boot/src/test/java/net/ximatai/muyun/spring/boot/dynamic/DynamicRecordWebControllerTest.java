package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.security.FieldEncryptionMode;
import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.common.security.FieldSignatureMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiDocument;
import net.ximatai.muyun.spring.dynamic.openapi.DynamicOpenApiGenerator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionResult;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionDialog;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionResultBody;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceMatchMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveItem;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveMode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveRequest;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveResponse;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceResolveStatus;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewItem;
import net.ximatai.muyun.spring.platform.code.CodeBusinessPreviewService;
import net.ximatai.muyun.spring.platform.code.CodeFieldRole;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationCommitResult;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationDraft;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationResult;
import net.ximatai.muyun.spring.platform.generation.ReferenceRecordGenerationFacade;
import net.ximatai.muyun.spring.platform.impact.RecordImpactType;
import net.ximatai.muyun.spring.platform.impact.RecordOriginContext;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshotService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryItemService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    private DynamicEntityOperations mainEntity;
    private ActiveTenantVerifier activeTenantVerifier;
    private CodeBusinessPreviewService codeBusinessPreviewService;
    private ReferenceRecordGenerationFacade referenceGenerationFacade;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(DynamicRecordService.class);
        mainEntity = mock(DynamicEntityOperations.class);
        activeTenantVerifier = mock(ActiveTenantVerifier.class);
        codeBusinessPreviewService = mock(CodeBusinessPreviewService.class);
        referenceGenerationFacade = mock(ReferenceRecordGenerationFacade.class);
        when(service.mainEntity(MODULE)).thenReturn(mainEntity);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(mainEntity.newRecord()).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.actionAuthorizationAvailability(eq(MODULE), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(1)));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), anyString(), any()))
                .thenAnswer(invocation -> DynamicActionAvailability.available(invocation.getArgument(2)));
        objectMapper.registerModule(new DynamicRecordJacksonConfiguration()
                .dynamicRecordJacksonModule(service));
        mvc = MockMvcBuilders
                .standaloneSetup(new DynamicRecordWebController(service, activeTenantVerifier,
                        codeBusinessPreviewService, referenceGenerationFacade))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
    }

    @Test
    void shouldExposeModuleDescriptor() throws Exception {
        when(service.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(module()));

        mvc.perform(get("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.mainEntityAlias").value(ENTITY))
                .andExpect(jsonPath("$.entities[0].entityAlias").value(ENTITY));
        verify(activeTenantVerifier).verifyActiveTenant("tenant_a");
    }

    @Test
    void shouldExposeDynamicOpenApiDocument() throws Exception {
        when(service.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(module()));

        mvc.perform(get("/{moduleAlias}/openapi", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.basePath").value("/" + MODULE))
                .andExpect(jsonPath("$.operations[0].path").value("/" + MODULE + "/describe"))
                .andExpect(jsonPath("$.schemas.ContractRecord.type").value("object"));
    }

    @Test
    void shouldPreviewBusinessCodesWithoutPersistingRecord() throws Exception {
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

        mvc.perform(post("/{moduleAlias}/code/preview", MODULE)
                        .contentType("application/json")
                        .content("""
                                {
                                  "values": {
                                    "code": "draft"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleId").value("rule-1"))
                .andExpect(jsonPath("$[0].fieldName").value("code"))
                .andExpect(jsonPath("$[0].value").value("SO-A0001"))
                .andExpect(jsonPath("$[0].effectiveAt").value("2026-06-08T10:00:00"));

        ArgumentCaptor<Map<String, Object>> contextCaptor = ArgumentCaptor.forClass(Map.class);
        verify(codeBusinessPreviewService).preview(eq(MODULE), eq(ENTITY), contextCaptor.capture(), eq(null),
                eq(null), eq(null));
        assertThat(contextCaptor.getValue()).containsEntry("code", "draft");
        verify(mainEntity, times(1)).newRecord();
    }

    @Test
    void shouldHideUnauthorizedActionsFromRuntimeDescriptor() throws Exception {
        when(service.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(actionModule()));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq("delete"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("delete", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("delete"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("delete", "action permission denied"));

        mvc.perform(get("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions[?(@.code == 'submit')]").isEmpty())
                .andExpect(jsonPath("$.actions[?(@.code == 'delete')]").isEmpty())
                .andExpect(jsonPath("$.entities[0].actions[?(@.code == 'submit')]").isEmpty())
                .andExpect(jsonPath("$.entities[0].actions[?(@.code == 'delete')]").isEmpty())
                .andExpect(jsonPath("$.actions[?(@.code == 'view')]").isNotEmpty())
                .andExpect(jsonPath("$.entities[0].actions[?(@.code == 'view')]").isNotEmpty());
    }

    @Test
    void shouldHideUnauthorizedActionPathsFromRuntimeOpenApi() throws Exception {
        when(service.describe(MODULE)).thenReturn(DynamicModuleDescriptor.from(actionModule()));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq("delete"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("delete", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("delete"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("delete", "action permission denied"));

        mvc.perform(get("/{moduleAlias}/openapi", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operations[?(@.path == '/sales.contract/submit/{recordId}')]").isEmpty())
                .andExpect(jsonPath("$.operations[?(@.path == '/sales.contract/delete/{id}')]").isEmpty())
                .andExpect(jsonPath("$.operations[?(@.path == '/sales.contract/view/{id}')]").isNotEmpty());
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
                .standaloneSetup(new DynamicRecordWebController(service, activeTenantVerifier),
                        new StaticContractController())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();

        takeoverMvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("static"));
        verifyNoInteractions(service);

        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(service.mainEntity(MODULE)).thenReturn(mainEntity);
        when(mainEntity.select("contract-1")).thenReturn(record);

        takeoverMvc.perform(get("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"));
    }

    @Test
    void shouldCreateAndUpdateMainEntityThroughAliasRootContract() throws Exception {
        DynamicRecord created = new DynamicRecord(entity()).setValue("code", "C-001").setValue("amount", 12);
        created.setId("contract-1");
        DynamicRecord updated = new DynamicRecord(entity()).setValue("amount", BigDecimal.TEN);
        updated.setId("contract-1");
        updated.setVersion(4);
        when(service.relations(MODULE)).thenReturn(List.of(
                new DynamicRelationDescriptor("lines", ENTITY, "contract_line", "contractId", false, false)
        ));
        when(service.newRecord(MODULE, "contract_line")).thenAnswer(invocation -> new DynamicRecord(lineEntity()));
        when(mainEntity.insert(any(DynamicRecord.class))).thenReturn("contract-1");
        when(mainEntity.select("contract-1")).thenReturn(created, updated);
        when(mainEntity.update(any(DynamicRecord.class))).thenReturn(1);

        mvc.perform(post("/{moduleAlias}/insert", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "originContext", Map.of(
                                        "impactType", "GENERATE_PUSH",
                                        "sourceModuleAlias", "sales.opportunity",
                                        "sourceRecordId", "opp-1",
                                        "targetModuleAlias", MODULE,
                                        "generationRuleId", "rule-1",
                                        "actionCode", "generateContract",
                                        "batchId", "batch-1",
                                        "draftKey", "draft-1"),
                                "values", Map.of("code", "C-001", "amount", 12),
                                "children", Map.of("lines", List.of(Map.of(
                                        "values", Map.of("lineNo", "L-001", "lineAmount", 7))))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.values.code").value("C-001"));

        ArgumentCaptor<DynamicRecord> createRecord = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(mainEntity).insert(createRecord.capture());
        assertThat(createRecord.getValue().getValue("code")).isEqualTo("C-001");
        assertThat(createRecord.getValue().getValue("amount")).isEqualTo(12);
        assertThat(createRecord.getValue().getValues()).doesNotContainKey("originContext");
        assertThat(createRecord.getValue().mutationMetadata()).containsKey("originContext");
        assertThat(createRecord.getValue().getChildren("lines")).singleElement()
                .satisfies(line -> {
                    assertThat(line.getValue("lineNo")).isEqualTo("L-001");
                    assertThat(line.getValue("lineAmount")).isEqualTo(7);
                });

        mvc.perform(post("/{moduleAlias}/update/{recordId}", MODULE, "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of("version", 3, "values", Map.of("amount", BigDecimal.TEN)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.version").value(4));

        ArgumentCaptor<DynamicRecord> updateRecord = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(mainEntity).update(updateRecord.capture());
        assertThat(updateRecord.getValue().getId()).isEqualTo("contract-1");
        assertThat(updateRecord.getValue().getVersion()).isEqualTo(3);
        assertThat(updateRecord.getValue().getValue("amount")).isEqualTo(10);
    }

    @Test
    void shouldRejectUnknownDynamicChildRelationInRequest() throws Exception {
        when(service.relations(MODULE)).thenReturn(List.of(
                new DynamicRelationDescriptor("lines", ENTITY, "contract_line", "contractId", false, false)
        ));
        Map<String, Object> body = Map.of(
                "values", Map.of("code", "C-001"),
                "children", Map.of("unknownLines", List.of(Map.of(
                        "values", Map.of("lineNo", "L-001")))));

        mvc.perform(post("/{moduleAlias}/insert", MODULE)
                        .contentType("application/json")
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("unknown dynamic child relation: unknownLines"));
    }

    @Test
    void shouldRejectNonArrayDynamicChildRelationInRequest() throws Exception {
        when(service.relations(MODULE)).thenReturn(List.of(
                new DynamicRelationDescriptor("lines", ENTITY, "contract_line", "contractId", false, false)
        ));
        Map<String, Object> body = Map.of(
                "values", Map.of("code", "C-001"),
                "children", Map.of("lines", Map.of(
                        "values", Map.of("lineNo", "L-001"))));

        mvc.perform(post("/{moduleAlias}/insert", MODULE)
                        .contentType("application/json")
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic child relation must be array: lines"));
    }

    @Test
    void shouldMaskProtectedDynamicFieldsInViewResponse() throws Exception {
        DynamicRecord record = new DynamicRecord(protectedEntity())
                .setValue("code", "C-001")
                .setValue("secret", "sensitive-value");
        record.setId("contract-1");
        when(mainEntity.select("contract-1")).thenReturn(record);

        mvc.perform(get("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.code").value("C-001"))
                .andExpect(jsonPath("$.values.secret").value("s*************e"));
    }

    @Test
    void shouldBuildMainEntityQueryCriteriaAndReturnPageResponse() throws Exception {
        Criteria criteria = Criteria.of().eq("code", "C-001");
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(mainEntity.queryCriteria(any())).thenReturn(criteria);
        when(mainEntity.pageQuery(eq(criteria), any(PageRequest.class), any(Sort[].class)))
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
        verify(mainEntity).queryCriteria(conditions.capture());
        verify(mainEntity).pageQuery(eq(criteria), page.capture(), sorts.capture());
        assertThat(conditions.getValue().getFirst().operator()).isEqualTo(DynamicQueryOperator.EQ);
        assertThat(conditions.getValue().getFirst().values()).isEqualTo(List.of("C-001"));
        assertThat(page.getValue().getOffset()).isEqualTo(30);
        assertThat(page.getValue().getLimit()).isEqualTo(30);
        assertThat(sorts.getValue()[0].getField()).isEqualTo("amount");
    }

    @Test
    void shouldApplyLowCodeQueryTemplateAndProjectByUiConfig() throws Exception {
        PlatformPageConfigSnapshotService snapshotService = mock(PlatformPageConfigSnapshotService.class);
        PlatformQueryItemService queryItemService = mock(PlatformQueryItemService.class);
        ModuleMetadataFieldService moduleFieldService = mock(ModuleMetadataFieldService.class);
        MockMvc lowCodeMvc = MockMvcBuilders
                .standaloneSetup(new DynamicRecordWebController(service, activeTenantVerifier,
                        codeBusinessPreviewService, referenceGenerationFacade,
                        snapshotService, queryItemService, moduleFieldService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new CurrentUserWebFilter(() -> java.util.Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant_a"))))
                .build();
        PlatformUiConfig uiConfig = new PlatformUiConfig();
        uiConfig.setId("ui-list");
        uiConfig.setUiSetId("set-list");
        uiConfig.setClientType(PlatformUiClientType.WEB);
        uiConfig.setPublished(true);
        PlatformUiConfigField codeField = new PlatformUiConfigField();
        codeField.setUiConfigId("ui-list");
        codeField.setModuleMetadataFieldId("module-field-code");
        codeField.setVisible(true);
        PlatformQueryTemplate template = new PlatformQueryTemplate();
        template.setId("tpl-active");
        template.setModuleAlias(MODULE);
        template.setAlias("active");
        when(snapshotService.snapshot(MODULE)).thenReturn(new PlatformPageConfigSnapshot(
                MODULE,
                List.of(),
                List.of(uiConfig),
                List.of(codeField),
                List.of(template),
                List.of()
        ));
        when(moduleFieldService.resolve("module-field-code")).thenReturn(resolvedModuleField(
                "module-field-code", "code"));
        Criteria templateCriteria = Criteria.of().eq("code", "C-001");
        when(queryItemService.compile(eq("tpl-active"), any())).thenReturn(templateCriteria);
        DynamicRecord record = new DynamicRecord(entity())
                .setValue("code", "C-001")
                .setValue("amount", BigDecimal.TEN);
        record.setId("contract-1");
        when(mainEntity.queryCriteria(any())).thenReturn(Criteria.of().eq("amount", BigDecimal.TEN));
        when(mainEntity.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(record), 1, PageRequest.of(1, 20)));

        lowCodeMvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content("""
                                {
                                  "uiConfigId": "ui-list",
                                  "queryTemplateId": "tpl-active",
                                  "externalQueryValues": {
                                    "owner": "user-1",
                                    "optional": null
                                  },
                                  "conditions": [
                                    {
                                      "fieldName": "amount",
                                      "operator": "EQ",
                                      "values": [10]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("contract-1"))
                .andExpect(jsonPath("$.records[0].values.code").value("C-001"))
                .andExpect(jsonPath("$.records[0].values.amount").doesNotExist())
                .andExpect(jsonPath("$.total").value(1));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> externalValues = ArgumentCaptor.forClass(Map.class);
        verify(queryItemService).compile(eq("tpl-active"), externalValues.capture());
        assertThat(externalValues.getValue()).containsEntry("owner", "user-1");
        assertThat(externalValues.getValue()).containsEntry("optional", null);
        verify(mainEntity).queryCriteria(any());
        verify(snapshotService, times(2)).snapshot(MODULE);
    }

    @Test
    void shouldKeepDynamicDefaultQueryOperatorWhenWebQueryOmitsOperator() throws Exception {
        Criteria criteria = Criteria.of().like("code", "C-001");
        when(mainEntity.queryCriteria(any())).thenReturn(criteria);
        when(mainEntity.pageQuery(eq(criteria), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(), 0, PageRequest.of(1, 20)));

        mvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "conditions", List.of(Map.of(
                                        "fieldName", "code",
                                        "values", List.of("C-001")
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DynamicQueryCondition>> conditions = ArgumentCaptor.forClass(List.class);
        verify(mainEntity).queryCriteria(conditions.capture());
        assertThat(conditions.getValue().getFirst().operator()).isNull();
    }

    @Test
    void shouldExposeDateAndTimestampValuesAsStableWebStrings() throws Exception {
        Criteria criteria = Criteria.of().eq("signedDate", LocalDate.parse("2026-06-01"));
        DynamicRecord record = new DynamicRecord(entity())
                .setValue("signedDate", LocalDate.parse("2026-06-01"))
                .setValue("signedAt", Instant.parse("2026-06-01T02:03:04Z"));
        record.setId("contract-1");
        when(mainEntity.select("contract-1")).thenReturn(record);
        when(mainEntity.queryCriteria(any())).thenReturn(criteria);
        when(mainEntity.pageQuery(eq(criteria), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(record), 1, PageRequest.of(1, 20)));

        mvc.perform(get("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.values.signedDate").value("2026-06-01"))
                .andExpect(jsonPath("$.values.signedAt").value("2026-06-01T02:03:04Z"));

        mvc.perform(post("/{moduleAlias}/query", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "conditions", List.of(Map.of(
                                        "fieldName", "signedDate",
                                        "operator", "EQ",
                                        "values", List.of("2026-06-01")
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].values.signedDate").value("2026-06-01"))
                .andExpect(jsonPath("$.records[0].values.signedAt").value("2026-06-01T02:03:04Z"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DynamicQueryCondition>> conditions = ArgumentCaptor.forClass(List.class);
        verify(mainEntity).queryCriteria(conditions.capture());
        assertThat(conditions.getValue().getFirst().values()).isEqualTo(List.of("2026-06-01"));
    }

    @Test
    void shouldExposeMainEntityViewDeleteAndActionsThroughAliasRootContract() throws Exception {
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(mainEntity.select("contract-1")).thenReturn(record);
        when(mainEntity.delete("contract-1")).thenReturn(1);
        when(service.actions(MODULE)).thenReturn(List.of(
                action("export", EntityActionLevel.LIST),
                action("submit", EntityActionLevel.RECORD, "view"),
                action("archive", EntityActionLevel.BATCH)
        ));

        mvc.perform(get("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.values.code").value("C-001"));

        mvc.perform(post("/{moduleAlias}/delete/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mvc.perform(get("/{moduleAlias}/actions", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("export"))
                .andExpect(jsonPath("$[0].permission.permissionCode").value(MODULE + ":view"))
                .andExpect(jsonPath("$[1].code").value("submit"))
                .andExpect(jsonPath("$[1].authInheritActionCode").value("view"))
                .andExpect(jsonPath("$[1].permission.permissionCode").value(MODULE + ":view"))
                .andExpect(jsonPath("$[1].permission.inheritActionCode").value("view"))
                .andExpect(jsonPath("$[1].permission.inheritPermissionCode").value(MODULE + ":view"))
                .andExpect(jsonPath("$[1].authInheritActionAlias").doesNotExist())
                .andExpect(jsonPath("$[2].code").value("archive"));

        verify(mainEntity).select("contract-1");
        verify(mainEntity).delete("contract-1");
        verify(service).actions(MODULE);
    }

    @Test
    void shouldHideUnauthorizedDynamicActionsFromActionLists() throws Exception {
        DynamicActionDescriptor export = action("export", EntityActionLevel.LIST);
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        DynamicActionDescriptor archive = action("archive", EntityActionLevel.BATCH);
        DynamicRecord existing = new DynamicRecord(entity()).setValue("code", "C-001");
        existing.setId("contract-1");
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.select(MODULE, ENTITY, "contract-1")).thenReturn(existing);
        when(service.actions(MODULE)).thenReturn(List.of(export, submit, archive));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAuthorizationAvailability(eq(MODULE), eq(ENTITY), eq("submit"), any()))
                .thenReturn(DynamicActionAvailability.unavailable("submit", "action permission denied"));
        when(service.actionAvailability(eq(MODULE), eq("export"), any(DynamicRecord.class)))
                .thenReturn(DynamicActionAvailability.available("export"));

        mvc.perform(get("/{moduleAlias}/actions", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("export"))
                .andExpect(jsonPath("$[1].code").value("archive"))
                .andExpect(jsonPath("$[2]").doesNotExist());

        mvc.perform(get("/{moduleAlias}/actions/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    void shouldExposeMainEntityTreeThroughStandardTreeWebContract() throws Exception {
        DynamicRecord first = new DynamicRecord(treeEntity()).setValue("code", "A");
        first.setId("A");
        first.setParentId("root");
        first.setSortOrder(1);
        DynamicRecord second = new DynamicRecord(treeEntity()).setValue("code", "B");
        second.setId("B");
        second.setParentId("root");
        second.setSortOrder(2);
        when(mainEntity.children("root")).thenReturn(List.of(first, second));
        when(mainEntity.children("A")).thenReturn(List.of());
        when(mainEntity.children("B")).thenReturn(List.of());

        mvc.perform(get("/{moduleAlias}/tree?flat=true", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("A"))
                .andExpect(jsonPath("$.records[0].values.code").value("A"))
                .andExpect(jsonPath("$.records[0].values.parentId").value("root"))
                .andExpect(jsonPath("$.records[1].id").value("B"));

        verify(mainEntity).children("root");
        verify(mainEntity).children("A");
        verify(mainEntity).children("B");
    }

    @Test
    void shouldExposeDynamicTreeNodeThroughStandardTreeWebContract() throws Exception {
        DynamicRecord root = new DynamicRecord(treeEntity()).setValue("code", "A");
        root.setId("A");
        root.setParentId("root");
        DynamicRecord child = new DynamicRecord(treeEntity()).setValue("code", "A-1");
        child.setId("A-1");
        child.setParentId("A");
        when(mainEntity.select("A")).thenReturn(root);
        when(mainEntity.children("A")).thenReturn(List.of(child));
        when(mainEntity.children("A-1")).thenReturn(List.of());

        mvc.perform(get("/{moduleAlias}/tree/{recordId}?flat=true", MODULE, "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("A"))
                .andExpect(jsonPath("$.records[1].id").value("A-1"))
                .andExpect(jsonPath("$.records[1].values.parentId").value("A"));

        verify(mainEntity).select("A");
        verify(mainEntity).children("A");
        verify(mainEntity).children("A-1");
    }

    @Test
    void shouldExposeDynamicNestedTreeByDefault() throws Exception {
        DynamicRecord root = new DynamicRecord(treeEntity()).setValue("code", "A");
        root.setId("A");
        root.setParentId("root");
        DynamicRecord child = new DynamicRecord(treeEntity()).setValue("code", "A-1");
        child.setId("A-1");
        child.setParentId("A");
        when(mainEntity.select("A")).thenReturn(root);
        when(mainEntity.children("A")).thenReturn(List.of(child));
        when(mainEntity.children("A-1")).thenReturn(List.of());

        mvc.perform(get("/{moduleAlias}/tree/{recordId}", MODULE, "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("A"))
                .andExpect(jsonPath("$.records[0].record.values.code").value("A"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("A-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.values.parentId").value("A"))
                .andExpect(jsonPath("$.records[0].children[0].children").isArray());

        verify(mainEntity).select("A");
        verify(mainEntity).children("A");
        verify(mainEntity).children("A-1");
    }

    @Test
    void shouldExcludeEntryNodeWhenIncludeSelfDisabled() throws Exception {
        DynamicRecord root = new DynamicRecord(treeEntity()).setValue("code", "A");
        root.setId("A");
        DynamicRecord child = new DynamicRecord(treeEntity()).setValue("code", "A-1");
        child.setId("A-1");
        child.setParentId("A");
        when(mainEntity.select("A")).thenReturn(root);
        when(mainEntity.children("A")).thenReturn(List.of(child));
        when(mainEntity.children("A-1")).thenReturn(List.of());

        mvc.perform(get("/{moduleAlias}/tree/{recordId}?includeSelf=false", MODULE, "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("A-1"))
                .andExpect(jsonPath("$.records[0].children").isArray());

        verify(mainEntity).select("A");
        verify(mainEntity).children("A");
        verify(mainEntity).children("A-1");
    }

    @Test
    void shouldExposeDynamicTreeSortThroughStandardSortWebContract() throws Exception {
        when(mainEntity.describe()).thenReturn(DynamicEntityDescriptor.from(treeEntity()));

        mvc.perform(post("/{moduleAlias}/sort/{recordId}", MODULE, "A")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "previousId", "B",
                                "parentId", "P"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(mainEntity).moveInTree("A", "B", null, "P");
    }

    @Test
    void shouldRejectEmptyDynamicTreeSortRequest() throws Exception {
        when(mainEntity.describe()).thenReturn(DynamicEntityDescriptor.from(treeEntity()));

        mvc.perform(post("/{moduleAlias}/sort/{recordId}", MODULE, "A")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tree sort requires previousId, nextId, or parentId"));
    }

    @Test
    void shouldExposeDynamicSortOnlyEntityThroughStandardSortWebContract() throws Exception {
        when(mainEntity.describe()).thenReturn(DynamicEntityDescriptor.from(sortableEntity()));

        mvc.perform(post("/{moduleAlias}/sort/{recordId}", MODULE, "A")
                        .contentType("application/json")
                        .content(json(Map.of("previousId", "B"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(mainEntity).moveAfter("A", "B");
    }

    @Test
    void shouldRejectParentIdWhenDynamicEntityOnlySupportsSort() throws Exception {
        when(mainEntity.describe()).thenReturn(DynamicEntityDescriptor.from(sortableEntity()));

        mvc.perform(post("/{moduleAlias}/sort/{recordId}", MODULE, "A")
                        .contentType("application/json")
                        .content(json(Map.of("parentId", "P"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sort parentId requires TREE capability"));
    }

    @Test
    void shouldRejectTreeWebWhenDynamicMainEntityDoesNotSupportTree() throws Exception {
        when(mainEntity.children("root"))
                .thenThrow(new PlatformException("dynamic entity does not support capability: TREE"));

        mvc.perform(get("/{moduleAlias}/tree", MODULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("dynamic entity does not support capability: TREE"));
    }

    @Test
    void shouldExposeDynamicEnableThroughStandardEnableWebContract() throws Exception {
        when(mainEntity.enable("contract-1")).thenReturn(1);
        when(mainEntity.disable("contract-1")).thenReturn(1);

        mvc.perform(post("/{moduleAlias}/enable/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(post("/{moduleAlias}/disable/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(mainEntity).enable("contract-1");
        verify(mainEntity).disable("contract-1");
    }

    @Test
    void shouldRejectEnableWebWhenDynamicMainEntityDoesNotSupportEnable() throws Exception {
        when(mainEntity.enable("contract-1"))
                .thenThrow(new PlatformException("dynamic entity does not support capability: ENABLE"));

        mvc.perform(post("/{moduleAlias}/enable/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("dynamic entity does not support capability: ENABLE"));
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

        mvc.perform(get("/{moduleAlias}/actions/{recordId}", MODULE, "contract-1"))
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

        mvc.perform(get("/{moduleAlias}/actions/{recordId}", MODULE, "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic record does not exist: missing"));
    }

    @Test
    void shouldExecuteActionWithRecordPayload() throws Exception {
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        when(service.action(MODULE, "submit")).thenReturn(submit);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.actionEntityAlias(MODULE, "submit")).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));
        when(service.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "submit", submit,
                                "contract-1", "trace-1", "tenant-1", false,
                                DynamicActionAvailability.available("submit")),
                        "ok",
                        DynamicActionResultBody.refreshed("ok").message("已提交")));

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
                .andExpect(jsonPath("$.context.actionLevel").value("RECORD"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"))
                .andExpect(jsonPath("$.context.recordId").value("contract-1"))
                .andExpect(jsonPath("$.context.traceId").value("trace-1"))
                .andExpect(jsonPath("$.context.entityAlias").doesNotExist())
                .andExpect(jsonPath("$.context.action").doesNotExist())
                .andExpect(jsonPath("$.body.type").value("VALUE"))
                .andExpect(jsonPath("$.body.value").value("ok"))
                .andExpect(jsonPath("$.body.message").value("已提交"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        ArgumentCaptor<DynamicActionExecutionRequest> request = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq("submit"), request.capture());
        assertThat(request.getValue().recordId()).isEqualTo("contract-1");
        assertThat(request.getValue().record().getValue("code")).isEqualTo("C-001");
        assertThat(request.getValue().queryConditions().iterator().next().fieldName()).isEqualTo("code");
        assertThat(request.getValue().payload()).containsEntry("comment", "submit");
    }

    @Test
    void shouldExecuteContributedWorkflowActionThroughRecordActionPath() throws Exception {
        DynamicActionDescriptor syncWorkflow = workflowAction("syncWorkflow");
        when(service.action(MODULE, "syncWorkflow")).thenReturn(syncWorkflow);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.actionEntityAlias(MODULE, "syncWorkflow")).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("syncWorkflow"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "syncWorkflow", syncWorkflow,
                                "contract-1", "trace-1", "tenant-1", false,
                                DynamicActionAvailability.available("syncWorkflow")),
                        "ok",
                        DynamicActionResultBody.refreshed("ok")));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "syncWorkflow", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of("payload", Map.of(
                                "selectedDirectLinkKey", "leftRoute",
                                "selectedReason", "choose left"
                        )))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.actionCode").value("syncWorkflow"))
                .andExpect(jsonPath("$.context.actionLevel").value("RECORD"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"))
                .andExpect(jsonPath("$.context.recordId").value("contract-1"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        ArgumentCaptor<DynamicActionExecutionRequest> request = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq("syncWorkflow"), request.capture());
        assertThat(request.getValue().recordId()).isEqualTo("contract-1");
        assertThat(request.getValue().payload()).containsEntry("selectedDirectLinkKey", "leftRoute")
                .containsEntry("selectedReason", "choose left");
    }

    @Test
    void shouldExposeDialogActionResultThroughStableWebResponse() throws Exception {
        DynamicActionDescriptor submitDialog = dialogAction("submitDialog", EntityActionLevel.RECORD);
        DynamicActionDialog dialog = new DynamicActionDialog("contractSubmitDialog", "提交合同");
        when(service.action(MODULE, "submitDialog")).thenReturn(submitDialog);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("submitDialog"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "submitDialog", submitDialog,
                                "contract-1", "trace-1", "tenant-1", false,
                                DynamicActionAvailability.available("submitDialog")),
                        dialog,
                        DynamicActionResultBody.dialog("contractSubmitDialog", "提交合同")));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submitDialog", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.context.actionCode").value("submitDialog"))
                .andExpect(jsonPath("$.context.actionLevel").value("RECORD"))
                .andExpect(jsonPath("$.context.executorType").value("DIALOG"))
                .andExpect(jsonPath("$.context.recordId").value("contract-1"))
                .andExpect(jsonPath("$.body.type").value("DIALOG"))
                .andExpect(jsonPath("$.body.value.dialogKey").value("contractSubmitDialog"))
                .andExpect(jsonPath("$.body.value.title").value("提交合同"))
                .andExpect(jsonPath("$.body.refresh").value(false));
    }

    @Test
    void shouldRejectActionRecordIdMismatch() throws Exception {
        when(service.action(MODULE, "submit")).thenReturn(action("submit", EntityActionLevel.RECORD));
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.actionEntityAlias(MODULE, "submit")).thenReturn(ENTITY);
        when(service.newRecord(MODULE, ENTITY)).thenAnswer(invocation -> new DynamicRecord(entity()));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "record", Map.of("id", "contract-2", "values", Map.of("code", "C-001"))
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("action request recordId must match record.id"));
    }

    @Test
    void shouldExposeActionFailureStageAndContext() throws Exception {
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        DynamicActionExecutionContext context = new DynamicActionExecutionContext(MODULE, ENTITY, "submit", submit,
                "contract-1", "trace-1", "tenant-1", false,
                DynamicActionAvailability.unavailable("submit", "只有草稿合同可以提交"));
        when(service.action(MODULE, "submit")).thenReturn(submit);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenThrow(new DynamicActionExecutionException("只有草稿合同可以提交", context,
                        DynamicActionExecutionException.STAGE_AVAILABILITY, null));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_ACTION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("只有草稿合同可以提交"))
                .andExpect(jsonPath("$.failureStage").value("availability"))
                .andExpect(jsonPath("$.traceId").value("trace-1"))
                .andExpect(jsonPath("$.context.moduleAlias").value(MODULE))
                .andExpect(jsonPath("$.context.actionCode").value("submit"))
                .andExpect(jsonPath("$.context.actionLevel").value("RECORD"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"))
                .andExpect(jsonPath("$.context.recordId").value("contract-1"))
                .andExpect(jsonPath("$.context.traceId").value("trace-1"));
    }

    @Test
    void shouldExposeExecuteActionFailureWithStableErrorShape() throws Exception {
        DynamicActionDescriptor submit = action("submit", EntityActionLevel.RECORD);
        DynamicActionExecutionContext context = new DynamicActionExecutionContext(MODULE, ENTITY, "submit", submit,
                "contract-1", "trace-2", "tenant-1", false,
                DynamicActionAvailability.available("submit"));
        when(service.action(MODULE, "submit")).thenReturn(submit);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenThrow(new DynamicActionExecutionException("submit failed", context,
                        DynamicActionExecutionException.STAGE_EXECUTE, new IllegalStateException("boom")));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_ACTION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("submit failed"))
                .andExpect(jsonPath("$.failureStage").value("execute"))
                .andExpect(jsonPath("$.traceId").value("trace-2"))
                .andExpect(jsonPath("$.context.actionCode").value("submit"))
                .andExpect(jsonPath("$.context.actionLevel").value("RECORD"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"));
    }

    @Test
    void shouldExposeActionFailureWithoutContextAsStableErrorShape() throws Exception {
        when(service.action(MODULE, "submit")).thenReturn(action("submit", EntityActionLevel.RECORD));
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("submit"), any(DynamicActionExecutionRequest.class)))
                .thenThrow(new DynamicActionExecutionException("submit failed", null));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "submit", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_ACTION_FAILED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("submit failed"))
                .andExpect(jsonPath("$.failureStage").value("execute"))
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(jsonPath("$.context").doesNotExist());
    }

    @Test
    void shouldRejectActionResponseThatWouldExposeInternalCriteria() throws Exception {
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
    void shouldRejectReservedOpenApiAsActionPath() throws Exception {
        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "openapi", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectReservedSortAsActionPath() throws Exception {
        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "sort")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectReservedReferenceAsActionPath() throws Exception {
        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "reference")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldExecuteListAndBatchActionsThroughStaticLikePaths() throws Exception {
        DynamicActionDescriptor publish = action("publish", EntityActionLevel.LIST);
        DynamicActionDescriptor archive = action("archive", EntityActionLevel.BATCH);
        DynamicActionDescriptor refreshSelected = action("refreshSelected", EntityActionLevel.ANY);
        when(service.action(MODULE, "publish")).thenReturn(publish);
        when(service.action(MODULE, "archive")).thenReturn(archive);
        when(service.action(MODULE, "refreshSelected")).thenReturn(refreshSelected);
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(service.executeAction(eq(MODULE), eq("publish"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "publish", publish,
                                null, "trace-list", "tenant-1", false,
                                DynamicActionAvailability.available("publish")),
                        "ok",
                        DynamicActionResultBody.redirect("/exports/contract.xlsx", "发布已生成")));
        when(service.executeAction(eq(MODULE), eq("archive"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(
                        new DynamicActionExecutionContext(MODULE, ENTITY, "archive", archive,
                                null, "trace-batch", "tenant-1", false,
                                DynamicActionAvailability.available("archive")),
                        2,
                        DynamicActionResultBody.changedCount(2, "已归档 2 条")));
        when(service.executeAction(eq(MODULE), eq("refreshSelected"), any(DynamicActionExecutionRequest.class)))
                .thenReturn(new DynamicActionExecutionResult(null, "ok",
                        DynamicActionResultBody.refreshed("ok")));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "publish")
                        .contentType("application/json")
                        .content(json(Map.of("payload", Map.of("format", "xlsx")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.actionLevel").value("LIST"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"))
                .andExpect(jsonPath("$.body.type").value("NONE"))
                .andExpect(jsonPath("$.body.value").doesNotExist())
                .andExpect(jsonPath("$.body.message").value("发布已生成"))
                .andExpect(jsonPath("$.body.refresh").value(false))
                .andExpect(jsonPath("$.body.redirectTo").value("/exports/contract.xlsx"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "archive")
                        .contentType("application/json")
                        .content(json(Map.of("ids", List.of("contract-1", "contract-2")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context.actionLevel").value("BATCH"))
                .andExpect(jsonPath("$.context.executorType").value("SERVICE"))
                .andExpect(jsonPath("$.body.type").value("COUNT"))
                .andExpect(jsonPath("$.body.value").value(2))
                .andExpect(jsonPath("$.body.message").value("已归档 2 条"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "refreshSelected")
                        .contentType("application/json")
                .content(json(Map.of("ids", List.of("contract-1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body.type").value("VALUE"))
                .andExpect(jsonPath("$.body.value").value("ok"))
                .andExpect(jsonPath("$.body.refresh").value(true));

        ArgumentCaptor<DynamicActionExecutionRequest> batchRequest = ArgumentCaptor.forClass(DynamicActionExecutionRequest.class);
        verify(service).executeAction(eq(MODULE), eq("archive"), batchRequest.capture());
        assertThat(batchRequest.getValue().ids()).containsExactly("contract-1", "contract-2");
    }

    @Test
        void shouldRejectActionPathWhenLevelDoesNotMatch() throws Exception {
        when(service.action(MODULE, "submit")).thenReturn(action("submit", EntityActionLevel.RECORD));
        when(service.action(MODULE, "publish")).thenReturn(action("publish", EntityActionLevel.LIST));
        when(service.action(MODULE, "archive")).thenReturn(action("archive", EntityActionLevel.BATCH));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "submit")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support list path: submit"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/{recordId}", MODULE, "publish", "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support record path: publish"));

        mvc.perform(post("/{moduleAlias}/{actionCode}", MODULE, "archive")
                        .contentType("application/json")
                        .content(json(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support list path: archive"));

        mvc.perform(post("/{moduleAlias}/{actionCode}/batch", MODULE, "publish")
                        .contentType("application/json")
                        .content(json(Map.of("ids", List.of("contract-1")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic action does not support batch path: publish"));
    }

    @Test
    void shouldTreatActionsPathAsRecordActionQueryInsteadOfExecution() throws Exception {
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);

        mvc.perform(get("/{moduleAlias}/actions/{actionCode}", MODULE, "submit"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic record does not exist: submit"));
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
                                )),
                                "formValues", Map.of("customerRegion", "north")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.mode").value("QUERY"));

        ArgumentCaptor<DynamicReferenceResolveRequest> request = ArgumentCaptor.forClass(DynamicReferenceResolveRequest.class);
        verify(service).resolveFieldReference(eq(MODULE), eq(ENTITY), eq("customerId"), request.capture());
        assertThat(request.getValue().fuzzy()).isEqualTo("ximatai");
        assertThat(request.getValue().criteria()).isSameAs(criteria);
        assertThat(request.getValue().includeProjections()).isFalse();
        assertThat(request.getValue().formValues()).containsEntry("customerRegion", "north");
    }

    @Test
    void shouldGenerateDraftsFromReferenceFieldWithoutExposingRuleAction() throws Exception {
        when(service.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(referenceGenerationFacade.generateFromReference(MODULE, ENTITY, "opportunityId", "opp-1"))
                .thenReturn(new RecordGenerationResult(
                        "rule-1",
                        "generateContract",
                        "sales.opportunity",
                        "opp-1",
                        MODULE,
                        "batch-1",
                        List.of()
                ));

        mvc.perform(post("/{moduleAlias}/references/{fieldName}/generate", MODULE, "opportunityId")
                        .contentType("application/json")
                        .content(json(Map.of("sourceRecordId", "opp-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value("rule-1"))
                .andExpect(jsonPath("$.sourceRecordId").value("opp-1"))
                .andExpect(jsonPath("$.targetModuleAlias").value(MODULE));

        verify(referenceGenerationFacade).generateFromReference(MODULE, ENTITY, "opportunityId", "opp-1");
    }

    @Test
    void shouldConfirmGeneratedDraftWithOriginContext() throws Exception {
        RecordOriginContext originContext = new RecordOriginContext(
                RecordImpactType.GENERATE_PUSH,
                "sales.opportunity",
                "opp-1",
                MODULE,
                "rule-1",
                "generateContract",
                "batch-1",
                "contract:1"
        );
        when(referenceGenerationFacade.confirmDraft(any(RecordGenerationDraft.class))).thenReturn("contract-1");

        mvc.perform(post("/{moduleAlias}/generation/confirm", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "targetModuleAlias", MODULE,
                                "targetEntityAlias", ENTITY,
                                "record", Map.of("values", Map.of("code", "C-001")),
                                "originContext", Map.of(
                                        "impactType", "GENERATE_PUSH",
                                        "sourceModuleAlias", "sales.opportunity",
                                        "sourceRecordId", "opp-1",
                                        "targetModuleAlias", MODULE,
                                        "generationRuleId", "rule-1",
                                        "actionCode", "generateContract",
                                        "batchId", "batch-1",
                                        "draftKey", "contract:1"
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value("rule-1"))
                .andExpect(jsonPath("$.batchId").value("batch-1"))
                .andExpect(jsonPath("$.recordIds[0]").value("contract-1"));

        ArgumentCaptor<RecordGenerationDraft> draft = ArgumentCaptor.forClass(RecordGenerationDraft.class);
        verify(referenceGenerationFacade).confirmDraft(draft.capture());
        assertThat(draft.getValue().targetModuleAlias()).isEqualTo(MODULE);
        assertThat(draft.getValue().targetEntityAlias()).isEqualTo(ENTITY);
        assertThat(draft.getValue().record().getValue("code")).isEqualTo("C-001");
        assertThat(draft.getValue().originContext()).isEqualTo(originContext);
    }

    @Test
    void shouldRejectGeneratedDraftConfirmForDifferentPathModule() throws Exception {
        mvc.perform(post("/{moduleAlias}/generation/confirm", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "targetModuleAlias", "finance.invoice",
                                "targetEntityAlias", "invoice",
                                "record", Map.of("values", Map.of())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "dynamic generation confirm targetModuleAlias mismatch: finance.invoice != " + MODULE));

        verifyNoInteractions(referenceGenerationFacade);
    }

    @Test
    void shouldRejectGeneratedDraftConfirmWhenChildRelationIsNotArray() throws Exception {
        Map<String, Object> children = new java.util.LinkedHashMap<>();
        children.put("lines", null);

        mvc.perform(post("/{moduleAlias}/generation/confirm", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "targetModuleAlias", MODULE,
                                "targetEntityAlias", ENTITY,
                                "record", Map.of(
                                        "values", Map.of("code", "C-001"),
                                        "children", children
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("dynamic child relation must be array: lines"));

        verifyNoInteractions(referenceGenerationFacade);
    }

    @Test
    void shouldCompleteReferenceGenerationAndConfirmThroughDynamicWebEntries() throws Exception {
        RecordOriginContext originContext = new RecordOriginContext(
                RecordImpactType.GENERATE_PUSH,
                "sales.opportunity",
                "opp-1",
                MODULE,
                "rule-1",
                "generateContract",
                "batch-1",
                "contract:1"
        );
        DynamicRecord draft = new DynamicRecord(entity())
                .setValue("code", "C-001")
                .setValue("amount", new BigDecimal("100.00"));
        draft.setChildren("lines", List.of(new DynamicRecord(lineEntity())
                .setValue("lineNo", "L-001")
                .setValue("lineAmount", new BigDecimal("100.00"))));
        when(service.relations(MODULE)).thenReturn(List.of(
                new DynamicRelationDescriptor("lines", ENTITY, "contract_line", "contractId", false, false)
        ));
        when(service.newRecord(MODULE, "contract_line")).thenAnswer(invocation -> new DynamicRecord(lineEntity()));
        when(service.resolveFieldReference(anyString(), anyString(), anyString(), any()))
                .thenReturn(new DynamicReferenceResolveResponse(
                        DynamicReferenceResolveStatus.OK,
                        DynamicReferenceResolveMode.QUERY,
                        List.of(new DynamicReferenceResolveItem(
                                "opp-1",
                                "OPP-001",
                                DynamicReferenceMatchMode.AUTO,
                                Map.of("opportunityNo", "OPP-001"),
                                Map.of("code", "C-001")
                        )),
                        List.of(),
                        0,
                        20,
                        1
                ));
        when(referenceGenerationFacade.generateFromReference(MODULE, ENTITY, "opportunityId", "opp-1"))
                .thenReturn(new RecordGenerationResult(
                        "rule-1",
                        "generateContract",
                        "sales.opportunity",
                        "opp-1",
                        MODULE,
                        "batch-1",
                        List.of(new RecordGenerationDraft(MODULE, ENTITY, draft, originContext))
                ));
        when(referenceGenerationFacade.confirmDraft(any(RecordGenerationDraft.class))).thenReturn("contract-1");

        mvc.perform(post("/{moduleAlias}/references/{fieldName}/resolve", MODULE, "opportunityId")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "fuzzy", "OPP",
                                "formValues", Map.of("region", "north")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].id").value("opp-1"))
                .andExpect(jsonPath("$.options[0].affectPatch.code").value("C-001"));
        MvcResult generationResult = mvc.perform(post("/{moduleAlias}/references/{fieldName}/generate", MODULE, "opportunityId")
                        .contentType("application/json")
                        .content(json(Map.of("sourceRecordId", "opp-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drafts[0].record.values.code").value("C-001"))
                .andExpect(jsonPath("$.drafts[0].originContext.batchId").value("batch-1"))
                .andReturn();
        @SuppressWarnings("unchecked")
        Map<String, Object> generationBody = objectMapper.readValue(
                generationResult.getResponse().getContentAsString(),
                Map.class
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> draftBody = (Map<String, Object>) ((List<?>) generationBody.get("drafts")).getFirst();

        mvc.perform(post("/{moduleAlias}/generation/confirm", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of(
                                "targetModuleAlias", draftBody.get("targetModuleAlias"),
                                "targetEntityAlias", draftBody.get("targetEntityAlias"),
                                "record", draftBody.get("record"),
                                "originContext", draftBody.get("originContext")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordIds[0]").value("contract-1"));

        ArgumentCaptor<DynamicReferenceResolveRequest> resolveRequest =
                ArgumentCaptor.forClass(DynamicReferenceResolveRequest.class);
        verify(service).resolveFieldReference(eq(MODULE), eq(ENTITY), eq("opportunityId"), resolveRequest.capture());
        assertThat(resolveRequest.getValue().formValues()).containsEntry("region", "north");
        verify(referenceGenerationFacade).generateFromReference(MODULE, ENTITY, "opportunityId", "opp-1");
        ArgumentCaptor<RecordGenerationDraft> confirmedDraft = ArgumentCaptor.forClass(RecordGenerationDraft.class);
        verify(referenceGenerationFacade).confirmDraft(confirmedDraft.capture());
        assertThat(confirmedDraft.getValue().record().getValue("code")).isEqualTo("C-001");
        assertThat(confirmedDraft.getValue().record().getChildren("lines")).singleElement()
                .satisfies(line -> assertThat(line.getValue("lineNo")).isEqualTo("L-001"));
        assertThat(confirmedDraft.getValue().originContext().batchId()).isEqualTo("batch-1");
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

        mvc.perform(get("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").doesNotExist())
                .andExpect(jsonPath("$.message").value("unknown module alias: " + MODULE));
    }

    @Test
    void shouldReturnStableBadRequestWhenTenantContextIsMissing() throws Exception {
        MockMvc noTenantMvc = MockMvcBuilders
                .standaloneSetup(new DynamicRecordWebController(service, activeTenantVerifier))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(new CurrentUserWebFilter(java.util.Optional::empty))
                .build();

        noTenantMvc.perform(get("/{moduleAlias}/describe", MODULE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(MODULE + " requires tenant context"));

        verifyNoInteractions(activeTenantVerifier);
    }

    @Test
    void shouldReturnConflictForOptimisticLockFailure() throws Exception {
        when(mainEntity.update(any(DynamicRecord.class)))
                .thenThrow(new OptimisticLockException("record version conflict: contract-1"));

        mvc.perform(post("/{moduleAlias}/update/{recordId}", MODULE, "contract-1")
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("code", "C-001")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DYNAMIC_CONFLICT"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("record version conflict: contract-1"));
    }

    @Test
    void shouldReturnStableBadRequestWhenDynamicRecordPayloadCannotBeDecoded() throws Exception {
        mvc.perform(post("/{moduleAlias}/insert", MODULE)
                        .contentType("application/json")
                        .content(json(Map.of("values", Map.of("unknown", "C-001")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DYNAMIC_BAD_REQUEST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("unknown dynamic field: unknown"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
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
                List.of(new EntityActionDefinition(ENTITY, "submit", "Submit", true, EntityActionLevel.RECORD,
                        EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null,
                        EntityActionExecutorType.SERVICE, "submitExecutor"))
        );
    }

    private DynamicActionDescriptor action(String code, EntityActionLevel level) {
        return action(code, level, null);
    }

    private DynamicActionDescriptor action(String code, EntityActionLevel level, String authInheritActionCode) {
        return new DynamicActionDescriptor(code, "Submit", true, level, EntityActionCategory.CUSTOM,
                EntityActionAccessMode.AUTH_REQUIRED, true, false, authInheritActionCode, false, null,
                EntityActionExecutorType.SERVICE, "submitExecutor").withPermission(MODULE);
    }

    private DynamicActionDescriptor workflowAction(String code) {
        return new DynamicActionDescriptor(code, "同步流程", true, EntityActionLevel.RECORD,
                EntityActionCategory.WORKFLOW, EntityActionAccessMode.AUTH_REQUIRED,
                true, false, null, false, null,
                EntityActionExecutorType.SERVICE, "platform.workflow").withPermission(MODULE);
    }

    private DynamicActionDescriptor dialogAction(String code, EntityActionLevel level) {
        return new DynamicActionDescriptor(code, "提交合同", true, level, EntityActionCategory.DIALOG,
                EntityActionAccessMode.AUTH_REQUIRED, true, false, null, false, null,
                EntityActionExecutorType.DIALOG, "contractSubmitDialog");
    }

    private EntityDefinition entity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                FieldDefinition.of("signedDate", FieldType.DATE, "Signed Date").column("signed_date"),
                FieldDefinition.timestamp("signedAt", "Signed At").column("signed_at")
        ));
    }

    private ResolvedModuleMetadataField resolvedModuleField(String moduleFieldId, String fieldName) {
        return new ResolvedModuleMetadataField(
                moduleFieldId,
                MODULE,
                "rel-main",
                "main",
                RelationRole.MAIN,
                "metadata-1",
                ENTITY,
                "Contract",
                "metadata-field-" + fieldName,
                fieldName,
                fieldName,
                fieldName,
                "string"
        );
    }

    private EntityDefinition lineEntity() {
        return new EntityDefinition("contract_line", "sales_contract_line", "Contract Line", List.of(
                FieldDefinition.string("lineNo", "Line No").column("line_no").length(64).required(),
                FieldDefinition.decimal("lineAmount", "Line Amount").column("line_amount").precision(18, 2)
        ));
    }

    private EntityDefinition protectedEntity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.string("secret", "Secret")
                        .protection(new FieldProtectionDefinition(
                                FieldEncryptionMode.NONE,
                                FieldSignatureMode.NONE,
                                FieldMaskingPolicy.MIDDLE
                        ))
        ));
    }

    private EntityDefinition treeEntity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.parentId(),
                FieldDefinition.sortOrder()
        )).withCapabilities(EntityCapability.TREE);
    }

    private EntityDefinition sortableEntity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required(),
                FieldDefinition.sortOrder()
        )).withCapabilities(EntityCapability.SORT);
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
