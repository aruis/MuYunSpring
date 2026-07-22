package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.spring.ability.action.BusinessExceptions;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformWebExceptionHandlerTest {
    @AfterEach
    void tearDown() {
        RequestTraceContext.clear();
    }

    @Test
    void shouldReturnUnifiedEnvelopeForPlatformExceptionWithTargets() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/validation")
                        .header(RequestTraceContext.TRACE_ID_HEADER, "trace-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string(RequestTraceContext.TRACE_ID_HEADER, "trace-1"))
                .andExpect(jsonPath("$.traceId").value("trace-1"))
                .andExpect(jsonPath("$.code").value("DYNAMIC_FIELD_REQUIRED"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("客户名称不能为空"))
                .andExpect(jsonPath("$.actionMessage").doesNotExist())
                .andExpect(jsonPath("$.targets[0].kind").value("field"))
                .andExpect(jsonPath("$.targets[0].fieldName").value("customerName"))
                .andExpect(jsonPath("$.targets[0].relationAlias").value("main"));
    }

    @Test
    void shouldReturnActionMessageForBusinessExceptionWithoutLosingHttpStatus() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/business")
                        .header(RequestTraceContext.TRACE_ID_HEADER, "trace-business")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.traceId").value("trace-business"))
                .andExpect(jsonPath("$.code").value("iam.employee-account.username-occupied"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("登录账号已被占用"))
                .andExpect(jsonPath("$.messageArgs.username").value("demo-admin"))
                .andExpect(jsonPath("$.actionMessage.code").value("iam.employee-account.username-occupied"))
                .andExpect(jsonPath("$.actionMessage.text").value("登录账号已被占用"))
                .andExpect(jsonPath("$.actionMessage.messageArgs.username").value("demo-admin"))
                .andExpect(jsonPath("$.actionMessage.type").value("WARNING"));
    }

    @Test
    void shouldReturnUnifiedEnvelopeForAuthenticationRequired() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/auth"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.code").value(PlatformErrorCodes.AUTH_REQUIRED))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("current user context is not available"))
                .andExpect(jsonPath("$.actionMessage.code").value(PlatformErrorCodes.AUTH_REQUIRED))
                .andExpect(jsonPath("$.actionMessage.text").value("current user context is not available"))
                .andExpect(jsonPath("$.actionMessage.type").value("ERROR"));
    }

    @Test
    void shouldReturnUnifiedEnvelopeForConfigurationErrorWithScope() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/config")
                        .header("X-Trace-Id", "trace-2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.traceId").value("trace-2"))
                .andExpect(jsonPath("$.code").value("DYNAMIC_DESCRIPTOR_MISSING"))
                .andExpect(jsonPath("$.scope.moduleAlias").value("crm.customer"));
    }

    @Test
    void shouldReturnWarningActionMessageForOptimisticLockConflict() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(PlatformErrorCodes.CONFLICT_VERSION))
                .andExpect(jsonPath("$.message").value("record version conflict"))
                .andExpect(jsonPath("$.actionMessage.code").value(PlatformErrorCodes.CONFLICT_VERSION))
                .andExpect(jsonPath("$.actionMessage.text").value("record version conflict"))
                .andExpect(jsonPath("$.actionMessage.type").value("WARNING"));
    }

    @Test
    void shouldReturnWarningActionMessageForBadRequest() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PlatformErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.message").value("name must not be blank"))
                .andExpect(jsonPath("$.actionMessage.code").value(PlatformErrorCodes.VALIDATION_FAILED))
                .andExpect(jsonPath("$.actionMessage.text").value("name must not be blank"))
                .andExpect(jsonPath("$.actionMessage.type").value("WARNING"));
    }

    @Test
    void shouldHideUnexpectedExceptionMessage() throws Exception {
        MockMvc mvc = mvc(new DemoController());

        mvc.perform(get("/demo/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(PlatformErrorCodes.INTERNAL_ERROR))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .addFilters(new RequestTraceWebFilter())
                .setControllerAdvice(new PlatformWebExceptionHandler())
                .build();
    }

    @RestController
    private static class DemoController {
        @GetMapping("/demo/validation")
        String validation() {
            throw PlatformErrors.validation("DYNAMIC_FIELD_REQUIRED", "客户名称不能为空",
                    ErrorTarget.field("customerName").relation("main"));
        }

        @GetMapping("/demo/business")
        String business() {
            throw BusinessExceptions.warning("iam.employee-account.username-occupied", "登录账号已被占用",
                    Map.of("username", "demo-admin"));
        }

        @GetMapping("/demo/auth")
        String auth() {
            throw new AuthenticationRequiredException("current user context is not available");
        }

        @GetMapping("/demo/config")
        String config() {
            throw PlatformErrors.config("DYNAMIC_DESCRIPTOR_MISSING", "模块页面配置不存在",
                    ErrorScope.module("crm.customer"));
        }

        @GetMapping("/demo/optimistic-lock")
        String optimisticLock() {
            throw new OptimisticLockException("record version conflict");
        }

        @GetMapping("/demo/bad-request")
        String badRequest() {
            throw new IllegalArgumentException("name must not be blank");
        }

        @GetMapping("/demo/unexpected")
        String unexpected() {
            throw new IllegalStateException("database password leaked");
        }
    }
}
