package net.ximatai.muyun.spring.boot.workflow;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaClause;
import net.ximatai.muyun.database.core.orm.CriteriaOperator;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationScopeType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowDelegationWebControllerTest {
    private WorkflowDelegationService service;
    private WorkflowDelegationWebController controller;

    @BeforeEach
    void setUp() {
        service = mock(WorkflowDelegationService.class);
        controller = new WorkflowDelegationWebController(service);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldDeclareDelegationRoutesAndActionMetadata() throws Exception {
        assertThat(WorkflowDelegationWebController.class.getAnnotation(Path.class).value())
                .isEqualTo("/platform.workflow_delegation");
        PlatformStaticModule module = WorkflowDelegationWebController.class.getAnnotation(PlatformStaticModule.class);
        assertThat(module.alias()).isEqualTo(WorkflowDelegationService.MODULE_ALIAS);

        assertRoute("query", new Class<?>[]{WebQueryRequest.class}, "/query",
                "query", PlatformActionLevel.LIST, false);
        assertRoute("insert", new Class<?>[]{WorkflowDelegation.class}, "/insert",
                "create", PlatformActionLevel.LIST, false);
        assertRoute("update", new Class<?>[]{String.class, WorkflowDelegation.class}, "/update/{id}",
                "update", PlatformActionLevel.RECORD, true);
        assertRoute("delegatedToMe", new Class<?>[]{WebQueryRequest.class}, "/delegatedToMe/query",
                "delegatedToMeQuery", PlatformActionLevel.LIST, false);
        assertRoute("manageQuery", new Class<?>[]{WebQueryRequest.class}, "/manage/query",
                "manageQuery", PlatformActionLevel.LIST, false);
        assertRoute("manageEnable", new Class<?>[]{String.class}, "/manage/enable/{id}",
                "manageEnable", PlatformActionLevel.RECORD, true);
    }

    @Test
    void shouldUseCurrentUserForSelfDelegationMutations() {
        WorkflowDelegation saved = delegation("delegation-1", "principal-1", "delegate-1");
        when(service.insertForPrincipal(any(WorkflowDelegation.class), eq("principal-1"))).thenReturn(saved);
        when(service.updateForPrincipal(eq("delegation-1"), any(WorkflowDelegation.class), eq("principal-1")))
                .thenReturn(saved);
        when(service.deleteForPrincipal("delegation-1", "principal-1")).thenReturn(1);
        when(service.enableForPrincipal("delegation-1", "principal-1")).thenReturn(saved);
        when(service.disableForPrincipal("delegation-1", "principal-1")).thenReturn(saved);

        WorkflowDelegation inserted;
        WorkflowDelegation updated;
        WebCountResponse deleted;
        WorkflowDelegation enabled;
        WorkflowDelegation disabled;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            inserted = controller.insert(delegation("draft", "other", "delegate-1"));
            updated = controller.update("delegation-1", delegation("draft", "other", "delegate-1"));
            deleted = controller.delete("delegation-1");
            enabled = controller.enable("delegation-1");
            disabled = controller.disable("delegation-1");
        }

        assertThat(inserted).isSameAs(saved);
        assertThat(updated).isSameAs(saved);
        assertThat(deleted.count()).isEqualTo(1);
        assertThat(enabled).isSameAs(saved);
        assertThat(disabled).isSameAs(saved);
        ArgumentCaptor<WorkflowDelegation> insertedDelegation = ArgumentCaptor.forClass(WorkflowDelegation.class);
        verify(service).insertForPrincipal(insertedDelegation.capture(), eq("principal-1"));
        assertThat(insertedDelegation.getValue().getPrincipalUserId()).isEqualTo("other");
        verify(service).updateForPrincipal(eq("delegation-1"), any(WorkflowDelegation.class), eq("principal-1"));
        verify(service).deleteForPrincipal("delegation-1", "principal-1");
    }

    @Test
    void shouldQuerySelfAndDelegateScopesWithSafeFilters() {
        when(service.pageByPrincipal(eq("principal-1"), any(Criteria.class), any())).thenReturn(PageResult.of(List.of(
                delegation("mine", "principal-1", "delegate-1")), 1, PageRequest.of(2, 30)));
        when(service.pageByDelegate(eq("principal-1"), any(Criteria.class), any())).thenReturn(PageResult.of(List.of(
                delegation("from-other", "other", "principal-1")), 1, PageRequest.of(1, 20)));

        WebPageResponse<WorkflowDelegation> mine;
        WebPageResponse<WorkflowDelegation> delegatedToMe;
        try (CurrentUserContext.Scope ignored = currentUser()) {
            mine = controller.query(new WebQueryRequest(
                    new WebPageRequest(2, 30),
                    List.of(
                            new WebQueryCondition("enabled", "EQ", List.of(true)),
                            new WebQueryCondition("moduleScopeType", "=", List.of("include"))
                    ),
                    List.of()
            ));
            delegatedToMe = controller.delegatedToMe(new WebQueryRequest(null, List.of(
                    new WebQueryCondition("principalCanProcess", null, List.of("false"))
            ), List.of()));
        }

        assertThat(mine.records()).singleElement()
                .extracting(WorkflowDelegation::getPrincipalUserId)
                .isEqualTo("principal-1");
        assertThat(delegatedToMe.records()).singleElement()
                .extracting(WorkflowDelegation::getDelegateUserId)
                .isEqualTo("principal-1");
        ArgumentCaptor<Criteria> selfCriteria = ArgumentCaptor.forClass(Criteria.class);
        ArgumentCaptor<PageRequest> selfPage = ArgumentCaptor.forClass(PageRequest.class);
        verify(service).pageByPrincipal(eq("principal-1"), selfCriteria.capture(), selfPage.capture());
        assertThat(selfPage.getValue().getOffset()).isEqualTo(30);
        assertThat(selfPage.getValue().getLimit()).isEqualTo(30);
        assertClause(selfCriteria.getValue(), "enabled", Boolean.TRUE);
        assertClause(selfCriteria.getValue(), "moduleScopeType", WorkflowDelegationScopeType.INCLUDE);

        ArgumentCaptor<Criteria> delegatedCriteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageByDelegate(eq("principal-1"), delegatedCriteria.capture(), any());
        assertClause(delegatedCriteria.getValue(), "principalCanProcess", Boolean.FALSE);
    }

    @Test
    void shouldRejectUnsafeQueryFieldsOperatorsAndSorts() {
        assertThatThrownBy(() -> controller.query(new WebQueryRequest(null, List.of(
                new WebQueryCondition("principalUserId", "EQ", List.of("other"))
        ), List.of())))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("field is not allowed");
        assertThatThrownBy(() -> controller.query(new WebQueryRequest(null, List.of(
                new WebQueryCondition("enabled", "LIKE", List.of(true))
        ), List.of())))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports EQ");
        assertThatThrownBy(() -> controller.query(new WebQueryRequest(null, List.of(), List.of(
                new WebSort("updatedAt", true)
        ))))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("does not support custom sorts");
    }

    @Test
    void shouldExposeManagementDelegationActionsWithoutCurrentUserOverride() {
        WorkflowDelegation managed = delegation("managed", "other", "delegate-1");
        when(service.pageQuery(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(PageResult.of(List.of(managed), 1, PageRequest.of(1, 20)));
        when(service.insert(any(WorkflowDelegation.class))).thenReturn("delegation-1");
        when(service.select("delegation-1")).thenReturn(managed);
        when(service.update(any(WorkflowDelegation.class))).thenReturn(1);
        when(service.select("delegation-2")).thenReturn(managed);
        when(service.delete("delegation-1")).thenReturn(1);
        when(service.enable(anyString())).thenReturn(managed);
        when(service.disable(anyString())).thenReturn(managed);

        WebPageResponse<WorkflowDelegation> query = controller.manageQuery(new WebQueryRequest(null, List.of(
                new WebQueryCondition("orgScopeType", "EQ", List.of("ALL"))
        ), List.of()));
        WorkflowDelegation inserted = controller.manageInsert(delegation("draft", "other", "delegate-1"));
        WorkflowDelegation updated = controller.manageUpdate("delegation-2", delegation("draft", "other", "delegate-1"));
        WebCountResponse deleted = controller.manageDelete("delegation-1");
        WorkflowDelegation enabled = controller.manageEnable("delegation-1");
        WorkflowDelegation disabled = controller.manageDisable("delegation-1");

        assertThat(query.records()).containsExactly(managed);
        assertThat(inserted.getPrincipalUserId()).isEqualTo("other");
        assertThat(updated).isSameAs(managed);
        assertThat(deleted.count()).isEqualTo(1);
        assertThat(enabled).isSameAs(managed);
        assertThat(disabled).isSameAs(managed);
        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        verify(service).pageQuery(criteria.capture(), any(PageRequest.class), any(Sort.class), any(Sort.class));
        assertClause(criteria.getValue(), "orgScopeType", WorkflowDelegationScopeType.ALL);
        ArgumentCaptor<WorkflowDelegation> updateCaptor = ArgumentCaptor.forClass(WorkflowDelegation.class);
        verify(service).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo("delegation-2");
        assertThat(updateCaptor.getValue().getPrincipalUserId()).isEqualTo("other");
    }

    private CurrentUserContext.Scope currentUser() {
        return CurrentUserContext.use(CurrentUser.tenantUser("principal-1", "Principal", "tenant-a"));
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

    private void assertRoute(String methodName,
                             Class<?>[] parameterTypes,
                             String path,
                             String actionCode,
                             PlatformActionLevel level,
                             boolean dataAuth) throws Exception {
        Method method = WorkflowDelegationWebController.class.getMethod(methodName, parameterTypes);
        assertThat(method.getAnnotation(POST.class.asSubclass(Annotation.class))).isNotNull();
        assertThat(method.getAnnotation(Path.class).value()).isEqualTo(path);
        CustomActionEndpoint endpoint = method.getAnnotation(CustomActionEndpoint.class);
        assertThat(endpoint.value()).isEqualTo(actionCode);
        assertThat(endpoint.level()).isEqualTo(level);
        assertThat(endpoint.dataAuth()).isEqualTo(dataAuth);
    }
}
