package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.ability.TreeAbility;
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
@SpringBootTest(classes = OrganizationRepositoryIT.TestApplication.class)
class OrganizationRepositoryIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    private final OrganizationService organizationService;
    private final OrganizationDao organizationDao;
    private final DataSource dataSource;

    @Autowired
    OrganizationRepositoryIT(OrganizationService organizationService,
                             OrganizationDao organizationDao,
                             DataSource dataSource) {
        this.organizationService = organizationService;
        this.organizationDao = organizationDao;
        this.dataSource = dataSource;
    }

    @Test
    void springRepositoryShouldEnsureTableAndRunCrudThroughRealDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).contains("PostgreSQL");
            assertThat(organizationColumns(connection))
                    .contains("id", "parent_id", "code", "title", "sort_order", "enabled", "deleted", "version");
            assertThat(organizationUniqueIndexes(connection)).anyMatch(indexName -> indexName.contains("code"));
        }
        assertThat(organizationDao.ensureTable()).isFalse();

        Organization root = new Organization();
        root.setCode("HQ");
        root.setTitle("Headquarters");
        String rootId = organizationService.insert(root);

        Organization branch = new Organization();
        branch.setCode("BR-001");
        branch.setTitle("Branch 001");
        branch.setParentId(rootId);
        organizationService.insert(branch);

        assertThat(organizationService.select(rootId))
                .extracting(Organization::getTitle, Organization::getParentId, Organization::getEnabled)
                .containsExactly("Headquarters", TreeAbility.ROOT_ID, Boolean.TRUE);
        assertThat(organizationService.children(rootId))
                .extracting(Organization::getCode)
                .containsExactly("BR-001");
        assertThat(organizationService.pageQuery(Criteria.of().eq("parentId", TreeAbility.ROOT_ID), PageRequest.of(1, 10)).getRecords())
                .extracting(Organization::getCode)
                .containsExactly("HQ");

        assertThat(organizationService.delete(rootId)).isEqualTo(1);
        assertThat(organizationService.select(rootId)).isNull();
        assertThat(organizationService.count(Criteria.of())).isEqualTo(1);
    }

    private List<String> organizationColumns(Connection connection) throws Exception {
        try (var columns = connection.getMetaData().getColumns(null, "public", "iam_organization", null)) {
            java.util.ArrayList<String> names = new java.util.ArrayList<>();
            while (columns.next()) {
                names.add(columns.getString("COLUMN_NAME"));
            }
            return names;
        }
    }

    private List<String> organizationUniqueIndexes(Connection connection) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "public", "iam_organization", true, false)) {
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
    @EnableMuYunRepositories(basePackageClasses = OrganizationDao.class)
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
        OrganizationService organizationService(OrganizationDao organizationDao) {
            return new OrganizationService(organizationDao);
        }
    }
}
