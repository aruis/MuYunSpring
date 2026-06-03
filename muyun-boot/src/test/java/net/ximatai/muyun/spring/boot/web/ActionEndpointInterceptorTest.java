package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
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

class ActionEndpointInterceptorTest {
    private final RecordingPolicyService policyService = new RecordingPolicyService();
    private final ActionEndpointInterceptor interceptor = new ActionEndpointInterceptor(
            policyService,
            new ActionEndpointContextResolver()
    );

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
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
                assertThat(context.permissionCode()).isEqualTo("iam.organization:query");
                assertThat(context.recordIds()).isEmpty();
                assertThat(context.currentUser()).get().extracting(CurrentUser::userId).isEqualTo("u1");
            });
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
                .hasMessageContaining("@ActionEndpoint requires module alias");
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
}
