package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionKind;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionAvailability;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DynamicRecordWebController.class)
@Import(DynamicRecordWebControllerIT.StaticContractController.class)
class DynamicRecordWebControllerIT {
    private static final String MODULE = "sales.contract";
    private static final String ENTITY = "contract";

    private final MockMvc mvc;

    @MockitoBean
    private DynamicRecordService recordService;

    @Autowired
    DynamicRecordWebControllerIT(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void shouldLetStaticControllerTakeOverExactAliasPathAndKeepDynamicFallback() throws Exception {
        mvc.perform(post("/{moduleAlias}/query", MODULE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("static"));
        verifyNoInteractions(recordService);

        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(recordService.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(recordService.select(MODULE, ENTITY, "contract-1")).thenReturn(record);

        mvc.perform(post("/{moduleAlias}/view/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("contract-1"))
                .andExpect(jsonPath("$.values.code").value("C-001"));
    }

    @Test
    void shouldNotCaptureRootFileLikePathInRealMvcMapping() throws Exception {
        mvc.perform(get("/openapi.json"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(recordService);
    }

    @Test
    void shouldRouteRecordActionAvailabilityBeforeGenericActionPath() throws Exception {
        DynamicRecord record = new DynamicRecord(entity()).setValue("code", "C-001");
        record.setId("contract-1");
        when(recordService.mainEntityAlias(MODULE)).thenReturn(ENTITY);
        when(recordService.select(MODULE, ENTITY, "contract-1")).thenReturn(record);
        when(recordService.actions(MODULE)).thenReturn(List.of(action("submit", EntityActionLevel.RECORD)));
        when(recordService.actionAvailability(eq(MODULE), eq("submit"), any(DynamicRecord.class)))
                .thenReturn(DynamicActionAvailability.available("submit"));

        mvc.perform(post("/{moduleAlias}/actions/{recordId}", MODULE, "contract-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action.code").value("submit"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    private EntityDefinition entity() {
        return new EntityDefinition(ENTITY, "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required()
        ));
    }

    private DynamicActionDescriptor action(String code, EntityActionLevel level) {
        return new DynamicActionDescriptor(code, DynamicActionKind.CUSTOM, "Submit", true,
                EntityActionStyle.PRIMARY, level, EntityActionCategory.CUSTOM,
                EntityActionAccessMode.AUTH_REQUIRED, true, false, null, false, null,
                EntityActionExecutorType.SERVICE, "submitExecutor");
    }

    @RestController
    @RequestMapping("/sales.contract")
    static class StaticContractController {
        @PostMapping("/query")
        Map<String, String> query() {
            return Map.of("source", "static");
        }
    }
}
