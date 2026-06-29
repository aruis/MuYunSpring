package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.database.core.orm.Criteria;
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
import java.util.List;
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

    @Test
    void shouldQueryJsonSetFieldWithCollectionCriteria() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        PlatformFieldType stringType = fieldType("string_" + suffix, FieldType.STRING,
                Set.of("LIKE", "EQ"), Set.of("input", "select"));
        PlatformFieldType dateType = fieldType("date_" + suffix, FieldType.DATE,
                Set.of("BETWEEN", "EQ"), Set.of("date"));
        PlatformFieldType emptyType = fieldType("empty_" + suffix, FieldType.TEXT,
                Set.of(), Set.of());
        emptyType.setDefaultQueryOperator(null);
        fieldTypeService.insert(stringType);
        fieldTypeService.insert(dateType);
        fieldTypeService.insert(emptyType);
        List<String> aliases = List.of(stringType.getAlias(), dateType.getAlias(), emptyType.getAlias());

        assertThat(fieldTypeService.list(Criteria.of()
                        .in("alias", aliases)
                        .contains("queryOperators", "LIKE")))
                .extracting(PlatformFieldType::getAlias)
                .containsExactly(stringType.getAlias());
        assertThat(fieldTypeService.list(Criteria.of()
                        .in("alias", aliases)
                        .containsAny("queryOperators", List.of("LIKE", "BETWEEN"))))
                .extracting(PlatformFieldType::getAlias)
                .containsExactlyInAnyOrder(stringType.getAlias(), dateType.getAlias());
        assertThat(fieldTypeService.list(Criteria.of()
                        .in("alias", aliases)
                        .containsAll("uiTypeAliases", List.of("input", "select"))))
                .extracting(PlatformFieldType::getAlias)
                .containsExactly(stringType.getAlias());
        assertThat(fieldTypeService.list(Criteria.of()
                        .in("alias", aliases)
                        .isEmpty("uiTypeAliases")))
                .extracting(PlatformFieldType::getAlias)
                .containsExactly(emptyType.getAlias());
        assertThat(fieldTypeService.list(Criteria.of()
                        .in("alias", aliases)
                        .isNotEmpty("uiTypeAliases")))
                .extracting(PlatformFieldType::getAlias)
                .containsExactlyInAnyOrder(stringType.getAlias(), dateType.getAlias());
    }

    private PlatformFieldType fieldType(String alias,
                                        FieldType fieldType,
                                        Set<String> queryOperators,
                                        Set<String> uiTypeAliases) {
        PlatformFieldType type = new PlatformFieldType();
        type.setAlias(alias);
        type.setTitle(alias);
        type.setFieldType(fieldType);
        type.setDefaultQueryOperator(DynamicQueryOperator.defaultOperator(fieldType));
        type.setQueryOperators(queryOperators);
        type.setUiTypeAliases(uiTypeAliases);
        return type;
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
