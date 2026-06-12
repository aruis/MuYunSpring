package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewField;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewService;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldProtectionConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldProtectionConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfigService;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffect;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldAffectService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldFilter;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldFilterService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFormulaRule;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFormulaRuleService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttribute;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttributeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMapping;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMappingService;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigPublishService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplate;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformConfigurationWebControllerTest {
    @Test
    void shouldExposeApplicationScopedModuleTree() throws Exception {
        PlatformModuleService service = mock(PlatformModuleService.class);
        PlatformModuleWebController controller = new PlatformModuleWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformModule root = module("platform.sales", "platform", null);
        PlatformModule child = module("platform.sales.order", "platform", "platform.sales");
        when(service.rootModules("platform")).thenReturn(List.of(root));
        when(service.children("platform", "platform.sales")).thenReturn(List.of(child));
        when(service.children("platform", "platform.sales.order")).thenReturn(List.of());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(get("/platform.module/tree/platform"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("platform.sales"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("platform.sales.order"));

        verify(service).rootModules("platform");
    }

    @Test
    void shouldQueryModuleActionsWithinPathModule() throws Exception {
        PlatformModuleActionService service = mock(PlatformModuleActionService.class);
        PlatformModuleActionWebController controller = new PlatformModuleActionWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformModuleAction action = action("action-1", "platform.sales.order", "submit");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(action), 1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/actions/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"actionCode","values":["submit"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].moduleAlias").value("platform.sales.order"))
                .andExpect(jsonPath("$.records[0].actionCode").value("submit"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class));
        assertClause(criteria.getValue(), "moduleAlias", "platform.sales.order");
        assertClause(criteria.getValue(), "actionCode", "submit");
    }

    @Test
    void shouldForceActionModuleAliasFromPathOnInsert() throws Exception {
        PlatformModuleActionService service = mock(PlatformModuleActionService.class);
        PlatformModuleActionWebController controller = new PlatformModuleActionWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformModuleAction inserted = action("action-1", "platform.sales.order", "submit");
        when(service.insert(any(PlatformModuleAction.class))).thenReturn("action-1");
        when(service.select("action-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/actions/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"moduleAlias":"other.module","actionCode":"submit","title":"Submit"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleAlias").value("platform.sales.order"));

        ArgumentCaptor<PlatformModuleAction> captor = ArgumentCaptor.forClass(PlatformModuleAction.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getModuleAlias()).isEqualTo("platform.sales.order");
    }

    @Test
    void shouldRejectCrossModuleActionUpdate() {
        PlatformModuleActionService service = mock(PlatformModuleActionService.class);
        PlatformModuleActionWebController controller = new PlatformModuleActionWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("action-1")).thenReturn(action("action-1", "other.module", "submit"));

        MockHttpServletRequest request = requestVars(Map.of("moduleAlias", "platform.sales.order"));

        assertThatThrownBy(() -> controller.update(request, "action-1", new PlatformModuleAction()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to module");
    }

    @Test
    void shouldRejectRelationFieldWhenRelationBelongsToOtherModule() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataFieldService fieldService = mock(ModuleMetadataFieldService.class);
        PlatformModuleMetadataFieldWebController controller =
                new PlatformModuleMetadataFieldWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", fieldService);

        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId("rel-1");
        relation.setModuleAlias("other.module");
        when(relationService.select("rel-1")).thenReturn(relation);

        MockHttpServletRequest request = requestVars(Map.of(
                "moduleAlias", "platform.sales.order",
                "relationId", "rel-1"));

        assertThatThrownBy(() -> controller.ensure(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to module");
    }

    @Test
    void shouldForceReferenceConfigFieldFromPathOnInsert() throws Exception {
        MetadataFieldService fieldService = mock(MetadataFieldService.class);
        MetadataFieldReferenceConfigService service = mock(MetadataFieldReferenceConfigService.class);
        MetadataFieldReferenceConfigWebController controller =
                new MetadataFieldReferenceConfigWebController(fieldService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(fieldService.select("field-1")).thenReturn(metadataField("field-1", "metadata-1"));
        MetadataFieldReferenceConfig inserted = new MetadataFieldReferenceConfig();
        inserted.setId("ref-1");
        inserted.setMetadataFieldId("field-1");
        inserted.setTargetMetadataId("target-metadata");
        when(service.insert(any(MetadataFieldReferenceConfig.class))).thenReturn("ref-1");
        when(service.select("ref-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.metadata/metadata-1/fields/field-1/reference-configs/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metadataFieldId":"other-field","targetMetadataId":"target-metadata"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadataFieldId").value("field-1"));

        ArgumentCaptor<MetadataFieldReferenceConfig> captor =
                ArgumentCaptor.forClass(MetadataFieldReferenceConfig.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getMetadataFieldId()).isEqualTo("field-1");
    }

    @Test
    void shouldRejectProtectionConfigWhenFieldBelongsToOtherMetadata() {
        MetadataFieldService fieldService = mock(MetadataFieldService.class);
        MetadataFieldProtectionConfigService service = mock(MetadataFieldProtectionConfigService.class);
        MetadataFieldProtectionConfigWebController controller =
                new MetadataFieldProtectionConfigWebController(fieldService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(fieldService.select("field-1")).thenReturn(metadataField("field-1", "other-metadata"));
        MockHttpServletRequest request = requestVars(Map.of(
                "metadataId", "metadata-1",
                "fieldId", "field-1"));

        assertThatThrownBy(() -> controller.insert(request, new MetadataFieldProtectionConfig()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to metadata");
    }

    @Test
    void shouldRejectCrossFieldProtectionConfigView() {
        MetadataFieldService fieldService = mock(MetadataFieldService.class);
        MetadataFieldProtectionConfigService service = mock(MetadataFieldProtectionConfigService.class);
        MetadataFieldProtectionConfigWebController controller =
                new MetadataFieldProtectionConfigWebController(fieldService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(fieldService.select("field-1")).thenReturn(metadataField("field-1", "metadata-1"));
        MetadataFieldProtectionConfig config = new MetadataFieldProtectionConfig();
        config.setId("protect-1");
        config.setMetadataFieldId("other-field");
        when(service.select("protect-1")).thenReturn(config);
        MockHttpServletRequest request = requestVars(Map.of(
                "metadataId", "metadata-1",
                "fieldId", "field-1"));

        assertThatThrownBy(() -> controller.view(request, "protect-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to field");
    }

    @Test
    void shouldQueryModuleFieldFiltersWithinPathField() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataFieldService fieldService = mock(ModuleMetadataFieldService.class);
        ModuleMetadataFieldFilterService service = mock(ModuleMetadataFieldFilterService.class);
        PlatformModuleMetadataFieldFilterWebController controller =
                new PlatformModuleMetadataFieldFilterWebController(relationService, fieldService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(fieldService.select("field-1")).thenReturn(moduleField("field-1", "rel-1"));
        ModuleMetadataFieldFilter filter = new ModuleMetadataFieldFilter();
        filter.setId("filter-1");
        filter.setModuleMetadataFieldId("field-1");
        filter.setFormFieldId("field-form");
        filter.setReferenceFieldId("field-ref");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(filter), 1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/fields/field-1/filters/query"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].moduleMetadataFieldId").value("field-1"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class));
        assertClause(criteria.getValue(), "moduleMetadataFieldId", "field-1");
    }

    @Test
    void shouldForceModuleFieldAffectOwnerFromPathOnInsert() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataFieldService fieldService = mock(ModuleMetadataFieldService.class);
        ModuleMetadataFieldAffectService service = mock(ModuleMetadataFieldAffectService.class);
        PlatformModuleMetadataFieldAffectWebController controller =
                new PlatformModuleMetadataFieldAffectWebController(relationService, fieldService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(fieldService.select("field-1")).thenReturn(moduleField("field-1", "rel-1"));
        ModuleMetadataFieldAffect inserted = new ModuleMetadataFieldAffect();
        inserted.setId("affect-1");
        inserted.setModuleMetadataFieldId("field-1");
        when(service.insert(any(ModuleMetadataFieldAffect.class))).thenReturn("affect-1");
        when(service.select("affect-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/fields/field-1/affects/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"moduleMetadataFieldId":"other-field","referenceFieldId":"ref","targetFieldId":"target"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleMetadataFieldId").value("field-1"));

        ArgumentCaptor<ModuleMetadataFieldAffect> captor = ArgumentCaptor.forClass(ModuleMetadataFieldAffect.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getModuleMetadataFieldId()).isEqualTo("field-1");
    }

    @Test
    void shouldForceFormulaRuleRelationFromPathOnInsert() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataFormulaRuleService service = mock(ModuleMetadataFormulaRuleService.class);
        PlatformModuleMetadataFormulaRuleWebController controller =
                new PlatformModuleMetadataFormulaRuleWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        ModuleMetadataFormulaRule inserted = new ModuleMetadataFormulaRule();
        inserted.setId("rule-1");
        inserted.setRelationId("rel-1");
        inserted.setAlias("checkAmount");
        when(service.insert(any(ModuleMetadataFormulaRule.class))).thenReturn("rule-1");
        when(service.select("rule-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/formula-rules/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"relationId":"other-rel","alias":"checkAmount","expression":"amount > 0"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationId").value("rel-1"));

        ArgumentCaptor<ModuleMetadataFormulaRule> captor = ArgumentCaptor.forClass(ModuleMetadataFormulaRule.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getRelationId()).isEqualTo("rel-1");
    }

    @Test
    void shouldRejectCrossRelationFormulaRuleSort() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        ModuleMetadataFormulaRuleService service = mock(ModuleMetadataFormulaRuleService.class);
        PlatformModuleMetadataFormulaRuleWebController controller =
                new PlatformModuleMetadataFormulaRuleWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        ModuleMetadataFormulaRule rule = new ModuleMetadataFormulaRule();
        rule.setId("rule-1");
        rule.setRelationId("other-rel");
        when(service.select("rule-1")).thenReturn(rule);
        MockHttpServletRequest request = requestVars(Map.of(
                "moduleAlias", "platform.sales.order",
                "relationId", "rel-1"));

        assertThatThrownBy(() -> controller.sort(request, "rule-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to relation");
    }

    @Test
    void shouldQueryMetadataViewsWithinPathRelation() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService service = mock(MetadataViewService.class);
        PlatformMetadataViewWebController controller = new PlatformMetadataViewWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(metadataView("view-1", "rel-1", EntityViewType.LIST)),
                        1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/views/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"viewType","values":["LIST"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].relationId").value("rel-1"))
                .andExpect(jsonPath("$.records[0].viewType").value("LIST"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class));
        assertClause(criteria.getValue(), "relationId", "rel-1");
        assertClause(criteria.getValue(), "viewType", "LIST");
    }

    @Test
    void shouldForceMetadataViewRelationFromPathOnInsert() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService service = mock(MetadataViewService.class);
        PlatformMetadataViewWebController controller = new PlatformMetadataViewWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(service.insert(any(MetadataView.class))).thenReturn("view-1");
        when(service.select("view-1")).thenReturn(metadataView("view-1", "rel-1", EntityViewType.FORM));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/views/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"relationId":"other-rel","viewType":"FORM","title":"Form"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationId").value("rel-1"));

        ArgumentCaptor<MetadataView> captor = ArgumentCaptor.forClass(MetadataView.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getRelationId()).isEqualTo("rel-1");
    }

    @Test
    void shouldRejectCrossRelationMetadataViewSort() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService service = mock(MetadataViewService.class);
        PlatformMetadataViewWebController controller = new PlatformMetadataViewWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(service.select("view-1")).thenReturn(metadataView("view-1", "other-rel", EntityViewType.LIST));
        MockHttpServletRequest request = requestVars(Map.of(
                "moduleAlias", "platform.sales.order",
                "relationId", "rel-1"));

        assertThatThrownBy(() -> controller.sort(request, "view-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to relation");
    }

    @Test
    void shouldRejectMetadataViewWhenRelationBelongsToOtherModule() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService service = mock(MetadataViewService.class);
        PlatformMetadataViewWebController controller = new PlatformMetadataViewWebController(relationService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "other.module"));
        MockHttpServletRequest request = requestVars(Map.of(
                "moduleAlias", "platform.sales.order",
                "relationId", "rel-1"));

        assertThatThrownBy(() -> controller.query(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to module");
    }

    @Test
    void shouldForceMetadataViewFieldOwnerFromPathOnInsert() throws Exception {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService viewService = mock(MetadataViewService.class);
        MetadataViewFieldService service = mock(MetadataViewFieldService.class);
        PlatformMetadataViewFieldWebController controller =
                new PlatformMetadataViewFieldWebController(relationService, viewService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(viewService.select("view-1")).thenReturn(metadataView("view-1", "rel-1", EntityViewType.LIST));
        when(service.insert(any(MetadataViewField.class))).thenReturn("view-field-1");
        when(service.select("view-field-1")).thenReturn(metadataViewField("view-field-1", "view-1", "field-1"));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/metadata-relations/rel-1/views/view-1/fields/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"viewId":"other-view","metadataFieldId":"field-1","title":"Code"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.viewId").value("view-1"));

        ArgumentCaptor<MetadataViewField> captor = ArgumentCaptor.forClass(MetadataViewField.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getViewId()).isEqualTo("view-1");
    }

    @Test
    void shouldRejectCrossRelationMetadataViewFieldQuery() {
        ModuleMetadataRelationService relationService = mock(ModuleMetadataRelationService.class);
        MetadataViewService viewService = mock(MetadataViewService.class);
        MetadataViewFieldService service = mock(MetadataViewFieldService.class);
        PlatformMetadataViewFieldWebController controller =
                new PlatformMetadataViewFieldWebController(relationService, viewService);
        ReflectionTestUtils.setField(controller, "service", service);
        when(relationService.select("rel-1")).thenReturn(relation("rel-1", "platform.sales.order"));
        when(viewService.select("view-1")).thenReturn(metadataView("view-1", "other-rel", EntityViewType.LIST));
        MockHttpServletRequest request = requestVars(Map.of(
                "moduleAlias", "platform.sales.order",
                "relationId", "rel-1",
                "viewId", "view-1"));

        assertThatThrownBy(() -> controller.query(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to relation");
    }

    @Test
    void shouldExposeFieldTypeDirectory() throws Exception {
        PlatformFieldTypeService service = mock(PlatformFieldTypeService.class);
        PlatformFieldTypeWebController controller = new PlatformFieldTypeWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformFieldType fieldType = new PlatformFieldType();
        fieldType.setId("string");
        fieldType.setAlias("string");
        fieldType.setTitle("String");
        fieldType.setFieldType(FieldType.STRING);
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(fieldType), 1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.field_type/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"alias","values":["string"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].alias").value("string"))
                .andExpect(jsonPath("$.records[0].fieldType").value("STRING"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class));
        assertClause(criteria.getValue(), "alias", "string");
    }

    @Test
    void shouldManageDictionaryCategoriesWithinPathApplication() throws Exception {
        DictionaryCategoryService service = mock(DictionaryCategoryService.class);
        DictionaryCategoryWebController controller = new DictionaryCategoryWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        DictionaryCategory root = dictionaryCategory("category-1", "platform", "common", null);
        DictionaryCategory child = dictionaryCategory("category-2", "platform", "status", "category-1");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(root), 1, PageRequest.of(1, 20)));
        when(service.rootCategories("platform")).thenReturn(List.of(root));
        when(service.children("platform", "category-1")).thenReturn(List.of(child));
        when(service.children("platform", "category-2")).thenReturn(List.of());
        when(service.insert(any(DictionaryCategory.class))).thenReturn("category-1");
        when(service.select("category-1")).thenReturn(root);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.application/platform/dictionary-categories/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"alias","values":["common"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].applicationAlias").value("platform"))
                .andExpect(jsonPath("$.records[0].alias").value("common"));
        mvc.perform(get("/platform.application/platform/dictionary-categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("category-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("category-2"));
        mvc.perform(post("/platform.application/platform/dictionary-categories/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"other","alias":"common","title":"Common"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationAlias").value("platform"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "applicationAlias", "platform");
        assertClause(criteria.getValue(), "alias", "common");
        ArgumentCaptor<DictionaryCategory> captor = ArgumentCaptor.forClass(DictionaryCategory.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("platform");
    }

    @Test
    void shouldRejectCrossApplicationDictionaryCategoryUpdate() {
        DictionaryCategoryService service = mock(DictionaryCategoryService.class);
        DictionaryCategoryWebController controller = new DictionaryCategoryWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("category-1")).thenReturn(dictionaryCategory("category-1", "crm", "common", null));

        MockHttpServletRequest request = requestVars(Map.of("applicationAlias", "platform"));

        assertThatThrownBy(() -> controller.update(request, "category-1", new DictionaryCategory()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dictionary category does not belong to application");
    }

    @Test
    void shouldManageDictionaryItemsWithinPathCategory() throws Exception {
        DictionaryItemService service = mock(DictionaryItemService.class);
        DictionaryItemWebController controller = new DictionaryItemWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        DictionaryItem root = dictionaryItem("item-1", "platform", "status", "enabled", null);
        DictionaryItem child = dictionaryItem("item-2", "platform", "status", "active", "item-1");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(root), 1, PageRequest.of(1, 20)));
        when(service.rootItems("platform", "status")).thenReturn(List.of(root));
        when(service.children("platform", "status", "item-1")).thenReturn(List.of(child));
        when(service.children("platform", "status", "item-2")).thenReturn(List.of());
        when(service.insert(any(DictionaryItem.class))).thenReturn("item-1");
        when(service.select("item-1")).thenReturn(root);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.application/platform/dictionary-categories/status/items/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"code","values":["enabled"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].applicationAlias").value("platform"))
                .andExpect(jsonPath("$.records[0].categoryAlias").value("status"))
                .andExpect(jsonPath("$.records[0].code").value("enabled"));
        mvc.perform(get("/platform.application/platform/dictionary-categories/status/items/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("item-1"))
                .andExpect(jsonPath("$.records[0].children[0].record.id").value("item-2"));
        mvc.perform(post("/platform.application/platform/dictionary-categories/status/items/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"applicationAlias":"other","categoryAlias":"other","code":"enabled","title":"Enabled"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationAlias").value("platform"))
                .andExpect(jsonPath("$.categoryAlias").value("status"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "applicationAlias", "platform");
        assertClause(criteria.getValue(), "categoryAlias", "status");
        assertClause(criteria.getValue(), "code", "enabled");
        ArgumentCaptor<DictionaryItem> captor = ArgumentCaptor.forClass(DictionaryItem.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getApplicationAlias()).isEqualTo("platform");
        assertThat(captor.getValue().getCategoryAlias()).isEqualTo("status");
    }

    @Test
    void shouldRejectCrossCategoryDictionaryItemUpdate() {
        DictionaryItemService service = mock(DictionaryItemService.class);
        DictionaryItemWebController controller = new DictionaryItemWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("item-1")).thenReturn(dictionaryItem("item-1", "platform", "priority", "enabled", null));

        MockHttpServletRequest request = requestVars(Map.of(
                "applicationAlias", "platform",
                "categoryAlias", "status"));

        assertThatThrownBy(() -> controller.update(request, "item-1", new DictionaryItem()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dictionary item does not belong to category");
    }

    @Test
    void shouldQueryUiSetsWithinPathModule() throws Exception {
        PlatformUiSetService service = mock(PlatformUiSetService.class);
        PlatformUiSetWebController controller = new PlatformUiSetWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformUiSet uiSet = uiSet("ui-set-1", "platform.sales.order", "list");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(uiSet), 1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/ui-sets/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"alias","values":["list"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].moduleAlias").value("platform.sales.order"))
                .andExpect(jsonPath("$.records[0].alias").value("list"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "moduleAlias", "platform.sales.order");
        assertClause(criteria.getValue(), "alias", "list");
    }

    @Test
    void shouldForceUiSetModuleAliasFromPathOnInsert() throws Exception {
        PlatformUiSetService service = mock(PlatformUiSetService.class);
        PlatformUiSetWebController controller = new PlatformUiSetWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformUiSet inserted = uiSet("ui-set-1", "platform.sales.order", "list");
        when(service.insert(any(PlatformUiSet.class))).thenReturn("ui-set-1");
        when(service.select("ui-set-1")).thenReturn(inserted);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/ui-sets/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"moduleAlias":"other.module","alias":"list","setType":"LIST","title":"List"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleAlias").value("platform.sales.order"));

        ArgumentCaptor<PlatformUiSet> captor = ArgumentCaptor.forClass(PlatformUiSet.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getModuleAlias()).isEqualTo("platform.sales.order");
    }

    @Test
    void shouldQueryTemplatesWithinPathModule() throws Exception {
        PlatformQueryTemplateService service = mock(PlatformQueryTemplateService.class);
        PlatformQueryTemplateWebController controller = new PlatformQueryTemplateWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformQueryTemplate template = queryTemplate("query-1", "platform.sales.order", "default");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(template), 1, PageRequest.of(1, 20)));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.module/platform.sales.order/query-templates/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"alias","values":["default"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].moduleAlias").value("platform.sales.order"))
                .andExpect(jsonPath("$.records[0].alias").value("default"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "moduleAlias", "platform.sales.order");
        assertClause(criteria.getValue(), "alias", "default");
    }

    @Test
    void shouldManageFieldUiTypeAttributesWithinPathUiType() throws Exception {
        PlatformFieldUiTypeAttributeService service = mock(PlatformFieldUiTypeAttributeService.class);
        PlatformFieldUiTypeAttributeWebController controller = new PlatformFieldUiTypeAttributeWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformFieldUiTypeAttribute attribute = fieldUiTypeAttribute("attr-1", "text", "placeholder");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(attribute), 1, PageRequest.of(1, 20)));
        when(service.insert(any(PlatformFieldUiTypeAttribute.class))).thenReturn("attr-1");
        when(service.select("attr-1")).thenReturn(attribute);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.field_ui_type/text/attributes/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"attributeAlias","values":["placeholder"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].fieldUiTypeAlias").value("text"))
                .andExpect(jsonPath("$.records[0].attributeAlias").value("placeholder"));
        mvc.perform(post("/platform.field_ui_type/text/attributes/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldUiTypeAlias":"other","attributeAlias":"placeholder","title":"Placeholder"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fieldUiTypeAlias").value("text"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "fieldUiTypeAlias", "text");
        assertClause(criteria.getValue(), "attributeAlias", "placeholder");
        ArgumentCaptor<PlatformFieldUiTypeAttribute> attributeCaptor =
                ArgumentCaptor.forClass(PlatformFieldUiTypeAttribute.class);
        verify(service).insert(attributeCaptor.capture());
        assertThat(attributeCaptor.getValue().getFieldUiTypeAlias()).isEqualTo("text");
    }

    @Test
    void shouldRejectCrossFieldUiTypeAttributeUpdate() {
        PlatformFieldUiTypeAttributeService service = mock(PlatformFieldUiTypeAttributeService.class);
        PlatformFieldUiTypeAttributeWebController controller = new PlatformFieldUiTypeAttributeWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("attr-1")).thenReturn(fieldUiTypeAttribute("attr-1", "number", "placeholder"));

        MockHttpServletRequest request = requestVars(Map.of("fieldUiTypeAlias", "text"));

        assertThatThrownBy(() -> controller.update(request, "attr-1", new PlatformFieldUiTypeAttribute()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field UI type attribute does not belong to field UI type");
    }

    @Test
    void shouldManageFieldUiTypeMappingsWithinPathUiType() throws Exception {
        PlatformFieldUiTypeFieldMappingService service = mock(PlatformFieldUiTypeFieldMappingService.class);
        PlatformFieldUiTypeFieldMappingWebController controller = new PlatformFieldUiTypeFieldMappingWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        PlatformFieldUiTypeFieldMapping mapping = fieldUiTypeMapping("mapping-1", "select", "options");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(mapping), 1, PageRequest.of(1, 20)));
        when(service.insert(any(PlatformFieldUiTypeFieldMapping.class))).thenReturn("mapping-1");
        when(service.select("mapping-1")).thenReturn(mapping);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.field_ui_type/select/field-mappings/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"sourceKey","values":["options"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].fieldUiTypeAlias").value("select"))
                .andExpect(jsonPath("$.records[0].sourceKey").value("options"));
        mvc.perform(post("/platform.field_ui_type/select/field-mappings/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldUiTypeAlias":"other","sourceKey":"options","title":"Options"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fieldUiTypeAlias").value("select"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "fieldUiTypeAlias", "select");
        assertClause(criteria.getValue(), "sourceKey", "options");
        ArgumentCaptor<PlatformFieldUiTypeFieldMapping> mappingCaptor =
                ArgumentCaptor.forClass(PlatformFieldUiTypeFieldMapping.class);
        verify(service).insert(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getFieldUiTypeAlias()).isEqualTo("select");
    }

    @Test
    void shouldRejectCrossFieldUiTypeMappingUpdate() {
        PlatformFieldUiTypeFieldMappingService service = mock(PlatformFieldUiTypeFieldMappingService.class);
        PlatformFieldUiTypeFieldMappingWebController controller = new PlatformFieldUiTypeFieldMappingWebController();
        ReflectionTestUtils.setField(controller, "service", service);
        when(service.select("mapping-1")).thenReturn(fieldUiTypeMapping("mapping-1", "radio", "options"));

        MockHttpServletRequest request = requestVars(Map.of("fieldUiTypeAlias", "select"));

        assertThatThrownBy(() -> controller.update(request, "mapping-1", new PlatformFieldUiTypeFieldMapping()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field UI type field mapping does not belong to field UI type");
    }

    @Test
    void shouldPublishUiConfigThroughPublishService() throws Exception {
        PlatformPageConfigPublishService service = mock(PlatformPageConfigPublishService.class);
        PlatformPageConfigPublishWebController controller = new PlatformPageConfigPublishWebController();
        ReflectionTestUtils.setField(controller, "service", service);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(post("/platform.page_config_publish/ui-configs/ui-config-1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(service).publishUiConfig("ui-config-1");
    }

    private PlatformModule module(String id, String applicationAlias, String parentId) {
        PlatformModule module = new PlatformModule();
        module.setId(id);
        module.setApplicationAlias(applicationAlias);
        module.setParentId(parentId);
        return module;
    }

    private PlatformModuleAction action(String id, String moduleAlias, String actionCode) {
        PlatformModuleAction action = new PlatformModuleAction();
        action.setId(id);
        action.setModuleAlias(moduleAlias);
        action.setActionCode(actionCode);
        action.setTitle(actionCode);
        return action;
    }

    private PlatformUiSet uiSet(String id, String moduleAlias, String alias) {
        PlatformUiSet uiSet = new PlatformUiSet();
        uiSet.setId(id);
        uiSet.setModuleAlias(moduleAlias);
        uiSet.setAlias(alias);
        uiSet.setSetType(PlatformUiSetType.LIST);
        uiSet.setTitle(alias);
        return uiSet;
    }

    private PlatformQueryTemplate queryTemplate(String id, String moduleAlias, String alias) {
        PlatformQueryTemplate template = new PlatformQueryTemplate();
        template.setId(id);
        template.setModuleAlias(moduleAlias);
        template.setAlias(alias);
        template.setTitle(alias);
        return template;
    }

    private PlatformFieldUiTypeAttribute fieldUiTypeAttribute(String id, String fieldUiTypeAlias, String attributeAlias) {
        PlatformFieldUiTypeAttribute attribute = new PlatformFieldUiTypeAttribute();
        attribute.setId(id);
        attribute.setFieldUiTypeAlias(fieldUiTypeAlias);
        attribute.setAttributeAlias(attributeAlias);
        attribute.setTitle(attributeAlias);
        return attribute;
    }

    private PlatformFieldUiTypeFieldMapping fieldUiTypeMapping(String id, String fieldUiTypeAlias, String sourceKey) {
        PlatformFieldUiTypeFieldMapping mapping = new PlatformFieldUiTypeFieldMapping();
        mapping.setId(id);
        mapping.setFieldUiTypeAlias(fieldUiTypeAlias);
        mapping.setSourceKey(sourceKey);
        mapping.setTitle(sourceKey);
        return mapping;
    }

    private DictionaryCategory dictionaryCategory(String id, String applicationAlias, String alias, String parentId) {
        DictionaryCategory category = new DictionaryCategory();
        category.setId(id);
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setParentId(parentId);
        category.setTitle(alias);
        return category;
    }

    private DictionaryItem dictionaryItem(String id, String applicationAlias, String categoryAlias,
                                          String code, String parentId) {
        DictionaryItem item = new DictionaryItem();
        item.setId(id);
        item.setApplicationAlias(applicationAlias);
        item.setCategoryAlias(categoryAlias);
        item.setCode(code);
        item.setParentId(parentId);
        item.setTitle(code);
        return item;
    }

    private MetadataField metadataField(String id, String metadataId) {
        MetadataField field = new MetadataField();
        field.setId(id);
        field.setMetadataId(metadataId);
        return field;
    }

    private ModuleMetadataRelation relation(String id, String moduleAlias) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setId(id);
        relation.setModuleAlias(moduleAlias);
        return relation;
    }

    private ModuleMetadataField moduleField(String id, String relationId) {
        ModuleMetadataField field = new ModuleMetadataField();
        field.setId(id);
        field.setRelationId(relationId);
        return field;
    }

    private MetadataView metadataView(String id, String relationId, EntityViewType viewType) {
        MetadataView view = new MetadataView();
        view.setId(id);
        view.setRelationId(relationId);
        view.setViewType(viewType);
        view.setTitle(viewType.name());
        return view;
    }

    private MetadataViewField metadataViewField(String id, String viewId, String metadataFieldId) {
        MetadataViewField field = new MetadataViewField();
        field.setId(id);
        field.setViewId(viewId);
        field.setMetadataFieldId(metadataFieldId);
        field.setTitle(metadataFieldId);
        return field;
    }

    private void assertClause(Criteria criteria, String field, Object value) {
        CriteriaClause clause = criteria.getClauses().stream()
                .filter(item -> field.equals(item.getField()))
                .findFirst()
                .orElseThrow();
        assertThat(clause.getOperator()).isEqualTo(CriteriaOperator.EQ);
        assertThat(clause.getValues()).containsExactly(value);
    }

    private MockHttpServletRequest requestVars(Map<String, String> variables) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables);
        return request;
    }
}
