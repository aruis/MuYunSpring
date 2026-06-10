package net.ximatai.muyun.spring.platform.duplicate;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordDuplicateCheckServiceTest {
    private static final String MODULE = "sales.contract";
    private final RecordDuplicateRuleService ruleService = new RecordDuplicateRuleService(new TestMemoryDao<>());
    private final DynamicRecordService recordService = mock(DynamicRecordService.class);
    private final DynamicEntityOperations operations = mock(DynamicEntityOperations.class);
    private final RecordDuplicateCheckService checkService =
            new RecordDuplicateCheckService(ruleService, recordService);

    @Test
    void shouldCheckDuplicateByConfiguredFieldsAndExcludeCurrentRecord() {
        seedRule("duplicate_contract", "code, customerName");
        DynamicRecord current = record("contract-1", "C-001", "Acme");
        DynamicRecord duplicate = record("contract-2", "C-001", "Acme");
        when(recordService.mainEntity(MODULE)).thenReturn(operations);
        when(operations.describe()).thenReturn(net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor.from(entity()));
        when(recordService.pageForAction(eq(MODULE), eq("contract"), eq("duplicate_contract"),
                any(Criteria.class), any(PageRequest.class), any(Sort[].class)))
                .thenReturn(PageResult.of(List.of(current, duplicate), 2, PageRequest.of(1, 10)));

        RecordDuplicateCheckResult result = checkService.check(MODULE, "duplicate_contract", "contract-1",
                Map.of("code", "C-001", "customerName", "Acme"));

        assertThat(result.duplicated()).isTrue();
        assertThat(result.fieldNames()).containsExactly("code", "customerName");
        assertThat(result.matches()).singleElement()
                .satisfies(match -> {
                    assertThat(match.recordId()).isEqualTo("contract-2");
                    assertThat(match.values()).containsEntry("code", "C-001")
                            .containsEntry("customerName", "Acme");
        });
        ArgumentCaptor<Criteria> criteria = ArgumentCaptor.forClass(Criteria.class);
        ArgumentCaptor<PageRequest> page = ArgumentCaptor.forClass(PageRequest.class);
        verify(recordService).pageForAction(eq(MODULE), eq("contract"), eq("duplicate_contract"),
                criteria.capture(), page.capture(), any(Sort[].class));
        assertThat(criteria.getValue().isEmpty()).isFalse();
        assertThat(page.getValue().getLimit()).isEqualTo(10);
    }

    @Test
    void shouldRejectMissingDuplicateFieldValue() {
        seedRule("duplicate_contract", "code,customerName");
        when(recordService.mainEntity(MODULE)).thenReturn(operations);
        when(operations.describe()).thenReturn(net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor.from(entity()));

        assertThatThrownBy(() -> checkService.check(MODULE, "duplicate_contract", null, Map.of("code", "C-001")))
                .isInstanceOf(PlatformException.class)
                .hasMessage("duplicate check requires field value: customerName");
    }

    @Test
    void shouldRejectRuleFieldMissingFromRuntimeEntity() {
        seedRule("duplicate_contract", "missingField");
        when(recordService.mainEntity(MODULE)).thenReturn(operations);
        when(operations.describe()).thenReturn(net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor.from(entity()));

        assertThatThrownBy(() -> checkService.check(MODULE, "duplicate_contract", null,
                Map.of("missingField", "x")))
                .isInstanceOf(PlatformException.class)
                .hasMessage("duplicate rule field does not exist: missingField");
    }

    @Test
    void shouldRejectEmptyDuplicateRuleFields() {
        RecordDuplicateRuleService dirtyRuleService = mock(RecordDuplicateRuleService.class);
        DynamicRecordService runtimeService = mock(DynamicRecordService.class);
        RecordDuplicateCheckService service = new RecordDuplicateCheckService(dirtyRuleService, runtimeService);
        RecordDuplicateRule rule = new RecordDuplicateRule();
        rule.setActionCode("duplicate_contract");
        when(dirtyRuleService.requireEnabledRule(MODULE, "duplicate_contract")).thenReturn(rule);
        when(dirtyRuleService.fieldNames(rule)).thenReturn(List.of());

        assertThatThrownBy(() -> service.check(MODULE, "duplicate_contract", null, Map.of()))
                .isInstanceOf(PlatformException.class)
                .hasMessage("duplicate rule requires fields: duplicate_contract");
    }

    private void seedRule(String actionCode, String fieldNames) {
        RecordDuplicateRule rule = new RecordDuplicateRule();
        rule.setModuleAlias(MODULE);
        rule.setActionCode(actionCode);
        rule.setFieldNames(fieldNames);
        ruleService.insert(rule);
    }

    private DynamicRecord record(String id, String code, String customerName) {
        DynamicRecord record = new DynamicRecord(entity())
                .setValue("code", code)
                .setValue("customerName", customerName);
        record.setId(id);
        record.setVersion(3);
        return record;
    }

    private EntityDefinition entity() {
        return new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code"),
                FieldDefinition.string("customerName", "Customer Name").column("customer_name")
        ));
    }
}
