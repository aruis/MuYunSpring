package net.ximatai.muyun.spring.boot.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.boot.realtime.DataChangeRealtimePublisher;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ActionResultResponseAdviceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        ActionExecutionContextHolder.clear();
    }

    @Test
    void shouldNotDeriveStandardMutationFactsDuringResponseWrapping() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(Class::getSimpleName, objectMapper);
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
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(Class::getSimpleName, objectMapper);
        Method method = ChildController.class.getMethod("insert");
        MethodParameter returnType = new MethodParameter(method, -1);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            assertThat(advice.supports(returnType, null)).isTrue();
        }
    }

    @Test
    void shouldReportAnnotatedBusinessMutationResultBeforeWrapping() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(type -> DemoModule.MODULE_ALIAS, objectMapper);
        Method method = TestController.class.getDeclaredMethod("publish");
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("id", "record-1"));

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
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
            assertThat(actionResult.message().code()).isEqualTo("demo.published");
            assertThat(actionResult.message().text()).isEqualTo("已发布");
            assertThat(actionResult.changes()).singleElement().satisfies(change -> {
                assertThat(change.type()).isEqualTo("record-updated");
                assertThat(change.moduleAlias()).isEqualTo("demo.module");
                assertThat(change.recordId()).isEqualTo("record-1");
            });
        }
    }

    @Test
    void shouldReportAnnotatedCollectionChangedResultBeforeWrapping() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(type -> DemoModule.MODULE_ALIAS, objectMapper);
        Method method = TestController.class.getDeclaredMethod("grant");
        MethodParameter returnType = new MethodParameter(method, -1);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            Object response = advice.beforeBodyWrite(
                    1,
                    returnType,
                    MediaType.APPLICATION_JSON,
                    null,
                    new ServletServerHttpRequest(new MockHttpServletRequest()),
                    null
            );

            assertThat(response).isInstanceOf(ActionResultResponse.class);
            ActionResultResponse actionResult = (ActionResultResponse) response;
            assertThat(actionResult.message().code()).isEqualTo("demo.granted");
            assertThat(actionResult.changes()).singleElement().satisfies(change -> {
                assertThat(change.type()).isEqualTo("collection-changed");
                assertThat(change.moduleAlias()).isEqualTo("demo.module");
                assertThat(change.recordId()).isNull();
            });
        }
    }

    @Test
    void shouldPublishCommittedChangeSetAfterWrapping() throws Exception {
        RecordingDataChangeRealtimePublisher publisher = new RecordingDataChangeRealtimePublisher();
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(
                type -> DemoModule.MODULE_ALIAS, objectMapper, publisher);
        Method method = TestController.class.getDeclaredMethod("grant");
        MethodParameter returnType = new MethodParameter(method, -1);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            Object response = advice.beforeBodyWrite(
                    1,
                    returnType,
                    MediaType.APPLICATION_JSON,
                    null,
                    new ServletServerHttpRequest(new MockHttpServletRequest()),
                    null
            );

            assertThat(response).isInstanceOf(ActionResultResponse.class);
            assertThat(publisher.published).isNotNull();
            assertThat(publisher.published.changeSetId())
                    .isEqualTo(((ActionResultResponse) response).changeSetId());
            assertThat(publisher.published.changes())
                    .isEqualTo(((ActionResultResponse) response).changes());
        }
    }

    @Test
    void shouldKeepActionResultWhenDataChangePublishFails() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(
                type -> DemoModule.MODULE_ALIAS, objectMapper, new ThrowingDataChangeRealtimePublisher());
        Method method = TestController.class.getDeclaredMethod("grant");
        MethodParameter returnType = new MethodParameter(method, -1);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            Object response = advice.beforeBodyWrite(
                    1,
                    returnType,
                    MediaType.APPLICATION_JSON,
                    null,
                    new ServletServerHttpRequest(new MockHttpServletRequest()),
                    null
            );

            assertThat(response).isInstanceOf(ActionResultResponse.class);
            ActionResultResponse actionResult = (ActionResultResponse) response;
            assertThat(actionResult.message().code()).isEqualTo("demo.granted");
            assertThat(actionResult.changes()).singleElement()
                    .extracting("moduleAlias")
                    .isEqualTo("demo.module");
        }
    }

    @Test
    void shouldSerializeActionResultWhenStringConverterIsSelected() throws Exception {
        ActionResultResponseAdvice advice = new ActionResultResponseAdvice(type -> DemoModule.MODULE_ALIAS, objectMapper);
        Method method = TestController.class.getDeclaredMethod("grantString");
        MethodParameter returnType = new MethodParameter(method, -1);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        ServletServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(new MutationContext())) {
            Object body = advice.beforeBodyWrite(
                    "grant-1",
                    returnType,
                    MediaType.TEXT_PLAIN,
                    StringHttpMessageConverter.class,
                    new ServletServerHttpRequest(new MockHttpServletRequest()),
                    response
            );

            assertThat(servletResponse.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(body).isInstanceOf(String.class);
            assertThat((String) body)
                    .contains("\"data\":\"grant-1\"")
                    .contains("\"code\":\"demo.granted\"")
                    .contains("\"type\":\"collection-changed\"");
        }
    }

    private static final class TestController {
        @StandardMutation(StandardMutationKind.SORT)
        int sort() {
            return 1;
        }

        @BusinessMutationResult(code = "demo.published", message = "已发布",
                change = BusinessMutationChange.UPDATED, module = DemoModule.class,
                recordIdSource = BusinessMutationRecordIdSource.PATH_VARIABLE, recordId = "id")
        int publish() {
            return 1;
        }

        @BusinessMutationResult(code = "demo.granted", message = "已授权",
                change = BusinessMutationChange.COLLECTION_CHANGED, module = DemoModule.class)
        int grant() {
            return 1;
        }

        @BusinessMutationResult(code = "demo.granted", message = "已授权",
                change = BusinessMutationChange.COLLECTION_CHANGED, module = DemoModule.class)
        String grantString() {
            return "grant-1";
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

    public static final class DemoModule {
        public static final String MODULE_ALIAS = "demo.module";
    }

    private static final class RecordingDataChangeRealtimePublisher implements DataChangeRealtimePublisher {
        private CommittedChangeSet published;

        @Override
        public void publish(CommittedChangeSet changeSet) {
            this.published = changeSet;
        }
    }

    private static final class ThrowingDataChangeRealtimePublisher implements DataChangeRealtimePublisher {
        @Override
        public void publish(CommittedChangeSet changeSet) {
            throw new IllegalStateException("broker unavailable");
        }
    }
}
