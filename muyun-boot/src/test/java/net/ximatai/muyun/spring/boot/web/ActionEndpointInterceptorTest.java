package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionAuthorizationResult;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
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
        ActionExecutionContextHolder.clear();
    }

    @Test
    void shouldAuthorizeResolvedActionContextAndExposeItDuringRequest() {
        TestRequestContext request = new TestRequestContext();
        ActionExecutionContext context = ActionExecutionContext.ofPlatformAction(
                "iam.organization",
                PlatformAction.QUERY,
                Set.of(),
                Optional.of(CurrentUser.tenantUser("u1", "User", "t1"))
        );
        request.setProperty(ActionEndpointInterceptor.ACTION_CONTEXT_PROPERTY, context);

        interceptor.filter(request);

        ActionExecutionContext authorized = (ActionExecutionContext) request.getProperty(
                ActionEndpointInterceptor.ACTION_CONTEXT_PROPERTY);
        assertThat(policyService.context).isSameAs(context);
        assertThat(authorized.authorizationResult()).isNotNull();
        assertThat(authorized.authorizationResult().operatorId()).isEqualTo("u1");
        assertThat(ActionExecutionContextHolder.current()).contains(authorized);
    }

    @Test
    void shouldClearActionContextWhenResponseFilterRuns() {
        TestRequestContext request = new TestRequestContext();
        ActionExecutionContext context = ActionExecutionContext.ofPlatformAction(
                "iam.organization",
                PlatformAction.UPDATE,
                Set.of("org-1"),
                Optional.empty()
        );
        request.setProperty(ActionEndpointInterceptor.ACTION_CONTEXT_PROPERTY, context);
        interceptor.filter(request);
        assertThat(ActionExecutionContextHolder.current()).isPresent();

        interceptor.filter(request, mock(ContainerResponseContext.class));

        assertThat(ActionExecutionContextHolder.current()).isEmpty();
    }

    @Test
    void shouldIgnoreRequestWithoutResolvedActionContext() {
        TestRequestContext request = new TestRequestContext();

        interceptor.filter(request);

        assertThat(policyService.context).isNull();
        assertThat(ActionExecutionContextHolder.current()).isEmpty();
    }

    @Test
    void shouldResolveActionContextFromJaxRsResourceInfo() throws NoSuchMethodException {
        ResourceInfo resourceInfo = mock(ResourceInfo.class);
        doReturn(AnnotatedActionResource.class).when(resourceInfo).getResourceClass();
        when(resourceInfo.getResourceMethod()).thenReturn(AnnotatedActionResource.class.getMethod("query"));
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        TestRequestContext request = new TestRequestContext();
        request.uriInfo = uriInfo;
        interceptor.resourceInfo = resourceInfo;

        interceptor.filter(request);

        ActionExecutionContext authorized = (ActionExecutionContext) request.getProperty(
                ActionEndpointInterceptor.ACTION_CONTEXT_PROPERTY);
        assertThat(policyService.context.moduleAlias()).isEqualTo("iam.organization");
        assertThat(policyService.context.actionCode()).isEqualTo("query");
        assertThat(authorized.authorizationResult()).isNotNull();
        assertThat(ActionExecutionContextHolder.current()).contains(authorized);
    }

    @Test
    void shouldClearActionContextWhenAuthorizationFails() {
        ActionEndpointInterceptor interceptor = new ActionEndpointInterceptor(
                new ThrowingPolicyService(),
                new ActionEndpointContextResolver()
        );
        TestRequestContext request = new TestRequestContext();
        request.setProperty(ActionEndpointInterceptor.ACTION_CONTEXT_PROPERTY,
                ActionExecutionContext.ofPlatformAction("iam.organization", PlatformAction.QUERY, Set.of(),
                        Optional.empty()));

        assertThatThrownBy(() -> interceptor.filter(request))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("denied");
        assertThat(ActionExecutionContextHolder.current()).isEmpty();
    }

    private static final class RecordingPolicyService implements ActionExecutionPolicyService {
        private ActionExecutionContext context;

        @Override
        public void requireAuthorized(ActionExecutionContext context) {
            this.context = context;
        }

        @Override
        public ActionAuthorizationResult authorize(ActionExecutionContext context) {
            this.context = context;
            return ActionAuthorizationResult.allowed(context, "TEST_ALLOWED");
        }
    }

    private static final class ThrowingPolicyService implements ActionExecutionPolicyService {
        @Override
        public void requireAuthorized(ActionExecutionContext context) {
            throw new PlatformException("denied");
        }

        @Override
        public ActionAuthorizationResult authorize(ActionExecutionContext context) {
            throw new PlatformException("denied");
        }
    }

    @PlatformStaticModule(application = "iam", alias = "iam.organization", title = "组织", route = "/iam/organizations")
    public static final class AnnotatedActionResource {
        @ActionEndpoint(PlatformAction.QUERY)
        public void query() {
        }
    }

    private static final class TestRequestContext implements ContainerRequestContext {
        private final Map<String, Object> properties = new HashMap<>();
        private UriInfo uriInfo;

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public java.util.Collection<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public void setProperty(String name, Object object) {
            properties.put(name, object);
        }

        @Override
        public void removeProperty(String name) {
            properties.remove(name);
        }

        @Override
        public jakarta.ws.rs.core.UriInfo getUriInfo() {
            return uriInfo;
        }

        @Override
        public void setRequestUri(java.net.URI requestUri) {
            throw unsupported();
        }

        @Override
        public void setRequestUri(java.net.URI baseUri, java.net.URI requestUri) {
            throw unsupported();
        }

        @Override
        public jakarta.ws.rs.core.Request getRequest() {
            throw unsupported();
        }

        @Override
        public String getMethod() {
            throw unsupported();
        }

        @Override
        public void setMethod(String method) {
            throw unsupported();
        }

        @Override
        public jakarta.ws.rs.core.MultivaluedMap<String, String> getHeaders() {
            throw unsupported();
        }

        @Override
        public String getHeaderString(String name) {
            throw unsupported();
        }

        @Override
        public java.util.Date getDate() {
            throw unsupported();
        }

        @Override
        public java.util.Locale getLanguage() {
            throw unsupported();
        }

        @Override
        public int getLength() {
            throw unsupported();
        }

        @Override
        public jakarta.ws.rs.core.MediaType getMediaType() {
            throw unsupported();
        }

        @Override
        public java.util.List<jakarta.ws.rs.core.MediaType> getAcceptableMediaTypes() {
            throw unsupported();
        }

        @Override
        public java.util.List<java.util.Locale> getAcceptableLanguages() {
            throw unsupported();
        }

        @Override
        public java.util.Map<String, jakarta.ws.rs.core.Cookie> getCookies() {
            throw unsupported();
        }

        @Override
        public boolean hasEntity() {
            throw unsupported();
        }

        @Override
        public java.io.InputStream getEntityStream() {
            throw unsupported();
        }

        @Override
        public void setEntityStream(java.io.InputStream input) {
            throw unsupported();
        }

        @Override
        public jakarta.ws.rs.core.SecurityContext getSecurityContext() {
            throw unsupported();
        }

        @Override
        public void setSecurityContext(jakarta.ws.rs.core.SecurityContext context) {
            throw unsupported();
        }

        @Override
        public void abortWith(jakarta.ws.rs.core.Response response) {
            throw unsupported();
        }

        private UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("not used by this contract test");
        }
    }
}
