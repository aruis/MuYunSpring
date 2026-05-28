package net.ximatai.muyun.spring.platform.publish;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModulePublisher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicSchemaService;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PlatformDynamicModulePublisherIT.TestApplication.class)
class PlatformDynamicModulePublisherIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.default-schema", () -> "public");
    }

    private final IDatabaseOperations<?> operations;
    private final DynamicSchemaService schemaService;

    @Autowired
    PlatformDynamicModulePublisherIT(IDatabaseOperations<?> operations, DynamicSchemaService schemaService) {
        this.operations = operations;
        this.schemaService = schemaService;
    }

    @Test
    void shouldPublishPlatformMetadataAndRunDynamicCrudByModuleAndMetadataAlias() {
        PlatformServices services = platformServices();
        services.moduleService.insert(module("crm.customer"));
        String metadataId = services.metadataService.insert(metadata("crm", "customer", "platform_publish_customer_it"));
        services.fieldService.insert(titleField(metadataId));
        services.fieldService.insert(field(metadataId, "code", "code", FieldType.STRING));
        services.relationService.insert(mainRelation("crm.customer", metadataId));
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(operations);
        PlatformDynamicModulePublisher publisher = new PlatformDynamicModulePublisher(
                new PlatformModuleDefinitionCompiler(
                        services.moduleService,
                        services.metadataService,
                        services.fieldService,
                        services.relationService
                ),
                new DynamicModulePublisher(schemaService, runtime)
        );

        publisher.publish("crm.customer");
        DynamicRecordService.EntityOperations customer = new DynamicRecordService(runtime).entity("crm.customer", "customer");
        DynamicRecord record = customer.newRecord()
                .setValue("title", "客户A")
                .setValue("code", "C-001");
        String id = customer.create(record);

        DynamicRecord selected = customer.select(id);

        assertThat(selected.getValue("title")).isEqualTo("客户A");
        assertThat(customer.list(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10)))
                .extracting(item -> item.getValue("title"))
                .containsExactly("客户A");
    }

    private PlatformServices platformServices() {
        TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
        TestMemoryDao<Metadata> metadataDao = new TestMemoryDao<>();
        TestMemoryDao<MetadataField> fieldDao = new TestMemoryDao<>();
        TestMemoryDao<ModuleMetadataRelation> relationDao = new TestMemoryDao<>();
        PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
        MetadataService metadataService = new MetadataService(metadataDao);
        MetadataFieldService fieldService = new MetadataFieldService(fieldDao, metadataService);
        ModuleMetadataRelationService relationService =
                new ModuleMetadataRelationService(relationDao, moduleService, metadataService);
        return new PlatformServices(moduleService, metadataService, fieldService, relationService);
    }

    private PlatformModule module(String alias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setModuleKind(ModuleKind.DYNAMIC);
        module.setTitle(alias);
        return module;
    }

    private Metadata metadata(String applicationAlias, String alias, String tableName) {
        Metadata metadata = new Metadata();
        metadata.setApplicationAlias(applicationAlias);
        metadata.setAlias(alias);
        metadata.setTitle(alias);
        metadata.setTableName(tableName);
        return metadata;
    }

    private MetadataField field(String metadataId, String fieldName, String columnName, FieldType fieldType) {
        MetadataField field = new MetadataField();
        field.setMetadataId(metadataId);
        field.setFieldName(fieldName);
        field.setColumnName(columnName);
        field.setFieldType(fieldType);
        field.setTitle(fieldName);
        return field;
    }

    private MetadataField titleField(String metadataId) {
        MetadataField field = field(metadataId, "title", "title", FieldType.STRING);
        field.setTitleField(true);
        field.setFieldLength(128);
        return field;
    }

    private ModuleMetadataRelation mainRelation(String moduleAlias, String metadataId) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setRelationRole(RelationRole.MAIN);
        relation.setTitle("main");
        return relation;
    }

    private record PlatformServices(PlatformModuleService moduleService,
                                    MetadataService metadataService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
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
        DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations) {
            return new DynamicSchemaService(operations);
        }
    }
}
