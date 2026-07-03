package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MuYunSpringWebConfigurationTest {
    @Test
    void shouldWriteCorsHeadersForAllowedOrigin() {
        MuYunSpringWebConfiguration filter = new MuYunSpringWebConfiguration(properties("http://localhost:5173"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(request.getHeaderString("Origin")).thenReturn("http://localhost:5173");
        when(request.getHeaderString("Access-Control-Request-Headers")).thenReturn("Authorization,Content-Type");
        when(response.getHeaders()).thenReturn(headers);

        filter.filter(request, response);

        assertThat(headers.getFirst(MuYunSpringWebConfiguration.ALLOW_ORIGIN)).isEqualTo("http://localhost:5173");
        assertThat(headers.getFirst(MuYunSpringWebConfiguration.ALLOW_METHODS))
                .isEqualTo("GET,POST,PUT,PATCH,DELETE,OPTIONS");
        assertThat(headers.getFirst(MuYunSpringWebConfiguration.ALLOW_HEADERS))
                .isEqualTo("Authorization,Content-Type");
        assertThat(headers.getFirst(MuYunSpringWebConfiguration.EXPOSE_HEADERS)).isEqualTo("Authorization");
        assertThat(headers.getFirst(MuYunSpringWebConfiguration.MAX_AGE)).isEqualTo("3600");
    }

    @Test
    void shouldAbortPreflightForAllowedOrigin() {
        MuYunSpringWebConfiguration filter = new MuYunSpringWebConfiguration(properties("http://127.0.0.1:5173"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        when(request.getHeaderString("Origin")).thenReturn("http://127.0.0.1:5173");
        when(request.getHeaderString("Access-Control-Request-Method")).thenReturn("POST");
        when(request.getHeaderString("Access-Control-Request-Headers")).thenReturn("Authorization");
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);

        filter.filter(request);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(request).abortWith(response.capture());
        assertThat(response.getValue().getStatus()).isEqualTo(204);
        assertThat(response.getValue().getHeaderString(MuYunSpringWebConfiguration.ALLOW_ORIGIN))
                .isEqualTo("http://127.0.0.1:5173");
        assertThat(response.getValue().getHeaderString(MuYunSpringWebConfiguration.ALLOW_HEADERS))
                .isEqualTo("Authorization");
    }

    @Test
    void shouldIgnoreDisallowedOrigin() {
        MuYunSpringWebConfiguration filter = new MuYunSpringWebConfiguration(properties("http://localhost:5173"));
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(request.getHeaderString("Origin")).thenReturn("http://example.com");
        when(response.getHeaders()).thenReturn(headers);

        filter.filter(request, response);

        assertThat(headers).isEmpty();
        verifyNoInteractions(response);
    }

    private MuYunSpringCorsProperties properties(String... allowedOrigins) {
        MuYunSpringCorsProperties properties = new MuYunSpringCorsProperties();
        properties.setAllowedOrigins(List.of(allowedOrigins));
        return properties;
    }
}
