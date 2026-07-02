package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.core.Response;
import net.ximatai.muyun.spring.common.exception.AuthenticationRequiredException;
import net.ximatai.muyun.spring.common.exception.ErrorScope;
import net.ximatai.muyun.spring.common.exception.ErrorTarget;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformWebExceptionHandlerTest {
    private final PlatformWebExceptionHandler handler = new PlatformWebExceptionHandler();

    @AfterEach
    void tearDown() {
        RequestTraceContext.clear();
    }

    @Test
    void shouldReturnUnifiedEnvelopeForPlatformExceptionWithTargets() {
        try (RequestTraceContext.Scope ignored = RequestTraceContext.use("trace-1")) {
            Response response = handler.handlePlatformException(PlatformErrors.validation(
                    "DYNAMIC_FIELD_REQUIRED",
                    "客户名称不能为空",
                    ErrorTarget.field("customerName").relation("main")
            ));

            PlatformWebError error = (PlatformWebError) response.getEntity();
            assertThat(response.getStatus()).isEqualTo(422);
            assertThat(error.traceId()).isEqualTo("trace-1");
            assertThat(error.code()).isEqualTo("DYNAMIC_FIELD_REQUIRED");
            assertThat(error.status()).isEqualTo(422);
            assertThat(error.message()).isEqualTo("客户名称不能为空");
            assertThat(error.targets()).singleElement().satisfies(target -> {
                assertThat(target.kind()).isEqualTo("field");
                assertThat(target.fieldName()).isEqualTo("customerName");
                assertThat(target.relationAlias()).isEqualTo("main");
            });
        }
    }

    @Test
    void shouldReturnUnifiedEnvelopeForAuthenticationRequired() {
        Response response = handler.handleAuthenticationRequired(
                new AuthenticationRequiredException("current user context is not available")
        );

        PlatformWebError error = (PlatformWebError) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(error.traceId()).isNotBlank();
        assertThat(error.code()).isEqualTo(PlatformErrorCodes.AUTH_REQUIRED);
        assertThat(error.status()).isEqualTo(401);
        assertThat(error.message()).isEqualTo("current user context is not available");
    }

    @Test
    void shouldReturnUnifiedEnvelopeForConfigurationErrorWithScope() {
        try (RequestTraceContext.Scope ignored = RequestTraceContext.use("trace-2")) {
            Response response = handler.handlePlatformException(PlatformErrors.config(
                    "DYNAMIC_DESCRIPTOR_MISSING",
                    "模块页面配置不存在",
                    ErrorScope.module("crm.customer")
            ));

            PlatformWebError error = (PlatformWebError) response.getEntity();
            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(error.traceId()).isEqualTo("trace-2");
            assertThat(error.code()).isEqualTo("DYNAMIC_DESCRIPTOR_MISSING");
            assertThat(error.scope().moduleAlias()).isEqualTo("crm.customer");
        }
    }

    @Test
    void shouldHideUnexpectedExceptionMessage() {
        Response response = handler.handleUnexpected(new IllegalStateException("database password leaked"));

        PlatformWebError error = (PlatformWebError) response.getEntity();
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(error.code()).isEqualTo(PlatformErrorCodes.INTERNAL_ERROR);
        assertThat(error.message()).isEqualTo("Internal server error");
        assertThat(error.traceId()).isNotBlank();
    }
}
