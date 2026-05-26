package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DynamicSchemaServiceIT.TestApplication.class)
class DynamicSchemaServiceIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.default-schema", () -> "public");
    }

    private final DynamicSchemaService schemaService;
    private final DataSource dataSource;

    @Autowired
    DynamicSchemaServiceIT(DynamicSchemaService schemaService, DataSource dataSource) {
        this.schemaService = schemaService;
        this.dataSource = dataSource;
    }

    @Test
    void shouldCreateDynamicTableAndKeepSecondEnsureIdempotent() throws Exception {
        EntityDefinition entity = new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("code", "code", FieldType.STRING, "Code").length(64).asRequired().asUnique(),
                        new FieldDefinition("name", "name", FieldType.STRING, "Name").length(128).asRequired(),
                        new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount").precision(18, 2),
                        new FieldDefinition("signed_at", "signed_at", FieldType.TIMESTAMP, "Signed At").asIndexed()
                )
        );

        assertThat(schemaService.ensureTable(entity)).isTrue();
        assertThat(schemaService.ensureTable(entity)).isFalse();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(columns(connection))
                    .contains("id", "version", "deleted", "created_by", "created_at", "updated_by", "updated_at",
                            "code", "name", "amount", "signed_at");
            assertThat(primaryKeys(connection)).containsExactly("id");
            assertThat(uniqueIndexes(connection)).anyMatch(indexName -> indexName.contains("code"));
        }
    }

    private List<String> columns(Connection connection) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", "app_contract", null)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<String> primaryKeys(Connection connection) throws Exception {
        try (var keys = connection.getMetaData().getPrimaryKeys(null, "public", "app_contract")) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (keys.next()) {
                names.add(keys.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<String> uniqueIndexes(Connection connection) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "public", "app_contract", true, false)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (name != null) {
                    names.add(name);
                }
            }
            return names;
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        DataSource dataSource() {
            return org.springframework.boot.jdbc.DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName(postgres.getDriverClassName())
                    .build();
        }

        @Bean
        DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations) {
            return new DynamicSchemaService(operations);
        }
    }
}
