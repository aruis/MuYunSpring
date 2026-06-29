package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.form.FormAbility;
import net.ximatai.muyun.spring.ability.form.FormDescriptor;
import net.ximatai.muyun.spring.ability.form.FormField;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrudWebFormSchemaTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeFormSchemaThroughCrudWebEndpoint() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new DemoRecordController(new DemoRecordService()))
                .build();

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            mvc.perform(get("/demo.record/form/schema"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scopeName").value("demo.record"))
                    .andExpect(jsonPath("$.title").value("Demo Record"))
                    .andExpect(jsonPath("$.fields[0].name").value("title"))
                    .andExpect(jsonPath("$.fields[0].title").value("名称"))
                    .andExpect(jsonPath("$.fields[0].required").value(true));
        }
    }

    @RestController
    @RequestMapping("/demo.record")
    private static final class DemoRecordController extends WebSupport<DemoRecordService>
            implements CrudWeb<DemoRecord, DemoRecordService> {
        private DemoRecordController(DemoRecordService service) {
            this.service = service;
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
            return PageResult.of(List.of(), 0, pageRequest);
        }
    }

    private static final class DemoRecord extends StandardEntity {
        private String title;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @SuppressWarnings("unchecked")
    private static BaseDao<DemoRecord, String> dao() {
        return mock(BaseDao.class);
    }
}
