package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaGroup;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRule;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRuleService;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordLinkageRuleWebControllerTest {
    @Test
    void shouldDeclareGenerationRuleTreeRoutes() throws Exception {
        assertThat(RecordGenerationRuleWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module/{moduleAlias}/generation-rules");
        assertCustomRoute(RecordGenerationRuleWebController.class, "viewTree",
                new Class<?>[]{UriInfo.class, String.class}, GET.class, "/viewTree/{id}",
                "viewTree", PlatformActionLevel.RECORD);
        assertCustomRoute(RecordGenerationRuleWebController.class, "saveTree",
                new Class<?>[]{UriInfo.class, RecordGenerationRule.class}, POST.class, "/saveTree",
                "saveTree", PlatformActionLevel.ANY);
        assertInheritedQueryRoute(RecordGenerationRuleWebController.class);
    }

    @Test
    void shouldDeclareWriteBackRuleTreeRoutes() throws Exception {
        assertThat(RecordWriteBackRuleWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.module/{moduleAlias}/write-back-rules");
        assertCustomRoute(RecordWriteBackRuleWebController.class, "viewTree",
                new Class<?>[]{UriInfo.class, String.class}, GET.class, "/viewTree/{id}",
                "viewTree", PlatformActionLevel.RECORD);
        assertCustomRoute(RecordWriteBackRuleWebController.class, "saveTree",
                new Class<?>[]{UriInfo.class, RecordWriteBackRule.class}, POST.class, "/saveTree",
                "saveTree", PlatformActionLevel.ANY);
        assertInheritedQueryRoute(RecordWriteBackRuleWebController.class);
    }

    @Test
    void shouldBindGenerationRuleSourceModuleFromPathWhenSavingTree() throws Exception {
        RecordGenerationRuleService service = mock(RecordGenerationRuleService.class);
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        setService(controller, service);
        RecordGenerationRule saved = generationRule("rule-1", "sales.contract");
        saved.setTargetModuleAlias("sales.invoice");
        when(service.saveRuleTree(any(RecordGenerationRule.class))).thenReturn(saved);

        RecordGenerationRule result = controller.saveTree(
                requestVars("sales.contract"),
                generationRule(null, "other.module"));

        assertThat(result.getSourceModuleAlias()).isEqualTo("sales.contract");
        ArgumentCaptor<RecordGenerationRule> captor = ArgumentCaptor.forClass(RecordGenerationRule.class);
        verify(service).saveRuleTree(captor.capture());
        assertThat(captor.getValue().getSourceModuleAlias()).isEqualTo("sales.contract");
    }

    @Test
    void shouldRejectGenerationRuleTreeUpdateOutsidePathModule() throws Exception {
        RecordGenerationRuleService service = mock(RecordGenerationRuleService.class);
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        setService(controller, service);
        when(service.select("rule-1")).thenReturn(generationRule("rule-1", "sales.contract"));

        assertThatThrownBy(() -> controller.saveTree(
                requestVars("sales.invoice"), generationRule("rule-1", "sales.invoice")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rule does not belong to module");
    }

    @Test
    void shouldQueryAndViewGenerationRulesWithinPathModule() throws Exception {
        RecordGenerationRuleService service = queryService(mock(RecordGenerationRuleService.class));
        RecordGenerationRuleWebController controller = new RecordGenerationRuleWebController();
        setService(controller, service);
        RecordGenerationRule rule = generationRule("rule-1", "sales.contract");
        rule.setActionCode("generateInvoice");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(rule), 1, PageRequest.of(1, 20)));
        when(service.select("rule-1")).thenReturn(rule);
        when(service.viewRuleTree("rule-1")).thenReturn(rule);

        WebPageResponse<RecordGenerationRule> response = controller.query(
                requestVars("sales.contract"),
                new WebQueryRequest(null,
                        List.of(new WebQueryCondition("actionCode", null, List.of("generateInvoice"))),
                        null));
        RecordGenerationRule viewed = controller.viewTree(requestVars("sales.contract"), "rule-1");

        assertThat(response.records()).singleElement()
                .extracting(RecordGenerationRule::getSourceModuleAlias)
                .isEqualTo("sales.contract");
        assertThat(viewed.getId()).isEqualTo("rule-1");
        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort[].class));
        assertClause(criteria.getValue(), "sourceModuleAlias", "sales.contract");
        assertClause(criteria.getValue(), "actionCode", "generateInvoice");
        verify(service).viewRuleTree("rule-1");
    }

    @Test
    void shouldBindWriteBackRuleTriggerModuleFromPathWhenSavingTree() throws Exception {
        RecordWriteBackRuleService service = mock(RecordWriteBackRuleService.class);
        RecordWriteBackRuleWebController controller = new RecordWriteBackRuleWebController();
        setService(controller, service);
        RecordWriteBackRule saved = writeBackRule("rule-1", "sales.invoice");
        saved.setTargetModuleAlias("sales.contract");
        when(service.saveRuleTree(any(RecordWriteBackRule.class))).thenReturn(saved);

        RecordWriteBackRule result = controller.saveTree(
                requestVars("sales.invoice"),
                writeBackRule(null, "other.module"));

        assertThat(result.getTriggerModuleAlias()).isEqualTo("sales.invoice");
        ArgumentCaptor<RecordWriteBackRule> captor = ArgumentCaptor.forClass(RecordWriteBackRule.class);
        verify(service).saveRuleTree(captor.capture());
        assertThat(captor.getValue().getTriggerModuleAlias()).isEqualTo("sales.invoice");
    }

    @Test
    void shouldRejectWriteBackRuleTreeUpdateOutsidePathModule() throws Exception {
        RecordWriteBackRuleService service = mock(RecordWriteBackRuleService.class);
        RecordWriteBackRuleWebController controller = new RecordWriteBackRuleWebController();
        setService(controller, service);
        when(service.select("rule-1")).thenReturn(writeBackRule("rule-1", "sales.invoice"));

        assertThatThrownBy(() -> controller.saveTree(
                requestVars("sales.contract"), writeBackRule("rule-1", "sales.contract")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rule does not belong to module");
    }

    private RecordGenerationRule generationRule(String id, String sourceModuleAlias) {
        RecordGenerationRule rule = new RecordGenerationRule();
        rule.setId(id);
        rule.setSourceModuleAlias(sourceModuleAlias);
        rule.setTitle("Generate invoice");
        return rule;
    }

    private RecordWriteBackRule writeBackRule(String id, String triggerModuleAlias) {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setId(id);
        rule.setTriggerModuleAlias(triggerModuleAlias);
        rule.setTargetModuleAlias("sales.contract");
        rule.setTitle("Write back contract");
        return rule;
    }

    private void assertClause(Criteria criteria, String field, Object value) {
        CriteriaClause clause = clauses(criteria).stream()
                .filter(item -> field.equals(item.getField()))
                .findFirst()
                .orElseThrow();
        assertThat(clause.getValues()).containsExactly(value);
    }

    private List<CriteriaClause> clauses(Criteria criteria) {
        List<CriteriaClause> result = new ArrayList<>();
        collect(criteria.getRoot(), result);
        return result;
    }

    private void collect(CriteriaGroup group, List<CriteriaClause> result) {
        for (CriteriaGroup.Entry entry : group.getEntries()) {
            Object node = criteriaNode(entry);
            if (node instanceof CriteriaClause clause) {
                result.add(clause);
            } else if (node instanceof CriteriaGroup childGroup) {
                collect(childGroup, result);
            }
        }
    }

    private Object criteriaNode(CriteriaGroup.Entry entry) {
        try {
            Method method = entry.getClass().getMethod("getNode");
            return method.invoke(entry);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read criteria node", e);
        }
    }

    private <S extends QueryAbility<?>> S queryService(S service) {
        doCallRealMethod().when(service).queryDescriptor();
        doCallRealMethod().when(service).queryCriteria(any(QueryRequest.class));
        doCallRealMethod().when(service).querySorts(any(QueryRequest.class));
        return service;
    }

    private UriInfo requestVars(String moduleAlias) {
        UriInfo request = mock(UriInfo.class);
        MultivaluedHashMap<String, String> parameters = new MultivaluedHashMap<>();
        parameters.putSingle("moduleAlias", moduleAlias);
        when(request.getPathParameters()).thenReturn(parameters);
        return request;
    }

    private void setService(Object target, Object service) throws ReflectiveOperationException {
        Field field = WebSupport.class.getDeclaredField("service");
        field.setAccessible(true);
        field.set(target, service);
    }

    private void assertCustomRoute(Class<?> controllerClass,
                                   String methodName,
                                   Class<?>[] parameterTypes,
                                   Class<?> httpMethod,
                                   String path,
                                   String actionCode,
                                   PlatformActionLevel level) throws Exception {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(httpMethod.asSubclass(java.lang.annotation.Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        assertThat(method.getParameters()[0].getAnnotation(Context.class)).isNotNull();
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(level);
        if (parameterTypes.length > 1 && parameterTypes[1] == String.class) {
            assertThat(method.getParameters()[1].getAnnotation(PathParam.class).value()).isEqualTo("id");
            assertThat(endpoint.recordIdPathVariable()).isEqualTo("id");
        }
    }

    private void assertInheritedQueryRoute(Class<?> controllerClass) throws Exception {
        Method method = controllerClass.getMethod("query", UriInfo.class, WebQueryRequest.class);
        assertThat(method.getAnnotation(POST.class)).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo("/query");
        assertThat(method.getParameters()[0].getAnnotation(Context.class)).isNotNull();
        assertThat(method.getAnnotation(ActionEndpoint.class).value()).isEqualTo(PlatformAction.QUERY);
    }
}
