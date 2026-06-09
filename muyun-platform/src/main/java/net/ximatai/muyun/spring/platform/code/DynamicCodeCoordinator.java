package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
                                  @Lazy DynamicRecordService recordService) {
        this(ruleService, generateService, previewService, sequenceStateService, ledgerEntryService, recycleEntryService,
                recordService, Clock.systemDefaultZone());
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
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService must not be null");
        this.generateService = Objects.requireNonNull(generateService, "generateService must not be null");
        this.previewService = previewService == null ? new CodePreviewService() : previewService;
        this.sequenceStateService = sequenceStateService;
        this.ledgerEntryService = ledgerEntryService;
        this.recycleEntryService = recycleEntryService;
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    @Override
    public void beforeCreate(String moduleAlias, String entityAlias, DynamicRecord record) {
        if (record == null) {
            return;
        }
        LocalDateTime at = LocalDateTime.now(clock);
        String organizationId = resolveOrganizationId(record);
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
                LocalDateTime.now(clock));
    }

    @Override
    public void beforeRelationChildCreate(String moduleAlias,
                                          String parentEntityAlias,
                                          String relationCode,
                                          String childEntityAlias,
                                          DynamicRecord parent,
                                          DynamicRecord child) {
        beforeCreate(moduleAlias, childEntityAlias, child);
    }

    @Override
    public void afterRelationChildCreate(String moduleAlias,
                                         String parentEntityAlias,
                                         String relationCode,
                                         String childEntityAlias,
                                         DynamicRecord parent,
                                         DynamicRecord child,
                                         String id) {
        afterCreate(moduleAlias, childEntityAlias, child, id);
    }

    @Override
    public void beforeUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord incoming) {
        if (before == null || incoming == null) {
            return;
        }
        LocalDateTime at = LocalDateTime.now(clock);
        String organizationId = resolveOrganizationId(incoming);
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
        for (ResolvedCodeRule resolved : resolvedRules) {
            applyUpdateRule(moduleAlias, entityAlias, before, incoming, resolved, organizationId, at, context);
        }
    }

    @Override
    public void afterUpdate(String moduleAlias, String entityAlias, DynamicRecord before, DynamicRecord updated) {
        if (before == null || updated == null) {
            return;
        }
        LocalDateTime at = LocalDateTime.now(clock);
        String organizationId = resolveOrganizationId(updated);
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
        beforeUpdate(moduleAlias, childEntityAlias, childBefore, childIncoming);
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
        afterUpdate(moduleAlias, childEntityAlias, childBefore, childUpdated);
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
        afterDelete(moduleAlias, childEntityAlias, childBefore);
    }

    @Override
    public void afterDelete(String moduleAlias, String entityAlias, DynamicRecord before) {
        if (before == null) {
            return;
        }
        LocalDateTime at = LocalDateTime.now(clock);
        String organizationId = resolveOrganizationId(before);
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
                                 Map<String, Object> context) {
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
        if (dependsOnChangedField(rule, before, incoming)) {
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
        String basisKey = basisKey(rule, previousContext, at);
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

    private boolean dependsOnChangedField(CodeRule rule, DynamicRecord before, DynamicRecord incoming) {
        if (rule.getSegments() == null || rule.getSegments().isEmpty() || incoming.explicitFieldCodes().isEmpty()) {
            return false;
        }
        if (rule.getSegments().stream().anyMatch(segment -> segment.getSegmentType() == CodeSegmentType.FORMULA)) {
            return true;
        }
        Set<String> dependencies = rule.getSegments().stream()
                .filter(segment -> Set.of(CodeSegmentType.FIELD_VALUE, CodeSegmentType.VALUE_MAPPING,
                        CodeSegmentType.CONTEXT_VAR).contains(segment.getSegmentType()))
                .map(CodeRuleSegment::getSourceRef)
                .filter(this::hasText)
                .collect(Collectors.toSet());
        return incoming.explicitFieldCodes().stream()
                .filter(dependencies::contains)
                .anyMatch(field -> !sameCode(safeGetValue(before, field), safeGetValue(incoming, field)));
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
        if (record.getAuthOrganizationId() != null && !record.getAuthOrganizationId().isBlank()) {
            return record.getAuthOrganizationId();
        }
        return CurrentUserContext.currentUser()
                .map(user -> user.organizationId())
                .orElse(null);
    }
}
