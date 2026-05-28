package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.EntityMetaResolver;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.SimpleEntityManager;
import net.ximatai.muyun.spring.ability.OptimisticLockException;
import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.schema.PlatformEntityManagers;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private final CachedOrganizationService cachedOrganizationService;
    private final OrganizationDao organizationDao;
    private final DataSource dataSource;
    private final OrganizationTransactionProbe transactionProbe;

    @Autowired
    OrganizationRepositoryIT(OrganizationService organizationService,
                             CachedOrganizationService cachedOrganizationService,
                             OrganizationDao organizationDao,
                             DataSource dataSource,
                             OrganizationTransactionProbe transactionProbe) {
        this.organizationService = organizationService;
        this.cachedOrganizationService = cachedOrganizationService;
        this.organizationDao = organizationDao;
        this.dataSource = dataSource;
        this.transactionProbe = transactionProbe;
    }

    @Test
    void springRepositoryShouldEnsureTableAndRunCrudThroughRealDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).contains("PostgreSQL");
            assertThat(organizationColumns(connection))
                    .contains("id", "parent_id", "code", "title", "sort_order", "enabled", "deleted", "version");
            assertThat(organizationUniqueIndexColumns(connection)).contains(List.of("tenant_id", "code"));
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

    @Test
    void springRepositoryShouldEnforceOptimisticLockOnRealDatabase() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Organization organization = new Organization();
        organization.setCode("CAS-" + suffix);
        organization.setTitle("CAS Organization");
        String id = organizationService.insert(organization);

        organization.setTitle("CAS Updated");
        organizationService.update(organization);
        assertThat(organizationService.select(id).getVersion()).isEqualTo(1);

        Organization staleUpdate = new Organization();
        staleUpdate.setId(id);
        staleUpdate.setCode("CAS-" + suffix);
        staleUpdate.setTitle("Stale Update");
        staleUpdate.setVersion(0);

        assertThatThrownBy(() -> organizationService.update(staleUpdate))
                .isInstanceOf(OptimisticLockException.class);
        assertThat(organizationService.select(id).getTitle()).isEqualTo("CAS Updated");

        Organization staleDelete = new Organization();
        staleDelete.setId(id);
        staleDelete.setVersion(0);

        assertThatThrownBy(() -> organizationService.delete(staleDelete))
                .isInstanceOf(OptimisticLockException.class);
        assertThat(organizationService.select(id)).isNotNull();

        assertThat(organizationService.delete(id)).isEqualTo(1);
        assertThat(organizationService.select(id)).isNull();
    }

    @Test
    void springRepositoryShouldEnforceOptimisticLockForHardDeleteOnRealDatabase() {
        HardDeleteOrganizationService hardDeleteService = new HardDeleteOrganizationService(organizationDao);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Organization organization = new Organization();
        organization.setCode("HARD-CAS-" + suffix);
        organization.setTitle("Hard CAS Organization");
        String id = hardDeleteService.insert(organization);

        organization.setTitle("Hard CAS Updated");
        hardDeleteService.update(organization);

        Organization staleDelete = new Organization();
        staleDelete.setId(id);
        staleDelete.setVersion(0);

        assertThatThrownBy(() -> hardDeleteService.delete(staleDelete))
                .isInstanceOf(OptimisticLockException.class);
        assertThat(hardDeleteService.select(id)).isNotNull();

        assertThat(hardDeleteService.delete(id)).isEqualTo(1);
        assertThat(organizationDao.findById(id)).isNull();
    }

    @Test
    void springRepositoryShouldKeepStaticCacheCleanWhenTransactionRollsBack() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String tenantId = "tenant-static-tx-" + suffix;
        Organization organization = new Organization();
        organization.setCode("TX-CACHE-" + suffix);
        organization.setTitle("Before transaction");

        try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
            String id = cachedOrganizationService.insert(organization);
            cachedOrganizationService.select(id);
            cachedOrganizationService.selectAllWithCache();

            assertThatThrownBy(() -> transactionProbe.updateOrganizationThenFail(cachedOrganizationService, id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("rollback static cache");

            assertThat(cachedOrganizationService.select(id).getTitle()).isEqualTo("Before transaction");
            assertThat(cachedOrganizationService.selectAllWithCache())
                    .extracting(Organization::getTitle)
                    .contains("Before transaction")
                    .doesNotContain("Inside transaction");
        }
    }

    @Test
    void springRepositoryShouldInvalidateStaticCacheAfterTransactionCommits() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String tenantId = "tenant-static-commit-" + suffix;
        Organization organization = new Organization();
        organization.setCode("TX-COMMIT-" + suffix);
        organization.setTitle("Before commit");

        try (TenantContext.Scope ignored = TenantContext.use(tenantId)) {
            String id = cachedOrganizationService.insert(organization);
            cachedOrganizationService.select(id);
            cachedOrganizationService.selectAllWithCache();

            transactionProbe.updateOrganizationAndCommit(cachedOrganizationService, id);

            assertThat(cachedOrganizationService.select(id).getTitle()).isEqualTo("After commit");
            assertThat(cachedOrganizationService.selectAllWithCache())
                    .extracting(Organization::getTitle)
                    .contains("After commit")
                    .doesNotContain("Before commit");
        }
    }

    @Test
    void springRepositoryVersionMethodsShouldRequireExpectedVersion() {
        Organization organization = new Organization();
        organization.setId("org-null-version");

        assertThatThrownBy(() -> organizationDao.updateByIdAndVersion(organization, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion");
        assertThatThrownBy(() -> organizationDao.deleteByIdAndVersion("org-null-version", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion");
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
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
        CachedOrganizationService organizationService(OrganizationDao organizationDao) {
            return new CachedOrganizationService(organizationDao);
        }

        @Bean
        OrganizationTransactionProbe organizationTransactionProbe() {
            return new OrganizationTransactionProbe();
        }

        @Bean
        EntityMetaResolver entityMetaResolver() {
            return PlatformEntityManagers.entityMetaResolver();
        }

        @Bean
        SimpleEntityManager simpleEntityManager(IDatabaseOperations<?> operations, EntityMetaResolver entityMetaResolver) {
            return PlatformEntityManagers.simpleEntityManager(operations, entityMetaResolver);
        }
    }

    private static final class CachedOrganizationService extends OrganizationService implements CacheAbility<Organization> {
        private CachedOrganizationService(OrganizationDao organizationDao) {
            super(organizationDao);
        }
    }

    static class OrganizationTransactionProbe {

        @Transactional
        public void updateOrganizationThenFail(CachedOrganizationService service, String id) {
            Organization update = service.select(id);
            update.setTitle("Inside transaction");
            service.update(update);

            assertThat(service.select(id).getTitle()).isEqualTo("Inside transaction");
            assertThat(service.selectAllWithCache())
                    .extracting(Organization::getTitle)
                    .contains("Inside transaction");
            throw new RuntimeException("rollback static cache");
        }

        @Transactional
        public void updateOrganizationAndCommit(CachedOrganizationService service, String id) {
            Organization update = service.select(id);
            update.setTitle("After commit");
            service.update(update);

            assertThat(service.select(id).getTitle()).isEqualTo("After commit");
            assertThat(service.selectAllWithCache())
                    .extracting(Organization::getTitle)
                    .contains("After commit");
        }
    }

    private static final class HardDeleteOrganizationService implements CrudAbility<Organization> {
        private final OrganizationDao organizationDao;

        private HardDeleteOrganizationService(OrganizationDao organizationDao) {
            this.organizationDao = organizationDao;
        }

        @Override
        public BaseDao<Organization, String> getDao() {
            return organizationDao;
        }

        @Override
        public String getModuleAlias() {
            return "iam.organization.hardDelete";
        }
    }
}
