package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResultResponseAdviceTest {
    @AfterEach
    void tearDown() {
        ActionExecutionContextHolder.clear();
    }

    @Test
    void shouldNotDeriveStandardMutationFactsDuringResponseWrapping() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(Class::getSimpleName);
        Method method = TestController.class.getDeclaredMethod("sort");
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("id", "record-1"));

        MutationContext mutationContext = new MutationContext();
        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(mutationContext);
             ActionExecutionContextHolder.Scope ignoredAction = ActionExecutionContextHolder.use(
                     ActionExecutionContext.ofPlatformAction("demo.record", PlatformAction.SORT,
                             Set.of("record-1"), Optional.empty()))) {
            Object response = advice.beforeBodyWrite(
                    1,
                    returnType,
                    MediaType.APPLICATION_JSON,
                    null,
                    new ServletServerHttpRequest(servletRequest),
                    null
            );

            assertThat(response).isInstanceOf(ActionResultResponse.class);
            ActionResultResponse actionResult = (ActionResultResponse) response;
            assertThat(actionResult.data()).isEqualTo(1);
            assertThat(actionResult.message()).isNull();
            assertThat(actionResult.changeSetId()).isEqualTo(mutationContext.changeSetId());
            assertThat(actionResult.changes()).isEmpty();
        }
    }

    @Test
    void shouldSupportInheritedStandardMutationHandler() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(Class::getSimpleName);
        Method method = ChildController.class.getMethod("insert");
        MethodParameter returnType = new MethodParameter(method, -1);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            assertThat(advice.supports(returnType, null)).isTrue();
        }
    }

    private static final class TestController {
        @StandardMutation(StandardMutationKind.SORT)
        int sort() {
            return 1;
        }
    }

    private abstract static class BaseController {
        @StandardMutation(StandardMutationKind.CREATE)
        public int insert() {
            return 1;
        }
    }

    private static final class ChildController extends BaseController {
    }
}
