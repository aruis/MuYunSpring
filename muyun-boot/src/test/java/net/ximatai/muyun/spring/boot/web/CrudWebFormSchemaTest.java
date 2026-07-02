package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.Path;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.form.FormAbility;
import net.ximatai.muyun.spring.ability.form.FormControlType;
import net.ximatai.muyun.spring.ability.form.FormDescriptor;
import net.ximatai.muyun.spring.ability.form.FormField;
import net.ximatai.muyun.spring.ability.form.FormSchema;
import net.ximatai.muyun.spring.ability.form.FormValueType;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.ModuleUiViewCodes;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleDefinitionCatalog;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CrudWebFormSchemaTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeFormSchemaThroughCrudWebContract() {
        DemoRecordController controller = new DemoRecordController(new DemoRecordService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            FormSchema schema = controller.formSchema(null);

            assertThat(schema.scopeName()).isEqualTo("demo.record");
            assertThat(schema.title()).isEqualTo("Demo Record");
            assertThat(schema.fields()).singleElement().satisfies(field -> {
                assertThat(field.name()).isEqualTo("title");
                assertThat(field.title()).isEqualTo("名称");
                assertThat(field.required()).isTrue();
            });
        }
    }

    @Test
    void shouldPreferStaticModuleUiDefinitionForFormSchemaContract() {
        DemoRecordUiController controller = new DemoRecordUiController(new DemoRecordService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            FormSchema schema = controller.formSchema(null);

            assertThat(schema.scopeName()).isEqualTo("demo.record.ui");
            assertThat(schema.title()).isEqualTo("UI Demo Record");
            assertThat(schema.fields()).hasSize(3);
            assertThat(schema.fields().get(0)).satisfies(field -> {
                assertThat(field.name()).isEqualTo("title");
                assertThat(field.title()).isEqualTo("UI 名称");
                assertThat(field.required()).isTrue();
                assertThat(field.readOnly()).isTrue();
            });
            assertThat(schema.fields().get(1)).satisfies(field -> {
                assertThat(field.name()).isEqualTo("status");
                assertThat(field.controlType()).isEqualTo(FormControlType.SELECT);
                assertThat(field.optionBinding()).isEqualTo(new OptionBinding("dictionary", "demo.status"));
                assertThat(field.optionTitleField()).isEqualTo("statusTitle");
            });
            assertThat(schema.fields().get(2)).satisfies(field -> {
                assertThat(field.name()).isEqualTo("enabled");
                assertThat(field.valueType()).isEqualTo(FormValueType.BOOLEAN);
                assertThat(field.controlType()).isEqualTo(FormControlType.SWITCH);
            });
        }
    }

    @Test
    void shouldIgnoreParentModuleUiContributionForFormSchemaContract() {
        DemoRecordChildUiController controller = new DemoRecordChildUiController(new DemoRecordService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            FormSchema schema = controller.formSchema(null);

            assertThat(schema.scopeName()).isEqualTo("demo.record");
            assertThat(schema.title()).isEqualTo("Demo Record");
            assertThat(schema.fields()).singleElement().satisfies(field -> {
                assertThat(field.name()).isEqualTo("title");
                assertThat(field.title()).isEqualTo("名称");
            });
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldProjectStaticModuleQueryThroughCrudWebContract() {
        DemoRecordUiController controller = new DemoRecordUiController(new DemoRecordService());
        controller.setStaticRecordReadProjectionService(new StaticRecordReadProjectionService(
                new StaticModuleDefinitionCatalog(List.of(demoStaticModuleDefinition()))
        ));

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            WebPageResponse<?> response = controller.query(null);

            Map<String, Object> projected = (Map<String, Object>) response.records().getFirst();
            assertThat(projected).containsEntry("id", "demo-1");
            assertThat(projected).containsEntry("title", "Demo One");
            assertThat(projected).doesNotContainKey("status");
        }
    }

    @Path("/demo.record")
    private static final class DemoRecordController extends WebSupport<DemoRecordService>
            implements CrudWeb<DemoRecord, DemoRecordService> {
        private DemoRecordController(DemoRecordService service) {
            this.service = service;
        }
    }

    @Path("/demo.record.ui")
    private static final class DemoRecordUiController extends WebSupport<DemoRecordService>
            implements CrudWeb<DemoRecord, DemoRecordService>, StaticModuleUiContributor {
        private StaticRecordReadProjectionService staticRecordReadProjectionService;

        private DemoRecordUiController(DemoRecordService service) {
            this.service = service;
        }

        private void setStaticRecordReadProjectionService(StaticRecordReadProjectionService staticRecordReadProjectionService) {
            this.staticRecordReadProjectionService = staticRecordReadProjectionService;
        }

        @Override
        public StaticRecordReadProjectionService staticRecordReadProjectionService() {
            return staticRecordReadProjectionService;
        }

        @Override
        public ModuleUiDefinition moduleUiDefinition() {
            return ModuleUiDefinition.builder("demo.record.ui")
                    .listView(list -> list
                            .field("title", field -> field.label("UI 名称")))
                    .formView(form -> form
                            .title("UI Demo Record")
                            .field("title", field -> field.label("UI 名称").required().readOnly())
                            .field("status", field -> field.label("状态"))
                            .field("enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                    .build();
        }
    }

    @Path("/demo.record.child")
    private static final class DemoRecordChildUiController extends WebSupport<DemoRecordService>
            implements CrudWeb<DemoRecord, DemoRecordService>, StaticModuleUiContributor {
        private DemoRecordChildUiController(DemoRecordService service) {
            this.service = service;
        }

        @Override
        public ModuleUiDefinition moduleUiDefinition() {
            return ModuleUiDefinition.builder("demo.parent")
                    .formView(ModuleUiViewCodes.childResourceDefaultForm("demo_record"), form -> form
                            .title("Child UI Demo Record")
                            .field("demo_record", "title", field -> field.label("子资源名称").required()))
                    .build();
        }
    }

    private static final class DemoRecordService extends AbstractAbilityService<DemoRecord>
            implements FormAbility<DemoRecord> {
        private DemoRecordService() {
            super("demo.record", DemoRecord.class, dao());
        }

        @Override
        public FormDescriptor formDescriptor() {
            return FormDescriptor.builder("demo.record")
                    .title("Demo Record")
                    .field(FormField.of("title").withTitle("名称").asRequired())
                    .build();
        }

        @Override
        public PageResult<DemoRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
            DemoRecord record = new DemoRecord();
            record.setId("demo-1");
            record.setTitle("Demo One");
            record.setStatus("draft");
            return PageResult.of(List.of(record), 1, pageRequest);
        }
    }

    private static StaticModuleDefinition demoStaticModuleDefinition() {
        return new StaticModuleDefinition(
                "demo",
                "demo.record.ui",
                "UI Demo Record",
                null,
                ModuleEntryType.ROUTE,
                "/demo-records",
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(new EntityDefinition(
                        "demo_record",
                        "demo_record",
                        "Demo Record",
                        List.of(
                                FieldDefinition.string("title", "名称"),
                                FieldDefinition.string("status", "状态")
                        )
                )),
                ModuleUiDefinition.builder("demo.record.ui")
                        .listView(list -> list
                                .field("title", field -> field.label("UI 名称")))
                        .build()
        );
    }

    @Table(name = "demo_record", comment = "Demo Record")
    public static final class DemoRecord extends StandardEntity {
        @Column(name = "title", comment = "名称")
        private String title;

        @OptionField(type = OptionSourceType.DICTIONARY, source = "demo.status")
        @Column(name = "status", comment = "状态")
        private String status;

        private String statusTitle;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusTitle() {
            return statusTitle;
        }

        public void setStatusTitle(String statusTitle) {
            this.statusTitle = statusTitle;
        }
    }

    @SuppressWarnings("unchecked")
    private static BaseDao<DemoRecord, String> dao() {
        return mock(BaseDao.class);
    }
}
