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
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
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
