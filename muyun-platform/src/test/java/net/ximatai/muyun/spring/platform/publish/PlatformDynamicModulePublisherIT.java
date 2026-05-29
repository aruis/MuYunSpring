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
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryKind;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategoryService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
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
import net.ximatai.muyun.spring.common.tenant.TenantContext;
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
    void shouldPublishM2PlatformConfigAndRunDynamicCrudByModuleAndMetadataAlias() {
        PlatformServices services = platformServices();
        services.applicationService.insert(application("crm"));
        services.moduleService.insert(module("crm.customer"));
        String categoryId = services.categoryService.insert(
                category("crm", "customer_status", DictionaryCategoryKind.DICTIONARY));
        services.itemService.insert(item("crm", "customer_status", "active"));
        String customerMetadataId = services.metadataService.insert(metadata("crm", "customer", "platform_publish_customer_it"));
        String contactMetadataId = services.metadataService.insert(metadata("crm", "customer_contact", "platform_publish_contact_it"));
        services.fieldService.insert(titleField(customerMetadataId));
        services.fieldService.insert(field(customerMetadataId, "code", "code", FieldType.STRING));
        services.fieldService.insert(field(customerMetadataId, "status", "status", FieldType.STRING));
        services.fieldService.insert(titleField(contactMetadataId));
        services.fieldService.insert(field(contactMetadataId, "customerId", "customer_id", FieldType.STRING));
        services.relationService.insert(mainRelation("crm.customer", customerMetadataId));
        services.relationService.insert(childRelation("crm.customer", contactMetadataId, customerMetadataId));
        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            String schemeId = services.schemeService.insert(menuScheme("main"));
            services.menuService.insert(moduleMenu(schemeId, "客户", "crm.customer"));
            assertThat(services.menuService.rootMenus(schemeId))
                    .extracting(Menu::getModuleAlias)
                    .containsExactly("crm.customer");
        }
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
        DynamicRecord contact = runtime.newRecord("crm.customer", "customer_contact")
                .setValue("title", "张三");
        DynamicRecord record = customer.newRecord()
                .setValue("title", "客户A")
                .setValue("code", "C-001")
                .setValue("status", "active")
                .setChildren("contacts", List.of(contact));
        String id = customer.create(record);

        DynamicRecord selected = customer.select(id);

        assertThat(categoryId).isNotBlank();
        assertThat(services.itemService.resolveItem("crm", "customer_status", "active").getCode()).isEqualTo("active");
        assertThat(selected.getValue("title")).isEqualTo("客户A");
        assertThat(selected.getValue("status")).isEqualTo("active");
        assertThat(selected.getChildren("contacts"))
                .hasSize(1)
                .first()
                .extracting(child -> child.getValue("title"))
                .isEqualTo("张三");
        assertThat(customer.list(Criteria.of().eq("code", "C-001"), PageRequest.of(1, 10)))
                .extracting(item -> item.getValue("title"))
                .containsExactly("客户A");
    }

    private PlatformServices platformServices() {
        TestMemoryDao<Application> applicationDao = new TestMemoryDao<>();
        TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
        TestMemoryDao<Metadata> metadataDao = new TestMemoryDao<>();
        TestMemoryDao<MetadataField> fieldDao = new TestMemoryDao<>();
        TestMemoryDao<ModuleMetadataRelation> relationDao = new TestMemoryDao<>();
        TestMemoryDao<MenuScheme> schemeDao = new TestMemoryDao<>();
        TestMemoryDao<Menu> menuDao = new TestMemoryDao<>();
        TestMemoryDao<DictionaryCategory> categoryDao = new TestMemoryDao<>();
        TestMemoryDao<DictionaryItem> itemDao = new TestMemoryDao<>();
        ApplicationService applicationService = new ApplicationService(applicationDao);
        PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
        MetadataService metadataService = new MetadataService(metadataDao);
        MetadataFieldService fieldService = new MetadataFieldService(fieldDao, metadataService);
        ModuleMetadataRelationService relationService =
                new ModuleMetadataRelationService(relationDao, moduleService, metadataService);
        MenuSchemeService schemeService = new MenuSchemeService(schemeDao);
        MenuService menuService = new MenuService(menuDao, schemeService, moduleService);
        DictionaryCategoryService categoryService = new DictionaryCategoryService(categoryDao);
        DictionaryItemService itemService = new DictionaryItemService(itemDao, categoryService);
        return new PlatformServices(applicationService, moduleService, metadataService, fieldService, relationService,
                schemeService, menuService, categoryService, itemService);
    }

    private Application application(String alias) {
        Application application = new Application();
        application.setAlias(alias);
        application.setTitle(alias);
        return application;
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

    private ModuleMetadataRelation childRelation(String moduleAlias, String metadataId, String parentMetadataId) {
        ModuleMetadataRelation relation = new ModuleMetadataRelation();
        relation.setModuleAlias(moduleAlias);
        relation.setMetadataId(metadataId);
        relation.setParentMetadataId(parentMetadataId);
        relation.setRelationRole(RelationRole.CHILD);
        relation.setRelationAlias("contacts");
        relation.setForeignKey("customerId");
        relation.setAutoPopulate(true);
        relation.setCascadeDelete(true);
        relation.setTitle("contacts");
        return relation;
    }

    private MenuScheme menuScheme(String alias) {
        MenuScheme scheme = new MenuScheme();
        scheme.setAlias(alias);
        scheme.setScopeType(MenuScopeType.TENANT);
        scheme.setTenantId("tenant-a");
        scheme.setTitle(alias);
        return scheme;
    }

    private Menu moduleMenu(String schemeId, String title, String moduleAlias) {
        Menu menu = new Menu();
        menu.setSchemeId(schemeId);
        menu.setMenuType(MenuType.MODULE);
        menu.setModuleAlias(moduleAlias);
        menu.setTitle(title);
        return menu;
    }

    private DictionaryCategory category(String applicationAlias, String alias, DictionaryCategoryKind kind) {
        DictionaryCategory category = new DictionaryCategory();
        category.setApplicationAlias(applicationAlias);
        category.setAlias(alias);
        category.setCategoryKind(kind);
        category.setTitle(alias);
        return category;
    }

    private DictionaryItem item(String applicationAlias, String categoryAlias, String code) {
        DictionaryItem item = new DictionaryItem();
        item.setApplicationAlias(applicationAlias);
        item.setCategoryAlias(categoryAlias);
        item.setCode(code);
        item.setTitle(code);
        return item;
    }

    private record PlatformServices(ApplicationService applicationService,
                                    PlatformModuleService moduleService,
                                    MetadataService metadataService,
                                    MetadataFieldService fieldService,
                                    ModuleMetadataRelationService relationService,
                                    MenuSchemeService schemeService,
                                    MenuService menuService,
                                    DictionaryCategoryService categoryService,
                                    DictionaryItemService itemService) {
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
