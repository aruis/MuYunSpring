package net.ximatai.muyun.spring.common.openapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApi31ProjectorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldProjectAnOpenApi31DocumentAcceptedByTheStandardParser() throws Exception {
        PlatformApiDocument document = new TestDocument();

        Map<String, Object> projected = OpenApi31Projector.project(document);
        var parsed = new OpenAPIV3Parser().readContents(objectMapper.writeValueAsString(projected), null,
                new ParseOptions());

        assertThat(parsed.getMessages()).isEmpty();
        assertThat(parsed.getOpenAPI()).isNotNull();
        assertThat(parsed.getOpenAPI().getOpenapi()).isEqualTo(OpenApi31Projector.VERSION);
        assertThat(parsed.getOpenAPI().getPaths()).containsKey("/crm.customer/view/{id}");
        assertThat(parsed.getOpenAPI().getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(projected).containsEntry("x-muyun-module-alias", "crm.customer");
        assertThat(projected).containsEntry("x-muyun-module-base-path", "/crm.customer");
    }

    private static final class TestDocument implements PlatformApiDocument {
        @Override
        public String moduleAlias() {
            return "crm.customer";
        }

        @Override
        public String title() {
            return "客户";
        }

        @Override
        public String basePath() {
            return "/crm.customer";
        }

        @Override
        public List<Operation> operations() {
            return List.of(new Operation("GET", "/crm.customer/view/{id}", "crm_customer_view",
                    "View customer", null, "Customer", "view", "crm.customer:view",
                    List.of("RESOURCE_NOT_FOUND")));
        }

        @Override
        public Map<String, Schema> schemas() {
            return Map.of("Customer", new Schema("Customer", "object", null, List.of("name"),
                    Map.of("name", new Property("string", null, true, false, false,
                            null, null, null, null, null, null, List.of())), null));
        }

        @Override
        public Map<String, ErrorResponse> errors() {
            return Map.of("RESOURCE_NOT_FOUND", new ErrorResponse("RESOURCE_NOT_FOUND", 404,
                    "PlatformWebError"));
        }
    }
}
