package net.ximatai.muyun.spring.boot.iam;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.platform.PostgresQuarkusTestResource;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(OrganizationRepositoryIT.PostgresProfile.class)
@QuarkusTestResource(value = PostgresQuarkusTestResource.class, restrictToAnnotatedClass = true)
class OrganizationRepositoryIT {

    @Inject
    Config config;

    @Inject
    OrganizationDao organizationDao;

    @Inject
    AgroalDataSource dataSource;

    @Test
    void quarkusRepositoryShouldEnsureTableAndRunOrganizationSmokePath() throws Exception {
        requirePostgres();
        OrganizationService organizationService = organizationService();

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).contains("PostgreSQL");
            assertThat(organizationColumns(connection))
                    .contains("id", "tenant_id", "parent_id", "code", "title", "sort_order", "enabled", "deleted", "version");
            assertThat(organizationUniqueIndexColumns(connection)).contains(List.of("tenant_id", "code"));
        }
        assertThat(organizationDao.ensureTable()).isFalse();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Organization root = new Organization();
        root.setCode("HQ-" + suffix);
        root.setTitle("Headquarters");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_org_it_" + suffix)) {
            String rootId = organizationService.insert(root);

            Organization branch = new Organization();
            branch.setCode("BR-" + suffix);
            branch.setTitle("Branch 001");
            branch.setParentId(rootId);
            organizationService.insert(branch);

            assertThat(organizationService.select(rootId))
                    .extracting(Organization::getTitle, Organization::getParentId, Organization::getEnabled)
                    .containsExactly("Headquarters", TreeAbility.ROOT_ID, Boolean.TRUE);
            assertThat(organizationService.children(rootId))
                    .extracting(Organization::getCode)
                    .containsExactly("BR-" + suffix);
            assertThat(organizationService.pageQuery(Criteria.of().eq("parentId", TreeAbility.ROOT_ID), PageRequest.of(1, 10)).getRecords())
                    .extracting(Organization::getCode)
                    .containsExactly("HQ-" + suffix);

            assertThat(organizationService.delete(rootId)).isEqualTo(1);
            assertThat(organizationService.select(rootId)).isNull();
            assertThat(organizationService.count(Criteria.of())).isEqualTo(1);
        }
    }

    private OrganizationService organizationService() {
        organizationDao.ensureTable();
        return new OrganizationService(organizationDao, tenantId -> {
        });
    }

    private List<String> organizationColumns(Connection connection) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", "iam_organization", null)) {
            ArrayList<String> names = new ArrayList<>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<List<String>> organizationUniqueIndexColumns(Connection connection) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "public", "iam_organization", true, false)) {
            Map<String, List<String>> columnsByIndex = new LinkedHashMap<>();
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                String column = indexes.getString("COLUMN_NAME");
                if (name != null && column != null) {
                    columnsByIndex.computeIfAbsent(name, ignored -> new ArrayList<>()).add(column);
                }
            }
            return new ArrayList<>(columnsByIndex.values());
        }
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
            config.put("quarkus.arc.exclude-types", "net.ximatai.muyun.spring.boot.web.CrudWebFormSchemaTest$*");
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
