package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = MigrationIT.TestApplication.class)
class MigrationIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.default-schema", () -> "public");
    }

    @Autowired
    TestPersonService personService;

    @Autowired
    MigrationRecordService migrationRecordService;

    @Autowired
    MigrationExecutor migrationExecutor;

    @Autowired
    TestMigration testMigration;

    @Test
    void shouldRunMigrationOnStartupAndTransformData() {
        // Startup ran TestSeedRunner (ages 18/20/22), then TestMigration: step 1 bumps ages by 10,
        // step 2 inserts Dave. Both the data change and the recorded version are verified.
        assertThat(personService.select("1").getAge()).isEqualTo(28);
        assertThat(personService.select("2").getAge()).isEqualTo(30);
        assertThat(personService.select("3").getAge()).isEqualTo(32);

        TestPerson dave = personService.select("4");
        assertThat(dave).isNotNull();
        assertThat(dave.getName()).isEqualTo("Dave");
        assertThat(dave.getAge()).isEqualTo(30);

        MigrationRecord record = migrationRecordService.findByAlias("test-person");
        assertThat(record).isNotNull();
        assertThat(record.getAppliedVersion()).isEqualTo(2);
    }

    @Test
    void shouldNotReapplyCompletedMigration() {
        int before = migrationRecordService.findByAlias("test-person").getAppliedVersion();

        migrationExecutor.run(testMigration);

        int after = migrationRecordService.findByAlias("test-person").getAppliedVersion();
        assertThat(after).isEqualTo(before);
        assertThat(personService.count(Criteria.of().eq("name", "Dave"))).isEqualTo(1);
    }

    @Test
    void shouldRejectNonPositiveVersion() {
        assertThatThrownBy(() -> new MigrateStep(0, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MigrateStep(-5, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MigrateStep(null, () -> { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectDuplicateVersion() {
        AbstractMigration duplicate = new AbstractMigration() {
            @Override
            public String getAlias() {
                return "duplicate-test";
            }

            @Override
            public List<MigrateStep> getMigrateSteps() {
                return List.of(
                        new MigrateStep(1, () -> { }),
                        new MigrateStep(1, () -> { })
                );
            }
        };

        assertThatThrownBy(() -> migrationExecutor.run(duplicate))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("duplicate");
        assertThat(migrationRecordService.findByAlias("duplicate-test")).isNull();
    }

    @Test
    void shouldApplyStepsInVersionOrderRegardlessOfDeclaration() {
        List<Integer> executed = new ArrayList<>();
        AbstractMigration unordered = new AbstractMigration() {
            @Override
            public String getAlias() {
                return "order-test";
            }

            @Override
            public List<MigrateStep> getMigrateSteps() {
                return List.of(
                        new MigrateStep(3, () -> executed.add(3)),
                        new MigrateStep(1, () -> executed.add(1)),
                        new MigrateStep(2, () -> executed.add(2))
                );
            }
        };

        migrationExecutor.run(unordered);

        assertThat(executed).containsExactly(1, 2, 3);
        assertThat(migrationRecordService.findByAlias("order-test").getAppliedVersion()).isEqualTo(3);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EnableMuYunRepositories(basePackages = "net.ximatai.muyun.spring.migration")
    @ComponentScan(basePackages = "net.ximatai.muyun.spring.migration")
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
    }
}
