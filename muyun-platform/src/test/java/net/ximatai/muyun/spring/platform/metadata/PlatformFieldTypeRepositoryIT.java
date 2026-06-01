package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PlatformFieldTypeRepositoryIT.TestApplication.class)
class PlatformFieldTypeRepositoryIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    private final PlatformFieldTypeService fieldTypeService;

    @Autowired
    PlatformFieldTypeRepositoryIT(PlatformFieldTypeService fieldTypeService) {
        this.fieldTypeService = fieldTypeService;
    }

    @Test
    void shouldPersistQueryOperatorsAsJsonSetThroughRepository() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        PlatformFieldType fieldType = new PlatformFieldType();
        fieldType.setAlias("string_" + suffix);
        fieldType.setTitle("String " + suffix);
        fieldType.setFieldType(FieldType.STRING);
        fieldType.setDefaultLength(128);
        fieldType.setDefaultQueryOperator(DynamicQueryOperator.LIKE);
        fieldType.setQueryOperators(Set.of(" LIKE ", "EQ"));

        String id = fieldTypeService.insert(fieldType);

        PlatformFieldType selected = fieldTypeService.select(id);
        assertThat(selected.getQueryOperators()).containsExactly("EQ", "LIKE");
        assertThat(selected.queryDefinition().operators()).containsExactlyInAnyOrder(DynamicQueryOperator.EQ, DynamicQueryOperator.LIKE);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMuYunRepositories(basePackageClasses = PlatformFieldTypeDao.class)
    static class TestApplication {
        @Bean
        DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName(postgres.getDriverClassName())
                    .build();
        }

        @Bean
        PlatformFieldTypeService fieldTypeService(PlatformFieldTypeDao fieldTypeDao) {
            return new PlatformFieldTypeService(fieldTypeDao);
        }
    }
}
