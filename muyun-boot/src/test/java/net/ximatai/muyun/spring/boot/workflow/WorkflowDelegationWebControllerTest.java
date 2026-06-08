package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationScopeType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowDelegationWebControllerTest {
    private WorkflowDelegationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(WorkflowDelegationService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new WorkflowDelegationWebController(service))
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("principal-1", "Principal", "tenant-a"))))
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldForceCurrentUserAsSelfPrincipalOnInsertAndUpdate() throws Exception {
        WorkflowDelegation inserted = delegation("created", "principal-1", "delegate-1");
        when(service.insertForPrincipal(any(WorkflowDelegation.class), eq("principal-1"))).thenReturn(inserted);
        when(service.updateForPrincipal(eq("delegation-1"), any(WorkflowDelegation.class), eq("principal-1")))
                .thenReturn(inserted);

        mvc.perform(post("/workflow/delegation/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"created","principalUserId":"other","delegateUserId":"delegate-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principalUserId").value("principal-1"));
        mvc.perform(post("/workflow/delegation/update/delegation-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"created","principalUserId":"other","delegateUserId":"delegate-1"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkflowDelegation> captor = ArgumentCaptor.forClass(WorkflowDelegation.class);
        verify(service).insertForPrincipal(captor.capture(), eq("principal-1"));
        assertThat(captor.getValue().getPrincipalUserId()).isEqualTo("other");
        verify(service).updateForPrincipal(eq("delegation-1"), any(WorkflowDelegation.class), eq("principal-1"));
    }

    @Test
    void shouldQuerySelfAndDelegatedToMeByCurrentUserWithSafeFilters() throws Exception {
        when(service.pageByPrincipal(eq("principal-1"), any(Criteria.class), any())).thenReturn(PageResult.of(List.of(
                delegation("mine", "principal-1", "delegate-1")), 1, PageRequest.of(1, 20)));
        when(service.pageByDelegate(eq("principal-1"), any(Criteria.class), any())).thenReturn(PageResult.of(List.of(
                delegation("from-other", "other", "principal-1")), 1, PageRequest.of(1, 20)));

        mvc.perform(post("/workflow/delegation/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[
                                  {"fieldName":"enabled","operator":"EQ","values":[true]},
                                  {"fieldName":"moduleScopeType","values":["include"]}
                                ]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].principalUserId").value("principal-1"));
        mvc.perform(post("/workflow/delegation/delegatedToMe/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"principalCanProcess","operator":"=","values":["false"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].delegateUserId").value("principal-1"));

        ArgumentCaptor<Criteria> selfCriteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageByPrincipal(eq("principal-1"), selfCriteria.capture(), any());
        assertClause(selfCriteria.getValue(), "enabled", Boolean.TRUE);
        assertClause(selfCriteria.getValue(), "moduleScopeType", WorkflowDelegationScopeType.INCLUDE);

        ArgumentCaptor<Criteria> delegatedCriteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageByDelegate(eq("principal-1"), delegatedCriteria.capture(), any());
        assertClause(delegatedCriteria.getValue(), "principalCanProcess", Boolean.FALSE);
    }

    @Test
    void shouldAllowManageQuerySafeFilters() throws Exception {
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(), any()))
                .thenReturn(PageResult.of(List.of(delegation("managed", "other", "delegate-1")),
                        1, PageRequest.of(1, 20)));

        mvc.perform(post("/workflow/delegation/manage/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conditions":[{"fieldName":"orgScopeType","values":["ALL"]}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].title").value("managed"));

        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(), any());
        assertClause(criteria.getValue(), "orgScopeType", WorkflowDelegationScopeType.ALL);
    }

    @Test
    void shouldRejectUnsafeDelegationQueryFields() {
        WorkflowDelegationWebController controller = new WorkflowDelegationWebController(service);

        assertThatThrownBy(() -> controller.query(new WebQueryRequest(null, List.of(
                new WebQueryCondition("principalUserId", "EQ", List.of("other"))
        ), List.of())))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("field is not allowed");
    }

    @Test
    void shouldNotOverridePrincipalForManageInsert() throws Exception {
        WorkflowDelegation managed = delegation("managed", "other", "delegate-1");
        when(service.insert(any(WorkflowDelegation.class))).thenReturn("delegation-1");
        when(service.select("delegation-1")).thenReturn(managed);

        mvc.perform(post("/workflow/delegation/manage/insert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"managed","principalUserId":"other","delegateUserId":"delegate-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principalUserId").value("other"));

        ArgumentCaptor<WorkflowDelegation> captor = ArgumentCaptor.forClass(WorkflowDelegation.class);
        verify(service).insert(captor.capture());
        assertThat(captor.getValue().getPrincipalUserId()).isEqualTo("other");
    }

    @Test
    void shouldExposeDelegationAsStaticActionModule() throws Exception {
        PlatformStaticModule module = WorkflowDelegationWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.alias()).isEqualTo(WorkflowDelegationService.MODULE_ALIAS);

        assertCustomAction("query", "query", PlatformActionLevel.LIST, false, WebQueryRequest.class);
        assertCustomAction("insert", "create", PlatformActionLevel.LIST, false, WorkflowDelegation.class);
        assertCustomAction("update", "update", PlatformActionLevel.RECORD, true,
                String.class, WorkflowDelegation.class);
        assertCustomAction("delegatedToMe", "delegatedToMeQuery", PlatformActionLevel.LIST, false,
                WebQueryRequest.class);
        assertCustomAction("manageQuery", "manageQuery", PlatformActionLevel.LIST, false,
                WebQueryRequest.class);
        assertCustomAction("manageEnable", "manageEnable", PlatformActionLevel.RECORD, true, String.class);
    }

    private WorkflowDelegation delegation(String title, String principal, String delegate) {
        WorkflowDelegation delegation = new WorkflowDelegation();
        delegation.setId(title);
        delegation.setTitle(title);
        delegation.setPrincipalUserId(principal);
        delegation.setDelegateUserId(delegate);
        return delegation;
    }

    private void assertClause(Criteria criteria, String field, Object value) {
        CriteriaClause clause = criteria.getClauses().stream()
                .filter(item -> field.equals(item.getField()))
                .findFirst()
                .orElseThrow();
        assertThat(clause.getOperator()).isEqualTo(CriteriaOperator.EQ);
        assertThat(clause.getValues()).containsExactly(value);
    }

    private void assertCustomAction(String methodName, String actionCode, PlatformActionLevel level,
                                    boolean dataAuth, Class<?>... parameterTypes) throws Exception {
        Method method = WorkflowDelegationWebController.class.getMethod(methodName, parameterTypes);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(level);
        assertThat(endpoint.dataAuth()).isEqualTo(dataAuth);
    }
}
