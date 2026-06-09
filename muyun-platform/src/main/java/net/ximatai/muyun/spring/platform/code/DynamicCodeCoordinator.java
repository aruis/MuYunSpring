package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicMutationContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Component
public class DynamicCodeCoordinator implements DynamicRecordMutationCoordinator {
    private static final PageRequest ONE = PageRequest.of(1, 1);

    private final CodeRuleService ruleService;
    private final CodeGenerateService generateService;
    private final CodePreviewService previewService;
    private final CodeSequenceStateService sequenceStateService;
    private final CodeLedgerEntryService ledgerEntryService;
    private final CodeRecycleEntryService recycleEntryService;
    private final DynamicRecordService recordService;
    private final CodeBusinessTimeService timeService;
    private final Clock clock;

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  @Lazy DynamicRecordService recordService) {
        this(ruleService, generateService, null, null, null, null, recordService, Clock.systemDefaultZone());
    }

    @Autowired
    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  CodePreviewService previewService,
                                  CodeSequenceStateService sequenceStateService,
                                  CodeLedgerEntryService ledgerEntryService,
                                  CodeRecycleEntryService recycleEntryService,
                                  CodeBusinessTimeService timeService,
                                  @Lazy DynamicRecordService recordService) {
        this(ruleService, generateService, previewService, sequenceStateService, ledgerEntryService, recycleEntryService,
                recordService, timeService, Clock.systemDefaultZone());
    }

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  DynamicRecordService recordService,
                                  Clock clock) {
        this(ruleService, generateService, null, null, null, null, recordService, clock);
    }

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  CodeLedgerEntryService ledgerEntryService,
                                  CodeRecycleEntryService recycleEntryService,
                                  DynamicRecordService recordService,
                                  Clock clock) {
        this(ruleService, generateService, null, null, ledgerEntryService, recycleEntryService, recordService, clock);
    }

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  CodePreviewService previewService,
                                  CodeLedgerEntryService ledgerEntryService,
                                  CodeRecycleEntryService recycleEntryService,
                                  DynamicRecordService recordService,
                                  Clock clock) {
        this(ruleService, generateService, previewService, null, ledgerEntryService, recycleEntryService, recordService,
                clock);
    }

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  CodePreviewService previewService,
                                  CodeSequenceStateService sequenceStateService,
                                  CodeLedgerEntryService ledgerEntryService,
                                  CodeRecycleEntryService recycleEntryService,
                                  DynamicRecordService recordService,
                                  Clock clock) {
        this(ruleService, generateService, previewService, sequenceStateService, ledgerEntryService, recycleEntryService,
                recordService, null, clock);
    }

    public DynamicCodeCoordinator(CodeRuleService ruleService,
                                  CodeGenerateService generateService,
                                  CodePreviewService previewService,
                                  CodeSequenceStateService sequenceStateService,
                                  CodeLedgerEntryService ledgerEntryService,
                                  CodeRecycleEntryService recycleEntryService,
                                  DynamicRecordService recordService,
                                  CodeBusinessTimeService timeService,
                                  Clock clock) {
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService must not be null");
        this.generateService = Objects.requireNonNull(generateService, "generateService must not be null");
        this.previewService = previewService == null ? new CodePreviewService() : previewService;
        this.sequenceStateService = sequenceStateService;
        this.ledgerEntryService = ledgerEntryService;
        this.recycleEntryService = recycleEntryService;
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.timeService = timeService == null ? new CodeBusinessTimeService(this.clock) : timeService;
    }

    @Override
    public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
        if (record == null) {
            return;
        }
        String organizationId = resolveOrganizationId(record);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                entityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> context = recordContext(record);
        for (ResolvedCodeRule resolved : resolvedRules) {
            applyCreateRule(moduleAlias, entityAlias, record, resolved, organizationId, at, context);
        }
    }

    @Override
    public void afterCreate(String moduleAlias, String entityAlias, DynamicRecord record, String id) {
        if (record == null || id == null || id.isBlank()) {
            return;
        }
        record.setId(id);
        syncCurrentBindings(moduleAlias, entityAlias, record, recordContext(record), resolveOrganizationId(record),
                businessTime(resolveOrganizationId(record)));
    }

    @Override
    public void beforeRelationChildCreate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parent,
                                          DynamicRecord child) {
        if (child == null) {
            return;
        }
        String organizationId = resolveOrganizationId(child, parent);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                childEntityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> context = childRecordContext(parentEntityAlias, relationCode, parent, childEntityAlias, child);
        for (ResolvedCodeRule resolved : resolvedRules) {
            applyCreateRule(moduleAlias, childEntityAlias, child, resolved, organizationId, at, context);
        }
    }

    @Override
    public void afterRelationChildCreate(String moduleAlias,
                                         String parentEntityAlias,
                                         String relationCode,
                                         String childEntityAlias,
                                         DynamicRecord parent,
                                         DynamicRecord child,
                                         String id) {
        if (child == null || id == null || id.isBlank()) {
            return;
        }
        child.setId(id);
        String organizationId = resolveOrganizationId(child, parent);
        syncCurrentBindings(moduleAlias, childEntityAlias, child,
                childRecordContext(parentEntityAlias, relationCode, parent, childEntityAlias, child), organizationId,
                businessTime(organizationId));
    }

    @Override
    public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
        if (before == null || incoming == null) {
            return;
        }
        String organizationId = resolveOrganizationId(incoming);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                entityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> context = mergedRecordContext(before, incoming);
        Set<String> changedKeys = incoming.explicitFieldCodes();
        for (ResolvedCodeRule resolved : resolvedRules) {
            applyUpdateRule(moduleAlias, entityAlias, before, incoming, resolved, organizationId, at, context,
                    recordContext(before), changedKeys);
        }
    }

    @Override
    public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
        if (before == null || updated == null) {
            return;
        }
        String organizationId = resolveOrganizationId(updated);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                entityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> beforeContext = recordContext(before);
        Map<String, Object> currentContext = mergedRecordContext(before, updated);
        for (ResolvedCodeRule resolved : resolvedRules) {
            CodeRule rule = resolved.rule();
            Object oldValue = safeGetValue(before, rule.getFieldName());
            Object newValue = currentContext.get(rule.getFieldName());
            if (sameCode(oldValue, newValue)) {
                syncCurrentBinding(rule, newValue, currentContext, updated.getId(), at);
                continue;
            }
            CodeLedgerAction action = updated.isExplicitlySet(rule.getFieldName())
                    ? CodeLedgerAction.RELEASED_BY_MANUAL_EDIT
                    : CodeLedgerAction.RELEASED_BY_LINKED_UPDATE;
            releaseCode(rule, oldValue, null, beforeContext, before.getId(), action, at);
            syncCurrentBinding(rule, newValue, currentContext, updated.getId(), at);
        }
    }

    @Override
    public void beforeRelationChildUpdate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parentBefore,
                                          DynamicRecord parentIncoming,
                                          DynamicRecord childBefore,
                                          DynamicRecord childIncoming) {
        if (childBefore == null || childIncoming == null) {
            return;
        }
        String organizationId = resolveOrganizationId(childIncoming, parentIncoming, parentBefore);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                childEntityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> context = childMergedRecordContext(parentEntityAlias, relationCode, parentBefore,
                parentIncoming, childEntityAlias, childBefore, childIncoming);
        Set<String> changedKeys = childChangedKeys(parentEntityAlias, relationCode, parentIncoming,
                childEntityAlias, childIncoming);
        Map<String, Object> beforeContext = childRecordContext(parentEntityAlias, relationCode, parentBefore,
                childEntityAlias, childBefore);
        for (ResolvedCodeRule resolved : resolvedRules) {
            applyUpdateRule(moduleAlias, childEntityAlias, childBefore, childIncoming, resolved, organizationId, at,
                    context, beforeContext, changedKeys);
        }
    }

    @Override
    public void afterRelationChildUpdate(String moduleAlias,
                                         String parentEntityAlias,
                                         String relationCode,
                                         String childEntityAlias,
                                         DynamicRecord parentBefore,
                                         DynamicRecord parentUpdated,
                                         DynamicRecord childBefore,
                                         DynamicRecord childUpdated) {
        if (childBefore == null || childUpdated == null) {
            return;
        }
        String organizationId = resolveOrganizationId(childUpdated, parentUpdated, parentBefore);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                childEntityAlias,
                null,
                null,
                organizationId,
                at
        ));
        if (resolvedRules.isEmpty()) {
            return;
        }
        Map<String, Object> beforeContext = childRecordContext(parentEntityAlias, relationCode, parentBefore,
                childEntityAlias, childBefore);
        Map<String, Object> currentContext = childMergedRecordContext(parentEntityAlias, relationCode, parentBefore,
                parentUpdated, childEntityAlias, childBefore, childUpdated);
        for (ResolvedCodeRule resolved : resolvedRules) {
            CodeRule rule = resolved.rule();
            Object oldValue = safeGetValue(childBefore, rule.getFieldName());
            Object newValue = currentContext.get(rule.getFieldName());
            if (sameCode(oldValue, newValue)) {
                syncCurrentBinding(rule, newValue, currentContext, childUpdated.getId(), at);
                continue;
            }
            CodeLedgerAction action = childUpdated.isExplicitlySet(rule.getFieldName())
                    ? CodeLedgerAction.RELEASED_BY_MANUAL_EDIT
                    : CodeLedgerAction.RELEASED_BY_LINKED_UPDATE;
            releaseCode(rule, oldValue, null, beforeContext, childBefore.getId(), action, at);
            syncCurrentBinding(rule, newValue, currentContext, childUpdated.getId(), at);
        }
    }

    @Override
    public void beforeRelationChildDelete(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parentBefore,
                                          DynamicRecord childBefore) {
    }

    @Override
    public void afterRelationChildDelete(String moduleAlias,
                                         String parentEntityAlias,
                                         String relationCode,
                                         String childEntityAlias,
                                         DynamicRecord parentBefore,
                                         DynamicRecord childBefore) {
        if (childBefore == null) {
            return;
        }
        String organizationId = resolveOrganizationId(childBefore, parentBefore);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                childEntityAlias,
                null,
                null,
                organizationId,
                at
        ));
        Map<String, Object> beforeContext = childRecordContext(parentEntityAlias, relationCode, parentBefore,
                childEntityAlias, childBefore);
        for (ResolvedCodeRule resolved : resolvedRules) {
            CodeRule rule = resolved.rule();
            releaseCode(rule, safeGetValue(childBefore, rule.getFieldName()), null, beforeContext, childBefore.getId(),
                    CodeLedgerAction.RELEASED_BY_DELETE, at);
        }
    }

    @Override
    public void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
        if (before == null) {
            return;
        }
        String organizationId = resolveOrganizationId(before);
        LocalDateTime at = businessTime(organizationId);
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                entityAlias,
                null,
                null,
                organizationId,
                at
        ));
        Map<String, Object> beforeContext = recordContext(before);
        for (ResolvedCodeRule resolved : resolvedRules) {
            CodeRule rule = resolved.rule();
            releaseCode(rule, safeGetValue(before, rule.getFieldName()), null, beforeContext, before.getId(),
                    CodeLedgerAction.RELEASED_BY_DELETE, at);
        }
    }

    private void applyCreateRule(String moduleAlias,
                                 String entityAlias,
                                 DynamicRecord record,
                                 ResolvedCodeRule resolved,
                                 String organizationId,
                                 LocalDateTime at,
                                 Map<String, Object> context) {
        CodeRule rule = resolved.rule();
        if (rule.getMode() == CodeMode.MANUAL) {
            return;
        }
        String fieldName = rule.getFieldName();
        Object existingValue = safeGetValue(record, fieldName);
        boolean explicitlySet = record.isExplicitlySet(fieldName);
        if (explicitlySet && existingValue != null && !String.valueOf(existingValue).isBlank()) {
            if (rule.getMode() == CodeMode.AUTO_WITH_MANUAL_EDIT) {
                rejectOccupiedCode(rule, existingValue, null);
                baselineSequenceFromAcceptedValue(rule, existingValue, context, at);
                return;
            }
            throw new PlatformException("AUTO code field does not accept manual value: " + fieldName);
        }
        generateAndAssign(moduleAlias, entityAlias, record, rule, organizationId, at, context, null);
    }

    private void applyUpdateRule(String moduleAlias,
                                 String entityAlias,
                                 DynamicRecord before,
                                 DynamicRecord incoming,
                                 ResolvedCodeRule resolved,
                                 String organizationId,
                                 LocalDateTime at,
                                 Map<String, Object> context,
                                 Map<String, Object> beforeContext,
                                 Set<String> changedKeys) {
        CodeRule rule = resolved.rule();
        if (rule.getMode() == CodeMode.MANUAL) {
            return;
        }
        String fieldName = rule.getFieldName();
        Object oldValue = safeGetValue(before, fieldName);
        Object incomingValue = safeGetValue(incoming, fieldName);
        boolean explicitlySet = incoming.isExplicitlySet(fieldName);
        if (explicitlySet && !sameCode(oldValue, incomingValue) && hasText(incomingValue)) {
            if (rule.getMode() != CodeMode.AUTO_WITH_MANUAL_EDIT) {
                throw new PlatformException("AUTO code field does not accept manual value: " + fieldName);
            }
            rejectOccupiedCode(rule, incomingValue, incoming.getId());
            context.put(fieldName, incomingValue);
            baselineSequenceFromAcceptedValue(rule, incomingValue, context, at);
            return;
        }
        if (!hasText(oldValue)) {
            generateAndAssign(moduleAlias, entityAlias, incoming, rule, organizationId, at, context, incoming.getId());
            return;
        }
        if (!Boolean.TRUE.equals(rule.getLinkedUpdate())) {
            return;
        }
        if (dependsOnChangedField(rule, beforeContext, context, changedKeys)) {
            generateAndAssign(moduleAlias, entityAlias, incoming, rule, organizationId, at, context, incoming.getId());
        }
    }

    private void generateAndAssign(String moduleAlias,
                                   String entityAlias,
                                   DynamicRecord record,
                                   CodeRule rule,
                                   String organizationId,
                                   LocalDateTime at,
                                   Map<String, Object> context,
                                   String excludeRecordId) {
        GenerateCodeResult generated = generateService.generate(new GenerateCodeCommand(
                moduleAlias,
                entityAlias,
                rule.getMetadataFieldId(),
                rule.getFieldName(),
                organizationId,
                at,
                context,
                (candidate, generatedValue, candidateContext) -> codeExists(moduleAlias, entityAlias,
                        rule.getFieldName(), generatedValue, excludeRecordId)
        ));
        record.putGeneratedValue(rule.getFieldName(), generated.value());
        context.put(rule.getFieldName(), generated.value());
    }

    private boolean codeExists(String moduleAlias, String entityAlias, String fieldName, String generatedValue,
                               String excludeRecordId) {
        if (generatedValue == null || generatedValue.isBlank()) {
            return false;
        }
        return !recordService.entity(moduleAlias, entityAlias)
                .list(Criteria.of().eq(fieldName, generatedValue), ONE)
                .stream()
                .filter(record -> excludeRecordId == null || !Objects.equals(excludeRecordId, record.getId()))
                .toList()
                .isEmpty();
    }

    private void syncCurrentBindings(String moduleAlias,
                                     String entityAlias,
                                     DynamicRecord record,
                                     Map<String, Object> context,
                                     String organizationId,
                                     LocalDateTime at) {
        if (ledgerEntryService == null) {
            return;
        }
        List<ResolvedCodeRule> resolvedRules = ruleService.resolveRules(new ResolveCodeRuleCommand(
                moduleAlias,
                entityAlias,
                null,
                null,
                organizationId,
                at
        ));
        for (ResolvedCodeRule resolved : resolvedRules) {
            CodeRule rule = resolved.rule();
            syncCurrentBinding(rule, safeGetValue(record, rule.getFieldName()), context, record.getId(), at);
        }
    }

    private void syncCurrentBinding(CodeRule rule,
                                    Object currentValue,
                                    Map<String, Object> context,
                                    String sourceRecordId,
                                    LocalDateTime at) {
        if (ledgerEntryService == null || rule == null || rule.getMode() == CodeMode.MANUAL || !hasText(currentValue)) {
            return;
        }
        ledgerEntryService.upsertActiveBinding(
                rule,
                String.valueOf(currentValue),
                basisKey(rule, context, at),
                periodKey(rule, at),
                sourceRecordId
        );
    }

    private void rejectOccupiedCode(CodeRule rule, Object codeValue, String sourceRecordId) {
        if (ledgerEntryService == null || rule == null || !hasText(codeValue)) {
            return;
        }
        CodeLedgerEntry entry = ledgerEntryService.findByRuleAndValue(rule.getId(), String.valueOf(codeValue));
        if (entry == null || entry.getStatus() != CodeLedgerStatus.ACTIVE) {
            return;
        }
        if (sourceRecordId == null || sourceRecordId.isBlank()
                || entry.getSourceRecordId() == null || entry.getSourceRecordId().isBlank()
                || !sourceRecordId.equals(entry.getSourceRecordId())) {
            throw new PlatformException("Code value is already occupied: " + codeValue);
        }
    }

    private void releaseCode(CodeRule rule,
                             Object oldValue,
                             Object newValue,
                             Map<String, Object> previousContext,
                             String sourceRecordId,
                             CodeLedgerAction action,
                             LocalDateTime at) {
        if (rule == null || !hasText(oldValue) || sameCode(oldValue, newValue)) {
            return;
        }
        String basisKey = safeBasisKey(rule, previousContext, at);
        String periodKey = periodKey(rule, at);
        CodeLedgerStatus inactiveStatus = Boolean.TRUE.equals(rule.getAllowRecycle())
                ? CodeLedgerStatus.AVAILABLE
                : CodeLedgerStatus.DISCARDED;
        if (recycleEntryService != null) {
            recycleEntryService.record(rule, basisKey, periodKey, String.valueOf(oldValue), sourceRecordId);
        }
        if (ledgerEntryService != null) {
            ledgerEntryService.upsertInactiveBinding(rule, String.valueOf(oldValue), basisKey, periodKey, sourceRecordId,
                    inactiveStatus, action);
        }
    }

    private void baselineSequenceFromAcceptedValue(CodeRule rule,
                                                   Object codeValue,
                                                   Map<String, Object> context,
                                                   LocalDateTime at) {
        if (sequenceStateService == null || rule == null || rule.getSequencePolicy() == null || !hasText(codeValue)) {
            return;
        }
        Long acceptedSequenceValue = parseAcceptedSequenceValue(rule, String.valueOf(codeValue), context, at);
        if (acceptedSequenceValue == null) {
            return;
        }
        String basisKey = basisKey(rule, context, at);
        String periodKey = periodKey(rule, at);
        CodeSequenceState state = sequenceStateService.selectState(rule.getId(), basisKey, periodKey);
        if (state == null || state.getCurrentValue() == null || state.getCurrentValue() < acceptedSequenceValue) {
            sequenceStateService.setCurrentValue(rule.getId(), basisKey, periodKey, acceptedSequenceValue);
        }
    }

    private Long parseAcceptedSequenceValue(CodeRule rule,
                                            String codeValue,
                                            Map<String, Object> context,
                                            LocalDateTime at) {
        List<CodeRuleSegment> segments = rule.getSegments() == null ? List.of() : rule.getSegments();
        if (segments.stream().filter(segment -> segment.getSegmentType() == CodeSegmentType.SEQUENCE).count() != 1) {
            return null;
        }
        CodePreviewResult rendered = previewService.previewDraft(new PreviewCodeRuleCommand(
                rule,
                context == null ? Map.of() : context,
                null,
                at,
                0L
        ));
        List<CodeRuleSegment> sortedSegments = segments.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CodeRuleSegment::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        StringBuilder regex = new StringBuilder("^");
        int renderedIndex = 0;
        for (CodeRuleSegment segment : sortedSegments) {
            if (renderedIndex >= rendered.segments().size()) {
                break;
            }
            CodePreviewSegmentResult renderedSegment = rendered.segments().get(renderedIndex++);
            if (renderedSegment.segmentType() == CodeSegmentType.SEQUENCE) {
                regex.append("(\\d+)");
            } else {
                regex.append(Pattern.quote(renderedSegment.value() == null ? "" : renderedSegment.value()));
            }
            if (segment.getSeparator() != null && !segment.getSeparator().isEmpty()) {
                regex.append(Pattern.quote(segment.getSeparator()));
            }
        }
        regex.append("$");
        java.util.regex.Matcher matcher = Pattern.compile(regex.toString()).matcher(codeValue);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String basisKey(CodeRule rule, Map<String, Object> context, LocalDateTime at) {
        CodePreviewResult rendered = previewService.previewDraft(new PreviewCodeRuleCommand(
                rule,
                context == null ? Map.of() : context,
                null,
                at,
                rule.getSequencePolicy() == null ? null : rule.getSequencePolicy().getStartValue()
        ));
        List<String> items = rendered.segments().stream()
                .filter(CodePreviewSegmentResult::sequenceBasis)
                .map(segment -> basisName(rule, segment.segmentId()) + "=" + (segment.value() == null ? "" : segment.value()))
                .toList();
        return items.isEmpty() ? CodeSequenceState.DEFAULT_BUCKET : String.join("||", items);
    }

    private String safeBasisKey(CodeRule rule, Map<String, Object> context, LocalDateTime at) {
        try {
            return basisKey(rule, context, at);
        } catch (RuntimeException ignored) {
            return CodeSequenceState.DEFAULT_BUCKET;
        }
    }

    private String periodKey(CodeRule rule, LocalDateTime at) {
        if (rule.getSequencePolicy() == null || rule.getSequencePolicy().getResetPolicy() == null) {
            return CodeSequenceState.DEFAULT_BUCKET;
        }
        LocalDateTime effectiveAt = at == null ? LocalDateTime.now(clock) : at;
        return switch (rule.getSequencePolicy().getResetPolicy()) {
            case NONE -> CodeSequenceState.DEFAULT_BUCKET;
            case YEAR -> DateTimeFormatter.ofPattern("yyyy").format(effectiveAt);
            case MONTH -> DateTimeFormatter.ofPattern("yyyyMM").format(effectiveAt);
            case DAY -> DateTimeFormatter.ofPattern("yyyyMMdd").format(effectiveAt);
        };
    }

    private String basisName(CodeRule rule, String segmentId) {
        return rule.getSegments().stream()
                .filter(segment -> Objects.equals(segment.getId(), segmentId))
                .findFirst()
                .map(segment -> hasText(segment.getSourceRef()) ? segment.getSourceRef() : segment.getSegmentType().name())
                .orElse(segmentId == null ? "segment" : segmentId);
    }

    private Map<String, Object> recordContext(DynamicRecord record) {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>(record.getValues());
        record.getPlatformValues().forEach(context::putIfAbsent);
        return context;
    }

    private Map<String, Object> mergedRecordContext(DynamicRecord before, DynamicRecord incoming) {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>(recordContext(before));
        context.putAll(incoming.getValues());
        incoming.getPlatformValues().forEach(context::putIfAbsent);
        return context;
    }

    private boolean dependsOnChangedField(CodeRule rule,
                                          Map<String, Object> beforeContext,
                                          Map<String, Object> afterContext,
                                          Set<String> changedKeys) {
        if (rule.getSegments() == null || rule.getSegments().isEmpty() || changedKeys == null || changedKeys.isEmpty()) {
            return false;
        }
        Set<String> dependencies = dependencyKeys(rule);
        if (dependencies.isEmpty()) {
            return rule.getSegments().stream().anyMatch(segment -> segment.getSegmentType() == CodeSegmentType.FORMULA);
        }
        return changedKeys.stream()
                .filter(dependencies::contains)
                .anyMatch(key -> !Objects.equals(valueOf(beforeContext, key), valueOf(afterContext, key)));
    }

    private Set<String> dependencyKeys(CodeRule rule) {
        if (rule == null || rule.getSegments() == null || rule.getSegments().isEmpty()) {
            return Set.of();
        }
        return rule.getSegments().stream()
                .filter(segment -> Set.of(CodeSegmentType.FIELD_VALUE, CodeSegmentType.VALUE_MAPPING,
                        CodeSegmentType.CONTEXT_VAR, CodeSegmentType.FORMULA).contains(segment.getSegmentType()))
                .flatMap(segment -> segment.getSegmentType() == CodeSegmentType.FORMULA
                        ? formulaDependencies(segment.getFormulaExpr()).stream()
                        : java.util.stream.Stream.of(segment.getSourceRef()))
                .filter(this::hasText)
                .collect(Collectors.toSet());
    }

    private Object valueOf(Map<String, Object> context, String key) {
        return context == null || key == null ? null : context.get(key);
    }

    private Set<String> formulaDependencies(String expression) {
        if (expression == null || expression.isBlank()) {
            return Set.of();
        }
        java.util.LinkedHashSet<String> dependencies = new java.util.LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(expression);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (hasText(key)) {
                dependencies.add(key);
            }
        }
        return dependencies;
    }

    private Set<String> childChangedKeys(String parentEntityAlias,
                                         String relationCode,
                                         DynamicRecord parentIncoming,
                                         String childEntityAlias,
                                         DynamicRecord childIncoming) {
        java.util.LinkedHashSet<String> changedKeys = new java.util.LinkedHashSet<>();
        if (parentIncoming != null) {
            for (String field : parentIncoming.explicitFieldCodes()) {
                if (hasText(field)) {
                    addPrefixedKey(changedKeys, parentEntityAlias, field);
                    addPrefixedKey(changedKeys, relationCode, field);
                }
            }
        }
        if (childIncoming != null) {
            for (String field : childIncoming.explicitFieldCodes()) {
                if (hasText(field)) {
                    changedKeys.add(field);
                    addPrefixedKey(changedKeys, childEntityAlias, field);
                }
            }
        }
        return Set.copyOf(changedKeys);
    }

    private void addPrefixedKey(Set<String> keys, String prefix, String field) {
        if (keys != null && hasText(prefix) && hasText(field)) {
            keys.add(prefix + "." + field);
        }
    }

    private Object safeGetValue(DynamicRecord record, String fieldName) {
        try {
            return record.getValue(fieldName);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean sameCode(Object left, Object right) {
        if (!hasText(left) && !hasText(right)) {
            return true;
        }
        return Objects.equals(left == null ? null : String.valueOf(left), right == null ? null : String.valueOf(right));
    }

    private boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String resolveOrganizationId(DynamicRecord record) {
        return resolveOrganizationId(record, (DynamicRecord[]) null);
    }

    private String resolveOrganizationId(DynamicRecord record, DynamicRecord... fallbackRecords) {
        if (record.getAuthOrganizationId() != null && !record.getAuthOrganizationId().isBlank()) {
            return record.getAuthOrganizationId();
        }
        if (fallbackRecords != null) {
            for (DynamicRecord fallback : fallbackRecords) {
                if (fallback != null && fallback.getAuthOrganizationId() != null
                        && !fallback.getAuthOrganizationId().isBlank()) {
                    return fallback.getAuthOrganizationId();
                }
            }
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.organizationId())
                .orElse(null);
    }

    private LocalDateTime businessTime(String organizationId) {
        Instant instant = DynamicMutationContext.current()
                .map(DynamicMutationContext::startedAt)
                .orElseGet(clock::instant);
        return timeService.resolveBusinessLocalDateTime(organizationId, instant);
    }

    private Map<String, Object> childRecordContext(String parentEntityAlias,
                                                   String relationCode,
                                                   DynamicRecord parent,
                                                   String childEntityAlias,
                                                   DynamicRecord child) {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        if (parent != null) {
            Map<String, Object> parentContext = recordContext(parent);
            context.putAll(parentContext);
            putPrefixedContext(context, parentEntityAlias, parentContext);
            putPrefixedContext(context, relationCode, parentContext);
        }
        if (child != null) {
            Map<String, Object> childContext = recordContext(child);
            context.putAll(childContext);
            putPrefixedContext(context, childEntityAlias, childContext);
        }
        return context;
    }

    private Map<String, Object> childMergedRecordContext(String parentEntityAlias,
                                                         String relationCode,
                                                         DynamicRecord parentBefore,
                                                         DynamicRecord parentIncoming,
                                                         String childEntityAlias,
                                                         DynamicRecord childBefore,
                                                         DynamicRecord childIncoming) {
        LinkedHashMap<String, Object> context = new LinkedHashMap<>();
        LinkedHashMap<String, Object> parentContext = new LinkedHashMap<>();
        if (parentBefore != null) {
            parentContext.putAll(recordContext(parentBefore));
        }
        if (parentIncoming != null) {
            parentContext.putAll(recordContext(parentIncoming));
        }
        context.putAll(parentContext);
        putPrefixedContext(context, parentEntityAlias, parentContext);
        putPrefixedContext(context, relationCode, parentContext);
        LinkedHashMap<String, Object> childContext = new LinkedHashMap<>();
        if (childBefore != null) {
            childContext.putAll(recordContext(childBefore));
        }
        if (childIncoming != null) {
            childContext.putAll(recordContext(childIncoming));
        }
        context.putAll(childContext);
        putPrefixedContext(context, childEntityAlias, childContext);
        return context;
    }

    private void putPrefixedContext(Map<String, Object> target, String prefix, Map<String, Object> source) {
        if (target == null || source == null || source.isEmpty() || prefix == null || prefix.isBlank()) {
            return;
        }
        source.forEach((key, value) -> {
            if (hasText(key)) {
                target.put(prefix + "." + key, value);
            }
        });
    }
}
