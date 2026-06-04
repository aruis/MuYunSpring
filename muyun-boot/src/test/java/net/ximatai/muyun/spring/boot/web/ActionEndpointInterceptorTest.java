package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.boot.iam.RoleWebController;
import net.ximatai.muyun.spring.boot.iam.UserAccountWebController;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionEndpointInterceptorTest {
    private final RecordingPolicyService policyService = new RecordingPolicyService();
    private final ActionEndpointInterceptor interceptor = new ActionEndpointInterceptor(
            policyService,
            new ActionEndpointContextResolver()
    );

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        ActionExecutionContextHolder.clear();
    }

    @Test
    void shouldResolveStaticScopedWebActionContext() throws Exception {
        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(CurrentUser.tenantUser("u1", "User", "t1"))) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.organization/query");

            interceptor.preHandle(request, new MockHttpServletResponse(),
                    handler(new StaticScopedWeb(), CrudWeb.class.getMethod("query", WebQueryRequest.class)));

            assertThat(policyService.context).satisfies(context -> {
                assertThat(context.moduleAlias()).isEqualTo("iam.organization");
                assertThat(context.platformAction()).isEqualTo(PlatformAction.QUERY);
                assertThat(context.actionCode()).isEqualTo("query");
                assertThat(context.permissionCode()).isEqualTo("iam.organization:view");
                assertThat(context.recordIds()).isEmpty();
                assertThat(context.currentUser()).get().extracting(CurrentUser::userId).isEqualTo("u1");
                assertThat(context.authorizationResult()).isNotNull();
                assertThat(context.authorizationResult().operatorId()).isEqualTo("u1");
            });
            assertThat(ActionExecutionContextHolder.current()).contains(policyService.context);
            interceptor.afterCompletion(request, new MockHttpServletResponse(),
                    handler(new StaticScopedWeb(), CrudWeb.class.getMethod("query", WebQueryRequest.class)), null);
            assertThat(ActionExecutionContextHolder.current()).isEmpty();
        }
    }

    @Test
    void shouldPreferDynamicModuleAliasFromPathAndCollectRecordIds() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sales.contract/update/contract-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(
                "moduleAlias", "sales.contract",
                "recordId", "contract-1"
        ));
        request.addParameter("ids", "contract-2, contract-3");

        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new StaticScopedWeb(), CrudWeb.class.getMethod("update", String.class, EntityContract.class)));

        assertThat(policyService.context).satisfies(context -> {
            assertThat(context.moduleAlias()).isEqualTo("sales.contract");
            assertThat(context.platformAction()).isEqualTo(PlatformAction.UPDATE);
            assertThat(context.actionCode()).isEqualTo("update");
            assertThat(context.recordIds()).containsExactlyInAnyOrder("contract-1", "contract-2", "contract-3");
        });
    }

    @Test
    void shouldClearActionContextWhenAsyncHandlingStarts() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.organization/update/org-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "org-1"));
        HandlerMethod handler = handler(new StaticScopedWeb(), CrudWeb.class.getMethod("update", String.class, EntityContract.class));

        interceptor.preHandle(request, new MockHttpServletResponse(), handler);
        assertThat(ActionExecutionContextHolder.current()).contains(policyService.context);

        interceptor.afterConcurrentHandlingStarted(request, new MockHttpServletResponse(), handler);

        assertThat(ActionExecutionContextHolder.current()).isEmpty();
    }

    @Test
    void shouldResolveRoleCustomEndpointActionContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.role/grant/role-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("roleId", "role-1"));
        RoleWebController controller = new RoleWebController(null);

        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(controller, RoleWebController.class.getMethod(
                        "grantAction", String.class, RoleWebController.GrantActionRequest.class)));

        assertThat(policyService.context).satisfies(context -> {
            assertThat(context.moduleAlias()).isEqualTo("iam.role");
            assertThat(context.platformAction()).isEqualTo(PlatformAction.UPDATE);
            assertThat(context.permissionCode()).isEqualTo("iam.role:update");
        });
    }

    @Test
    void shouldResolveUserManagementEndpointActionContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.user/changePassword/user-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "user-1"));
        UserAccountWebController controller = new UserAccountWebController(null);

        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(controller, UserAccountWebController.class.getMethod(
                        "changePassword", String.class, UserAccountWebController.ChangePasswordRequest.class)));

        assertThat(policyService.context).satisfies(context -> {
            assertThat(context.moduleAlias()).isEqualTo("iam.user");
            assertThat(context.platformAction()).isNull();
            assertThat(context.actionCode()).isEqualTo("changePassword");
            assertThat(context.permissionCode()).isEqualTo("iam.user:changePassword");
            assertThat(context.actionPolicy().requiresDataScope()).isTrue();
            assertThat(context.recordIds()).containsExactly("user-1");
        });
    }

    @Test
    void shouldUseRegisteredCustomActionPolicyWhenResolvingWebEndpoint() throws Exception {
        PlatformModuleActionService moduleActionService = mock(PlatformModuleActionService.class);
        PlatformModuleAction action = new PlatformModuleAction();
        action.setModuleAlias("iam.user");
        action.setActionCode("changePassword");
        action.setPermissionActionCode("update");
        action.setActionLevel(EntityActionLevel.RECORD);
        action.setActionAuth(Boolean.TRUE);
        action.setDataAuth(Boolean.TRUE);
        action.setEnabled(Boolean.TRUE);
        when(moduleActionService.findByModuleAliasAndActionCode("iam.user", "changePassword"))
                .thenReturn(action);
        ActionEndpointInterceptor interceptor = new ActionEndpointInterceptor(
                policyService,
                new ActionEndpointContextResolver(moduleActionService)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.user/changePassword/user-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "user-1"));
        UserAccountWebController controller = new UserAccountWebController(null);

        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(controller, UserAccountWebController.class.getMethod(
                        "changePassword", String.class, UserAccountWebController.ChangePasswordRequest.class)));

        assertThat(policyService.context).satisfies(context -> {
            assertThat(context.moduleAlias()).isEqualTo("iam.user");
            assertThat(context.actionCode()).isEqualTo("changePassword");
            assertThat(context.permissionCode()).isEqualTo("iam.user:update");
            assertThat(context.actionPolicy().requiresDataScope()).isTrue();
            assertThat(context.recordIds()).containsExactly("user-1");
        });
    }

    @Test
    void shouldRejectEndpointMethodWithBothStandardAndCustomActionAnnotations() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/iam.organization/invalid");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new InvalidDualActionWeb(), InvalidDualActionWeb.class.getMethod("invalid"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("both standard and custom action endpoint");
    }


    @Test
    void shouldIgnoreEndpointWithoutActionAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/iam.organization/actions");

        interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new StaticScopedWeb(), ActionWeb.class.getMethod("actions")));

        assertThat(policyService.context).isNull();
    }

    @Test
    void shouldFailFastWhenActionEndpointCannotResolveModuleAlias() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/query");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(),
                handler(new Object(), CrudWeb.class.getMethod("query", WebQueryRequest.class))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("action endpoint requires module alias");
    }

    private HandlerMethod handler(Object bean, Method method) {
        return new HandlerMethod(bean, method);
    }

    private static final class RecordingPolicyService implements ActionExecutionPolicyService {
        private ActionExecutionContext context;

        @Override
        public void requireAuthorized(ActionExecutionContext context) {
            this.context = context;
        }

        @Override
        public ActionAuthorizationResult authorize(ActionExecutionContext context) {
            ActionAuthorizationResult result = ActionAuthorizationResult.allowed(context, "TEST_ALLOWED");
            this.context = context.withAuthorizationResult(result);
            return result;
        }
    }

    private static final class StaticScopedWeb implements ScopedWeb<Object> {
        @Override
        public String webScopeName() {
            return "iam.organization";
        }

        @Override
        public Object service() {
            return new Object();
        }
    }

    private static final class InvalidDualActionWeb implements ScopedWeb<Object> {
        @ActionEndpoint(PlatformAction.UPDATE)
        @net.ximatai.muyun.spring.common.platform.CustomActionEndpoint("custom")
        public void invalid() {
        }

        @Override
        public String webScopeName() {
            return "iam.organization";
        }

        @Override
        public Object service() {
            return new Object();
        }
    }
}
