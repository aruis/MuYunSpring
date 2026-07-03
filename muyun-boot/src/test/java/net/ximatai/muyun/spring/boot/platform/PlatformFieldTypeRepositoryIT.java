package net.ximatai.muyun.spring.boot.platform;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeDao;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(PlatformFieldTypeRepositoryIT.PostgresProfile.class)
@QuarkusTestResource(value = PostgresQuarkusTestResource.class, restrictToAnnotatedClass = true)
class PlatformFieldTypeRepositoryIT {

    @Inject
    Config config;

    @Inject
    PlatformFieldTypeDao fieldTypeDao;

    private PlatformFieldTypeService fieldTypeService() {
        fieldTypeDao.ensureTable();
        return new PlatformFieldTypeService(fieldTypeDao);
    }

    @Test
    void shouldPersistQueryOperatorsAsJsonSetThroughRepository() {
        requirePostgres();
        PlatformFieldTypeService fieldTypeService = fieldTypeService();
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
        requirePostgres();
        PlatformFieldTypeService fieldTypeService = fieldTypeService();
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

    private void requirePostgres() {
        assumeTrue(
                config.getOptionalValue("muyun.test.postgres.enabled", Boolean.class).orElse(false),
                "PostgreSQL integration test is disabled; run with -Pmuyun.postgres.it.required=true to enable it"
        );
    }

    public static class PostgresProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_platform_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.default-schema", "public");
            config.put("muyun.database.install-postgres-plugins", "true");
            config.put("muyun.platform-bootstrap.enabled", "false");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.exclude-types", String.join(",",
                    "net.ximatai.muyun.spring.boot.web.CrudWebFormSchemaTest$*",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$NoopTenantService",
                    "net.ximatai.muyun.spring.boot.dynamic.DynamicRecordWebControllerIT$TestBeans",
                    "net.ximatai.muyun.spring.boot.iam.IamWebControllerIT$TestBeans"
            ));
            config.put("quarkus.arc.remove-unused-beans", "false");
            if (Boolean.getBoolean("muyun.postgres.it.required")) {
                return config;
            }

            config.put("muyun.test.postgres.enabled", "false");
            config.put("muyun.database.repository-schema-mode", "NONE");
            return config;
        }
    }
}
