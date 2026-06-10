package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicRelationDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEvent;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationEventType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordSaveOperation;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicWriteBackContext;
import net.ximatai.muyun.spring.platform.impact.RecordImpactRelationService;
import net.ximatai.muyun.spring.platform.impact.RecordImpactType;
import net.ximatai.muyun.spring.platform.impact.RecordOriginContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordWriteBackRuntimeServiceTest {
    @Test
    void shouldDispatchBusinessMutationEventSynchronously() {
        List<DynamicRecordMutationEvent> received = new ArrayList<>();
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(List.of(received::add));
        DynamicRecordMutationEvent event = event(RuntimeMutationSource.BUSINESS, true);

        service.onMutationEvent(event);

        assertThat(received).containsExactly(event);
    }

    @Test
    void shouldSkipWriteBackEventWhenCascadeIsSingleHop() {
        List<DynamicRecordMutationEvent> received = new ArrayList<>();
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(List.of(received::add));

        service.onMutationEvent(event(RuntimeMutationSource.WRITE_BACK, false));

        assertThat(received).isEmpty();
    }

    @Test
    void shouldRejectNullEvent() {
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(List.of());

        assertThatThrownBy(() -> service.onMutationEvent(null))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("write-back mutation event must not be null");
    }

    @Test
    void shouldSaveAndViewWriteBackRuleTree() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();

        RecordWriteBackRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getCascadeMode()).isEqualTo(RecordWriteBackCascadeMode.SINGLE_HOP);
        assertThat(saved.getMatchRules()).singleElement()
                .satisfies(match -> {
                    assertThat(match.getSourceField()).isEqualTo("contractNo");
                    assertThat(match.getTargetField()).isEqualTo("contractNo");
                });
        assertThat(saved.getFieldRules()).singleElement()
                .satisfies(field -> {
                    assertThat(field.getTargetField()).isEqualTo("receivedAmount");
                    assertThat(field.getSourceField()).isEqualTo("amount");
                    assertThat(field.getOperation()).isEqualTo(RecordWriteBackFieldOperation.COVER);
                });
    }

    @Test
    void shouldUseBusinessTransactionForSuccessfulExecutionLogWrites() throws NoSuchMethodException {
        Method insert = RecordWriteBackExecutionLogService.class
                .getMethod("insertExecutionLog", RecordWriteBackExecutionLog.class);
        Method update = RecordWriteBackExecutionLogService.class
                .getMethod("updateExecutionLog", RecordWriteBackExecutionLog.class);
        Method failure = RecordWriteBackExecutionLogService.class
                .getMethod("saveFailureExecutionLog", RecordWriteBackExecutionLog.class);

        assertThat(insert.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(update.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRED);
        assertThat(failure.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    void shouldInsertIndependentFailedExecutionLogWhenPlannedLogIsNotCommitted() {
        RecordWriteBackExecutionLogService executionLogService = executionLogService();
        RecordWriteBackExecutionLog log = plannedExecutionLog();
        log.setId("uncommitted-log");
        log.setStatus(RecordWriteBackExecutionStatus.FAILED);
        log.setMessage("target missing");

        String failedId = executionLogService.saveFailureExecutionLog(log);

        assertThat(failedId).isNotEqualTo("uncommitted-log");
        assertThat(executionLogService.selectFailed("sales.contract", PageRequest.of(1, 10)))
                .singleElement()
                .satisfies(failed -> {
                    assertThat(failed.getId()).isEqualTo(failedId);
                    assertThat(failed.getStatus()).isEqualTo(RecordWriteBackExecutionStatus.FAILED);
                    assertThat(failed.getMessage()).isEqualTo("target missing");
                    assertThat(failed.getTraceId()).isEqualTo("trace-1");
                    assertThat(failed.getRuleId()).isEqualTo("rule-1");
                });
    }

    @Test
    void shouldRejectWriteBackRuleWithoutMatchRule() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        rule.setMatchRules(List.of());

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("at least one match rule");
    }

    @Test
    void shouldAllowGenerationRelationRuleWithoutMatchRuleButRequireGenerationRuleId() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = relationRule();
        rule.setRelationGenerationRuleId(null);

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("relationGenerationRuleId");

        rule.setRelationGenerationRuleId("gen-1");
        RecordWriteBackRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getTargetLocateMode()).isEqualTo(RecordWriteBackTargetLocateMode.GENERATION_RELATION);
        assertThat(saved.getMatchRules()).isEmpty();
    }

    @Test
    void shouldClearSourceFieldForConstantFieldRule() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        RecordWriteBackFieldRule fieldRule = rule.getFieldRules().getFirst();
        fieldRule.setSourceType(RecordWriteBackFieldSourceType.CONSTANT);
        fieldRule.setSourceField("staleSource");
        fieldRule.setConstantValue("DONE");

        RecordWriteBackRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getFieldRules()).singleElement()
                .satisfies(savedFieldRule -> {
                    assertThat(savedFieldRule.getSourceType()).isEqualTo(RecordWriteBackFieldSourceType.CONSTANT);
                    assertThat(savedFieldRule.getSourceField()).isNull();
                });
    }

    @Test
    void shouldRequireChildMatchRuleForChildTargetWriteBackRule() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = childLineRule();
        rule.setMatchRules(rule.getMatchRules().stream()
                .filter(matchRule -> matchRule.getTargetRelationCode() == null)
                .toList());

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("at least one child match rule");
    }

    @Test
    void shouldRejectChildMatchRuleWithoutChildTargetDeclaration() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        RecordWriteBackMatchRule childMatch = new RecordWriteBackMatchRule();
        childMatch.setSourceField("code");
        childMatch.setTargetField("lineNo");
        childMatch.setTargetRelationCode("lines");
        rule.setMatchRules(List.of(childMatch));

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("targetRelationCode requires child target");
    }

    @Test
    void shouldRejectChildTargetFieldMatchWithoutRootMatchRule() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = childLineRule();
        rule.setMatchRules(rule.getMatchRules().stream()
                .filter(matchRule -> matchRule.getTargetRelationCode() != null)
                .toList());

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("requires at least one root match rule");
    }

    @Test
    void shouldRejectAddOrSubtractWithoutStateTrigger() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        rule.getFieldRules().getFirst().setOperation(RecordWriteBackFieldOperation.ADD);

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("state trigger mode");
    }

    @Test
    void shouldRejectInvalidTriggerModeToken() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        rule.setTriggerModes("ON_ENTER,UNKNOWN");

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("trigger mode is invalid");
    }

    @Test
    void shouldRejectAlwaysCombinedWithStateTriggerModes() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = baseRule();
        rule.setTriggerModes("ALWAYS,ON_ENTER");

        assertThatThrownBy(() -> ruleService.saveRuleTree(rule))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("ALWAYS trigger mode cannot combine");
    }

    @Test
    void shouldNormalizeCombinedStateTriggerModes() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = stateAddRule(RecordWriteBackTriggerMode.ON_ENTER);
        rule.setTriggerModes("ON_CHANGE_WHILE_EFFECTIVE, ON_EXIT, ON_ENTER");

        RecordWriteBackRule saved = ruleService.saveRuleTree(rule);

        assertThat(saved.getTriggerMode()).isEqualTo(RecordWriteBackTriggerMode.ON_ENTER);
        assertThat(saved.getTriggerModes()).isEqualTo("ON_ENTER,ON_EXIT,ON_CHANGE_WHILE_EFFECTIVE");
    }

    @Test
    void shouldApplyCoverPatchToMatchedTargetRecord() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(baseRule());
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        target.setId("invoice-1");
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(event(RuntimeMutationSource.BUSINESS, true));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        ArgumentCaptor<DynamicWriteBackContext> contextCaptor = ArgumentCaptor.forClass(DynamicWriteBackContext.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), contextCaptor.capture());
        assertThat(recordCaptor.getValue().getValue("receivedAmount")).isEqualTo(BigDecimal.TEN);
        assertThat(contextCaptor.getValue().traceId()).isEqualTo("trace-1");
        assertThat(contextCaptor.getValue().depth()).isEqualTo(2);
        assertThat(contextCaptor.getValue().cascadeAllowed()).isFalse();
        RecordWriteBackExecutionLog executionLog = logService.selectByRuleId(rule.getId(), 10).getFirst();
        assertThat(effectLogService.selectByExecutionId(executionLog.getId())).singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTraceId()).isEqualTo("trace-1");
                    assertThat(effect.getTargetModuleAlias()).isEqualTo("finance.invoice");
                    assertThat(effect.getTargetRecordId()).isEqualTo("invoice-1");
                    assertThat(effect.getTargetField()).isEqualTo("receivedAmount");
                    assertThat(effect.getSourceField()).isEqualTo("amount");
                    assertThat(effect.getOperation()).isEqualTo(RecordWriteBackFieldOperation.COVER);
                    assertThat(effect.getBeforeValue()).isEqualTo("0");
                    assertThat(effect.getAfterValue()).isEqualTo("10");
                });
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null)).hasSize(1);
    }

    @Test
    void shouldNoopRepeatedAppliedCoverEffectAcrossTransactions() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(baseRule());
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLog applied = appliedEffect(rule, "invoice-1", "10");
        effectLogService.insert(applied);
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(event(RuntimeMutationSource.BUSINESS, true));

        verify(dynamicRecordService, never()).updateWriteBack(any(), any(), any(), any());
        assertThat(logService.selectByRuleId(rule.getId(), 10)).singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(RecordWriteBackExecutionStatus.NOOP);
                    assertThat(log.getTargetRecordId()).isEqualTo("invoice-1");
                    assertThat(log.getPatchSnapshot()).isEqualTo("{}");
                });
        assertThat(logService.selectByStatus("sales.contract", RecordWriteBackExecutionStatus.NOOP, null))
                .singleElement()
                .satisfies(log -> assertThat(log.getRuleId()).isEqualTo(rule.getId()));
        assertThat(logService.selectFailed("sales.contract", null)).isEmpty();
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null)).hasSize(1);
    }

    @Test
    void shouldApplyAddPatchWhenSourceEntersEffectiveState() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_ENTER));
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("5"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventWithState("DRAFT", "APPROVED", BigDecimal.TEN, BigDecimal.TEN));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), any());
        assertThat((BigDecimal) recordCaptor.getValue().getValue("receivedAmount"))
                .isEqualByComparingTo(new BigDecimal("15"));
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null)).singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getRuleId()).isEqualTo(rule.getId());
                    assertThat(effect.getOperation()).isEqualTo(RecordWriteBackFieldOperation.ADD);
                    assertThat(effect.getStatus()).isEqualTo(RecordWriteBackEffectStatus.ACTIVE);
                    assertThat(effect.getContributionValue()).isEqualTo("10");
                    assertThat(effect.getDeltaValue()).isEqualTo("10");
                    assertThat(effect.getBeforeValue()).isEqualTo("5");
                    assertThat(effect.getAfterValue()).isEqualTo("15");
                });
    }

    @Test
    void shouldNotApplyDuplicateEnterContributionTwice() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_ENTER));
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("5"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));
        DynamicRecordMutationEvent event = eventWithState("DRAFT", "APPROVED", BigDecimal.TEN, BigDecimal.TEN);

        service.onMutationEvent(event);
        service.onMutationEvent(event);

        verify(dynamicRecordService, times(1)).updateWriteBack(eq("finance.invoice"), eq("invoice"), any(), any());
        assertThat((BigDecimal) target.getValue("receivedAmount")).isEqualByComparingTo(new BigDecimal("15"));
        assertThat(effectLogService.selectActiveContributions(rule.getId(), "sales.contract", "contract-1",
                "finance.invoice", "invoice-1", "receivedAmount", "amount")).hasSize(1);
        assertThat(logService.selectByRuleId(rule.getId(), 10))
                .extracting(RecordWriteBackExecutionLog::getStatus)
                .contains(RecordWriteBackExecutionStatus.SUCCESS, RecordWriteBackExecutionStatus.NOOP);
        assertThat(logService.selectByRuleId(rule.getId(), 10)).filteredOn(log ->
                        log.getStatus() == RecordWriteBackExecutionStatus.NOOP)
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTargetRecordId()).isEqualTo("invoice-1");
                    assertThat(log.getPatchSnapshot()).isEqualTo("{}");
                });
    }

    @Test
    void shouldApplyAddDeltaWhenSourceChangesWhileEffective() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_CHANGE_WHILE_EFFECTIVE));
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLog active = activeEffect(rule, "invoice-1", "10");
        effectLogService.insert(active);
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("15"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventWithState("APPROVED", "APPROVED", BigDecimal.TEN, new BigDecimal("14")));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), any());
        assertThat((BigDecimal) recordCaptor.getValue().getValue("receivedAmount"))
                .isEqualByComparingTo(new BigDecimal("19"));
        assertThat(effectLogService.select(active.getId()).getStatus()).isEqualTo(RecordWriteBackEffectStatus.REVERSED);
        assertThat(effectLogService.selectActiveContributions(rule.getId(), "sales.contract", "contract-1",
                "finance.invoice", "invoice-1", "receivedAmount", "amount")).singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getContributionValue()).isEqualTo("14");
                    assertThat(effect.getDeltaValue()).isEqualTo("4");
                });
    }

    @Test
    void shouldReverseActiveContributionWhenSourceExitsEffectiveState() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_EXIT));
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLog active = activeEffect(rule, "invoice-1", "10");
        effectLogService.insert(active);
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("15"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventWithState("APPROVED", "DRAFT", BigDecimal.TEN, BigDecimal.TEN));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), any());
        assertThat((BigDecimal) recordCaptor.getValue().getValue("receivedAmount"))
                .isEqualByComparingTo(new BigDecimal("5"));
        assertThat(effectLogService.select(active.getId()).getStatus()).isEqualTo(RecordWriteBackEffectStatus.REVERSED);
        assertThat(effectLogService.selectActiveContributions(rule.getId(), "sales.contract", "contract-1",
                "finance.invoice", "invoice-1", "receivedAmount", "amount")).isEmpty();
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null))
                .filteredOn(effect -> effect.getId() == null || !effect.getId().equals(active.getId()))
                .singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getStatus()).isEqualTo(RecordWriteBackEffectStatus.APPLIED);
                    assertThat(effect.getContributionValue()).isEqualTo("0");
                    assertThat(effect.getDeltaValue()).isEqualTo("-10");
                    assertThat(effect.getBeforeValue()).isEqualTo("15");
                    assertThat(effect.getAfterValue()).isEqualTo("5");
                });
    }

    @Test
    void shouldNotApplyDuplicateExitContributionTwice() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_EXIT));
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLog active = activeEffect(rule, "invoice-1", "10");
        effectLogService.insert(active);
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("15"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));
        DynamicRecordMutationEvent event = eventWithState("APPROVED", "DRAFT", BigDecimal.TEN, BigDecimal.TEN);

        service.onMutationEvent(event);
        service.onMutationEvent(event);

        verify(dynamicRecordService, times(1)).updateWriteBack(eq("finance.invoice"), eq("invoice"), any(), any());
        assertThat((BigDecimal) target.getValue("receivedAmount")).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(effectLogService.select(active.getId()).getStatus()).isEqualTo(RecordWriteBackEffectStatus.REVERSED);
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null))
                .filteredOn(effect -> effect.getId() == null || !effect.getId().equals(active.getId()))
                .hasSize(1);
        assertThat(logService.selectByRuleId(rule.getId(), 10))
                .extracting(RecordWriteBackExecutionLog::getStatus)
                .contains(RecordWriteBackExecutionStatus.SUCCESS, RecordWriteBackExecutionStatus.NOOP);
        assertThat(logService.selectByRuleId(rule.getId(), 10)).filteredOn(log ->
                        log.getStatus() == RecordWriteBackExecutionStatus.NOOP)
                .singleElement()
                .satisfies(log -> {
                    assertThat(log.getTargetRecordId()).isEqualTo("invoice-1");
                    assertThat(log.getPatchSnapshot()).isEqualTo("{}");
                });
    }

    @Test
    void shouldApplySubtractPatchWhenSourceEntersEffectiveState() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = stateAddRule(RecordWriteBackTriggerMode.ON_ENTER);
        rule.getFieldRules().getFirst().setOperation(RecordWriteBackFieldOperation.SUBTRACT);
        ruleService.saveRuleTree(rule);
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", new BigDecimal("15"));
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventWithState("DRAFT", "APPROVED", new BigDecimal("4"), new BigDecimal("4")));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), any());
        assertThat((BigDecimal) recordCaptor.getValue().getValue("receivedAmount"))
                .isEqualByComparingTo(new BigDecimal("11"));
    }

    @Test
    void shouldSkipStateRuleWhenStateTransitionDoesNotMatch() {
        RecordWriteBackRuleService ruleService = ruleService();
        ruleService.saveRuleTree(stateAddRule(RecordWriteBackTriggerMode.ON_ENTER));
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventWithState("DRAFT", "DRAFT", BigDecimal.TEN, BigDecimal.TEN));

        verify(dynamicRecordService, never()).listSystem(any(), any(), any(), any());
        verify(dynamicRecordService, never()).updateWriteBack(any(), any(), any(), any());
    }

    @Test
    void shouldLocateWriteBackTargetByGenerationRelation() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(relationRule());
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        RecordImpactRelationService impactRelationService = new RecordImpactRelationService(new TestMemoryDao<>());
        impactRelationService.registerFromOriginContext(new RecordOriginContext(
                RecordImpactType.GENERATE_PUSH,
                "sales.contract",
                "contract-1",
                "finance.invoice",
                "gen-1",
                "pushInvoice",
                "batch-1",
                "draft-1"
        ), "invoice-1", "user-1");
        DynamicRecord target = dynamicRecord("contract")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        target.setId("contract-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("sales.contract")).thenReturn("contract");
        when(dynamicRecordService.selectSystem("sales.contract", "contract", "contract-1")).thenReturn(target);
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(impactRelationService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(eventForModule("finance.invoice", "invoice", "invoice-1"));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("sales.contract"), eq("contract"),
                recordCaptor.capture(), any());
        verify(dynamicRecordService, never()).listSystem(any(), any(), any(), any());
        assertThat(recordCaptor.getValue().getId()).isEqualTo("contract-1");
        assertThat(recordCaptor.getValue().getValue("receivedAmount")).isEqualTo(BigDecimal.TEN);
        RecordWriteBackExecutionLog executionLog = logService.selectByRuleId(rule.getId(), 10).getFirst();
        assertThat(effectLogService.selectByExecutionId(executionLog.getId())).singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTriggerModuleAlias()).isEqualTo("finance.invoice");
                    assertThat(effect.getTriggerRecordId()).isEqualTo("invoice-1");
                    assertThat(effect.getTargetModuleAlias()).isEqualTo("sales.contract");
                    assertThat(effect.getTargetRecordId()).isEqualTo("contract-1");
                });
    }

    @Test
    void shouldApplyPatchToMatchedTargetChildRowThroughParentWriteBackSave() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(childLineRule());
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecord targetRoot = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        targetRoot.setId("invoice-1");
        DynamicRecord targetLine = dynamicRecord("invoice_line")
                .setValue("invoiceId", "invoice-1")
                .setValue("lineNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        targetLine.setId("line-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.relations("finance.invoice")).thenReturn(List.of(
                new DynamicRelationDescriptor("lines", "invoice", "invoice_line", "invoiceId", false, false)
        ));
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(targetRoot));
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice_line"), any(), any()))
                .thenReturn(List.of(targetLine));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(event(RuntimeMutationSource.BUSINESS, true));

        ArgumentCaptor<DynamicRecord> recordCaptor = ArgumentCaptor.forClass(DynamicRecord.class);
        verify(dynamicRecordService).updateWriteBack(eq("finance.invoice"), eq("invoice"),
                recordCaptor.capture(), any());
        DynamicRecord savedRoot = recordCaptor.getValue();
        assertThat(savedRoot.getId()).isEqualTo("invoice-1");
        assertThat(savedRoot.isPartialChildren("lines")).isTrue();
        assertThat(savedRoot.getChildren("lines")).singleElement()
                .satisfies(child -> {
                    assertThat(child.getId()).isEqualTo("line-1");
                    assertThat(child.getValue("receivedAmount")).isEqualTo(BigDecimal.TEN);
                });
        RecordWriteBackExecutionLog executionLog = logService.selectByRuleId(rule.getId(), 10).getFirst();
        assertThat(executionLog.getTargetRecordId()).isEqualTo("line-1");
        assertThat(effectLogService.selectByExecutionId(executionLog.getId())).singleElement()
                .satisfies(effect -> {
                    assertThat(effect.getTargetRecordId()).isEqualTo("line-1");
                    assertThat(effect.getTargetField()).isEqualTo("receivedAmount");
                    assertThat(effect.getBeforeValue()).isEqualTo("0");
                    assertThat(effect.getAfterValue()).isEqualTo("10");
                });
    }

    @Test
    void shouldNotRunConfiguredRulesForSingleHopWriteBackEvent() {
        RecordWriteBackRuleService ruleService = ruleService();
        ruleService.saveRuleTree(baseRule());
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(new RecordWriteBackExecutionLogService(new TestMemoryDao<>())),
                Optional.of(dynamicRecordService));

        service.onMutationEvent(event(RuntimeMutationSource.WRITE_BACK, false));

        verify(dynamicRecordService, never()).listSystem(any(), any(), any(), any());
    }

    @Test
    void shouldFailFastWhenMatchedTargetRecordDoesNotExist() {
        RecordWriteBackRuleService ruleService = ruleService();
        RecordWriteBackRule rule = ruleService.saveRuleTree(baseRule());
        RecordWriteBackExecutionLogService logService =
                new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of());
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(dynamicRecordService));

        assertThatThrownBy(() -> service.onMutationEvent(event(RuntimeMutationSource.BUSINESS, true)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("target record not found");
        assertThat(logService.selectByTraceId("trace-1")).singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(RecordWriteBackExecutionStatus.FAILED);
                    assertThat(log.getMessage()).contains("target record not found");
                    assertThat(log.getTargetRecordId()).isNull();
                });
        assertThat(logService.selectFailed("sales.contract", PageRequest.of(1, 10))).hasSize(1);
        assertThat(logService.selectByRuleId(rule.getId(), 10)).hasSize(1);
        assertThat(logService.selectByTrigger("sales.contract", "contract-1", null)).hasSize(1);
    }

    @Test
    void shouldKeepTargetAndPatchSnapshotWhenTargetSaveFails() {
        RecordWriteBackRuleService ruleService = ruleService();
        ruleService.saveRuleTree(baseRule());
        RecordingExecutionLogService logService = new RecordingExecutionLogService();
        RecordWriteBackEffectLogService effectLogService =
                new RecordWriteBackEffectLogService(new TestMemoryDao<>());
        DynamicRecord target = dynamicRecord("invoice")
                .setValue("contractNo", "C-001")
                .setValue("receivedAmount", BigDecimal.ZERO);
        target.setId("invoice-1");
        DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
        when(dynamicRecordService.mainEntityAlias("finance.invoice")).thenReturn("invoice");
        when(dynamicRecordService.listSystem(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenReturn(List.of(target));
        when(dynamicRecordService.updateWriteBack(eq("finance.invoice"), eq("invoice"), any(), any()))
                .thenThrow(new PlatformException("target save failed"));
        RecordWriteBackRuntimeService service = new RecordWriteBackRuntimeService(
                List.of(),
                Optional.of(ruleService),
                Optional.of(logService),
                Optional.of(effectLogService),
                Optional.of(dynamicRecordService));

        assertThatThrownBy(() -> service.onMutationEvent(event(RuntimeMutationSource.BUSINESS, true)))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("target save failed");

        assertThat(logService.selectByTarget("finance.invoice", "invoice-1", null)).singleElement()
                .satisfies(log -> {
                    assertThat(log.getStatus()).isEqualTo(RecordWriteBackExecutionStatus.FAILED);
                    assertThat(log.getMessage()).isEqualTo("target save failed");
                    assertThat(log.getPatchSnapshot()).contains("receivedAmount=10");
                });
        assertThat(logService.insertExecutionCalls).isEqualTo(1);
        assertThat(logService.updateExecutionCalls).isEqualTo(1);
        assertThat(effectLogService.selectByTarget("finance.invoice", "invoice-1", null)).isEmpty();
    }

    private DynamicRecordMutationEvent event(RuntimeMutationSource source, boolean cascadeAllowed) {
        DynamicRecord record = dynamicRecord("contract");
        record.setId("contract-1");
        record.setValue("code", "C-001");
        record.setValue("contractNo", "C-001");
        record.setValue("amount", BigDecimal.TEN);
        return new DynamicRecordMutationEvent(
                "event-1",
                DynamicRecordMutationEventType.AFTER_SAVE,
                "sales.contract",
                "contract",
                "contract-1",
                DynamicRecordSaveOperation.UPDATE,
                record.copy(),
                record.copy(),
                source,
                "trace-1",
                1,
                "exec-1",
                cascadeAllowed,
                Map.of()
        );
    }

    private DynamicRecordMutationEvent eventWithState(String beforeStatus,
                                                      String afterStatus,
                                                      BigDecimal beforeAmount,
                                                      BigDecimal afterAmount) {
        DynamicRecord before = dynamicRecord("contract");
        before.setId("contract-1");
        before.setValue("code", "C-001");
        before.setValue("contractNo", "C-001");
        before.setValue("amount", beforeAmount);
        before.setValue("status", beforeStatus);
        DynamicRecord after = before.copy();
        after.setValue("amount", afterAmount);
        after.setValue("status", afterStatus);
        return new DynamicRecordMutationEvent(
                "event-1",
                DynamicRecordMutationEventType.AFTER_SAVE,
                "sales.contract",
                "contract",
                "contract-1",
                DynamicRecordSaveOperation.UPDATE,
                before,
                after,
                RuntimeMutationSource.BUSINESS,
                "trace-1",
                1,
                "exec-1",
                true,
                Map.of()
        );
    }

    private DynamicRecordMutationEvent eventForModule(String moduleAlias, String entityAlias, String recordId) {
        DynamicRecord record = dynamicRecord(entityAlias);
        record.setId(recordId);
        record.setValue("code", "C-001");
        record.setValue("contractNo", "C-001");
        record.setValue("amount", BigDecimal.TEN);
        return new DynamicRecordMutationEvent(
                "event-1",
                DynamicRecordMutationEventType.AFTER_SAVE,
                moduleAlias,
                entityAlias,
                recordId,
                DynamicRecordSaveOperation.UPDATE,
                record.copy(),
                record.copy(),
                RuntimeMutationSource.BUSINESS,
                "trace-1",
                1,
                "exec-1",
                true,
                Map.of()
        );
    }

    private RecordWriteBackRuleService ruleService() {
        RecordWriteBackMatchRuleService matchRuleService =
                new RecordWriteBackMatchRuleService(new TestMemoryDao<>());
        RecordWriteBackFieldRuleService fieldRuleService =
                new RecordWriteBackFieldRuleService(new TestMemoryDao<>());
        return new RecordWriteBackRuleService(new TestMemoryDao<>(), matchRuleService, fieldRuleService);
    }

    private RecordWriteBackExecutionLogService executionLogService() {
        return new RecordWriteBackExecutionLogService(new TestMemoryDao<>());
    }

    private RecordWriteBackExecutionLog plannedExecutionLog() {
        RecordWriteBackExecutionLog log = new RecordWriteBackExecutionLog();
        log.setTraceId("trace-1");
        log.setRuleId("rule-1");
        log.setEventId("event-1");
        log.setEventType(DynamicRecordMutationEventType.AFTER_SAVE);
        log.setDepth(1);
        log.setTriggerModuleAlias("sales.contract");
        log.setTriggerRecordId("contract-1");
        log.setTargetModuleAlias("finance.invoice");
        log.setStatus(RecordWriteBackExecutionStatus.PLANNED);
        log.setEventSnapshot("{}");
        return log;
    }

    private RecordWriteBackRule baseRule() {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setTriggerModuleAlias("sales.contract");
        rule.setTargetModuleAlias("finance.invoice");
        rule.setTitle("合同回写发票");
        RecordWriteBackMatchRule matchRule = new RecordWriteBackMatchRule();
        matchRule.setSourceField("contractNo");
        matchRule.setTargetField("contractNo");
        RecordWriteBackFieldRule fieldRule = new RecordWriteBackFieldRule();
        fieldRule.setSourceField("amount");
        fieldRule.setTargetField("receivedAmount");
        fieldRule.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        fieldRule.setOperation(RecordWriteBackFieldOperation.COVER);
        rule.setMatchRules(List.of(matchRule));
        rule.setFieldRules(List.of(fieldRule));
        return rule;
    }

    private RecordWriteBackRule stateAddRule(RecordWriteBackTriggerMode triggerMode) {
        RecordWriteBackRule rule = baseRule();
        rule.setTriggerMode(triggerMode);
        rule.setTriggerField("status");
        rule.setTriggerValue("APPROVED");
        rule.getFieldRules().getFirst().setOperation(RecordWriteBackFieldOperation.ADD);
        return rule;
    }

    private RecordWriteBackRule relationRule() {
        RecordWriteBackRule rule = new RecordWriteBackRule();
        rule.setTriggerModuleAlias("finance.invoice");
        rule.setTargetModuleAlias("sales.contract");
        rule.setTitle("发票通过生成关系回写合同");
        rule.setTargetLocateMode(RecordWriteBackTargetLocateMode.GENERATION_RELATION);
        rule.setRelationGenerationRuleId("gen-1");
        RecordWriteBackFieldRule fieldRule = new RecordWriteBackFieldRule();
        fieldRule.setSourceField("amount");
        fieldRule.setTargetField("receivedAmount");
        fieldRule.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        fieldRule.setOperation(RecordWriteBackFieldOperation.COVER);
        rule.setMatchRules(List.of());
        rule.setFieldRules(List.of(fieldRule));
        return rule;
    }

    private RecordWriteBackRule childLineRule() {
        RecordWriteBackRule rule = baseRule();
        rule.setTargetRelationCode("lines");
        rule.setTargetEntityAlias("invoice_line");
        RecordWriteBackMatchRule childMatch = new RecordWriteBackMatchRule();
        childMatch.setSourceField("code");
        childMatch.setTargetField("lineNo");
        childMatch.setTargetRelationCode("lines");
        rule.setMatchRules(List.of(rule.getMatchRules().getFirst(), childMatch));
        return rule;
    }

    private RecordWriteBackEffectLog activeEffect(RecordWriteBackRule rule,
                                                  String targetRecordId,
                                                  String contributionValue) {
        RecordWriteBackEffectLog effect = new RecordWriteBackEffectLog();
        effect.setExecutionId("active-exec");
        effect.setTraceId("active-trace");
        effect.setRuleId(rule.getId());
        effect.setTriggerModuleAlias("sales.contract");
        effect.setTriggerRecordId("contract-1");
        effect.setTargetModuleAlias("finance.invoice");
        effect.setTargetRecordId(targetRecordId);
        effect.setTargetField("receivedAmount");
        effect.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        effect.setSourceField("amount");
        effect.setOperation(RecordWriteBackFieldOperation.ADD);
        effect.setStatus(RecordWriteBackEffectStatus.ACTIVE);
        effect.setContributionValue(contributionValue);
        effect.setDeltaValue(contributionValue);
        effect.setBeforeValue("5");
        effect.setAfterValue("15");
        return effect;
    }

    private RecordWriteBackEffectLog appliedEffect(RecordWriteBackRule rule,
                                                  String targetRecordId,
                                                  String afterValue) {
        RecordWriteBackEffectLog effect = new RecordWriteBackEffectLog();
        effect.setExecutionId("applied-exec");
        effect.setTraceId("applied-trace");
        effect.setRuleId(rule.getId());
        effect.setTriggerModuleAlias("sales.contract");
        effect.setTriggerRecordId("contract-1");
        effect.setTargetModuleAlias("finance.invoice");
        effect.setTargetRecordId(targetRecordId);
        effect.setTargetField("receivedAmount");
        effect.setSourceType(RecordWriteBackFieldSourceType.FIELD);
        effect.setSourceField("amount");
        effect.setOperation(RecordWriteBackFieldOperation.COVER);
        effect.setStatus(RecordWriteBackEffectStatus.APPLIED);
        effect.setBeforeValue("0");
        effect.setAfterValue(afterValue);
        return effect;
    }

    private DynamicRecord dynamicRecord(String entityAlias) {
        return new DynamicRecord(new EntityDefinition(entityAlias, "app_" + entityAlias, entityAlias, List.of(
                FieldDefinition.string("code", "Code"),
                new FieldDefinition("contractNo", "contract_no", net.ximatai.muyun.spring.dynamic.metadata.FieldType.STRING, "Contract No"),
                new FieldDefinition("invoiceId", "invoice_id",
                        net.ximatai.muyun.spring.dynamic.metadata.FieldType.STRING, "Invoice Id"),
                new FieldDefinition("lineNo", "line_no",
                        net.ximatai.muyun.spring.dynamic.metadata.FieldType.STRING, "Line No"),
                FieldDefinition.string("status", "Status"),
                FieldDefinition.decimal("amount", "Amount"),
                new FieldDefinition("receivedAmount", "received_amount", net.ximatai.muyun.spring.dynamic.metadata.FieldType.DECIMAL, "Received Amount")
        )));
    }

    private static final class RecordingExecutionLogService extends RecordWriteBackExecutionLogService {
        private int insertExecutionCalls;
        private int updateExecutionCalls;

        private RecordingExecutionLogService() {
            super(new TestMemoryDao<>());
        }

        @Override
        public String insertExecutionLog(RecordWriteBackExecutionLog log) {
            insertExecutionCalls++;
            return super.insertExecutionLog(log);
        }

        @Override
        public void updateExecutionLog(RecordWriteBackExecutionLog log) {
            updateExecutionCalls++;
            super.updateExecutionLog(log);
        }
    }
}
