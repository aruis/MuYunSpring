package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CodeGenerateService {
    private static final int DEFAULT_DUPLICATE_RETRY = 5;

    private final CodeRuleService ruleService;
    private final CodePreviewService previewService;
    private final CodeSequenceStateService sequenceStateService;
    private final CodeRecycleEntryService recycleEntryService;
    private final Clock clock;

    public CodeGenerateService(CodeRuleService ruleService,
                               CodePreviewService previewService,
                               CodeSequenceStateService sequenceStateService) {
        this(ruleService, previewService, sequenceStateService, null, Clock.systemDefaultZone());
    }

    public CodeGenerateService(CodeRuleService ruleService,
                               CodePreviewService previewService,
                               CodeSequenceStateService sequenceStateService,
                               Clock clock) {
        this(ruleService, previewService, sequenceStateService, null, clock);
    }

    public CodeGenerateService(CodeRuleService ruleService,
                               CodePreviewService previewService,
                               CodeSequenceStateService sequenceStateService,
                               CodeRecycleEntryService recycleEntryService) {
        this(ruleService, previewService, sequenceStateService, recycleEntryService, Clock.systemDefaultZone());
    }

    @Autowired
    public CodeGenerateService(CodeRuleService ruleService,
                               CodePreviewService previewService,
                               CodeSequenceStateService sequenceStateService,
                               CodeRecycleEntryService recycleEntryService,
                               Clock clock) {
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService must not be null");
        this.previewService = Objects.requireNonNull(previewService, "previewService must not be null");
        this.sequenceStateService = Objects.requireNonNull(sequenceStateService, "sequenceStateService must not be null");
        this.recycleEntryService = recycleEntryService;
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public GenerateCodeResult generate(GenerateCodeCommand command) {
        if (command == null) {
            throw new PlatformException("Code generate command must not be null");
        }
        LocalDateTime at = command.at() == null ? LocalDateTime.now(clock) : command.at();
        Map<String, Object> context = command.context() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(command.context());
        ResolvedCodeRule resolved = ruleService.resolveRule(new ResolveCodeRuleCommand(
                command.moduleAlias(),
                command.entityAlias(),
                command.metadataFieldId(),
                command.fieldName(),
                command.organizationId(),
                at
        ));
        if (resolved == null) {
            throw new PlatformException("No code rule resolved for target: "
                    + command.moduleAlias() + "/" + command.entityAlias());
        }
        return generateResolved(resolved, context, at, command.uniquenessChecker());
    }

    private GenerateCodeResult generateResolved(ResolvedCodeRule resolved,
                                                Map<String, Object> context,
                                                LocalDateTime at,
                                                CodeValueUniquenessChecker uniquenessChecker) {
        CodeRule rule = resolved.rule();
        if (rule.getMode() == CodeMode.MANUAL) {
            throw new PlatformException("MANUAL code rule does not support automatic generation");
        }
        List<CodeRuleSegment> segments = orderedSegments(rule);
        if (segments.isEmpty()) {
            throw new PlatformException("Code rule has no segments to generate");
        }
        boolean hasSequence = segments.stream().anyMatch(segment -> segment.getSegmentType() == CodeSegmentType.SEQUENCE);
        if (hasSequence && rule.getSequencePolicy() == null) {
            throw new PlatformException("Code rule with SEQUENCE segment requires sequencePolicy");
        }
        String basisKey = hasSequence ? buildBasisKey(rule, context, at) : CodeSequenceState.DEFAULT_BUCKET;
        String periodKey = hasSequence ? sequenceStateService.periodKey(rule.getSequencePolicy(), at) : CodeSequenceState.DEFAULT_BUCKET;
        GenerateCodeResult recycled = tryConsumeRecycle(resolved, context, basisKey, periodKey, at, uniquenessChecker);
        if (recycled != null) {
            return recycled;
        }

        int retry = 0;
        while (true) {
            Long sequenceValue = hasSequence
                    ? sequenceStateService.allocateNextValue(rule.getId(), basisKey, periodKey, rule.getSequencePolicy())
                    : null;
            CodePreviewResult rendered = previewService.previewDraft(new PreviewCodeRuleCommand(
                    rule,
                    context,
                    resolved.resolvedOrganizationId(),
                    at,
                    sequenceValue
            ));
            if (!isDuplicate(uniquenessChecker, resolved, rendered.value(), context)) {
                return new GenerateCodeResult(
                        rendered.value(),
                        rule.getId(),
                        rule.getMetadataFieldId(),
                        rule.getFieldName(),
                        rule.getFieldRole(),
                        resolved.resolvedOrganizationId(),
                        basisKey,
                        periodKey,
                        sequenceValue
                );
            }
            if (!hasSequence || retry >= DEFAULT_DUPLICATE_RETRY - 1) {
                throw new PlatformException("Generated code already exists: " + rendered.value());
            }
            retry++;
        }
    }

    private GenerateCodeResult tryConsumeRecycle(ResolvedCodeRule resolved,
                                                 Map<String, Object> context,
                                                 String basisKey,
                                                 String periodKey,
                                                 LocalDateTime at,
                                                 CodeValueUniquenessChecker uniquenessChecker) {
        CodeRule rule = resolved.rule();
        if (recycleEntryService == null || !Boolean.TRUE.equals(rule.getAllowRecycle())) {
            return null;
        }
        CodeRecycleEntry entry = recycleEntryService.consumeAvailable(rule.getId(), basisKey, periodKey);
        if (entry == null) {
            return null;
        }
        if (isDuplicate(uniquenessChecker, resolved, entry.getRecycledValue(), context)) {
            entry.setStatus(CodeRecycleStatus.AVAILABLE);
            recycleEntryService.update(entry);
            throw new PlatformException("Generated code already exists: " + entry.getRecycledValue());
        }
        return new GenerateCodeResult(
                entry.getRecycledValue(),
                rule.getId(),
                rule.getMetadataFieldId(),
                rule.getFieldName(),
                rule.getFieldRole(),
                resolved.resolvedOrganizationId(),
                basisKey,
                periodKey,
                null
        );
    }

    private List<CodeRuleSegment> orderedSegments(CodeRule rule) {
        return rule.getSegments() == null ? List.of() : rule.getSegments().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CodeRuleSegment::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private String buildBasisKey(CodeRule rule, Map<String, Object> context, LocalDateTime at) {
        CodePreviewResult rendered = previewService.previewDraft(new PreviewCodeRuleCommand(
                rule,
                context,
                null,
                at,
                rule.getSequencePolicy() == null ? null : rule.getSequencePolicy().getStartValue()
        ));
        List<String> items = rendered.segments().stream()
                .filter(CodePreviewSegmentResult::sequenceBasis)
                .map(segment -> basisName(rule, segment.segmentId()) + "=" + nullToEmpty(segment.value()))
                .collect(Collectors.toList());
        return items.isEmpty() ? CodeSequenceState.DEFAULT_BUCKET : String.join("||", items);
    }

    private String basisName(CodeRule rule, String segmentId) {
        return orderedSegments(rule).stream()
                .filter(segment -> Objects.equals(segment.getId(), segmentId))
                .findFirst()
                .map(segment -> {
                    if (segment.getSourceRef() != null && !segment.getSourceRef().isBlank()) {
                        return segment.getSourceRef();
                    }
                    if (segment.getSegmentType() != null) {
                        return segment.getSegmentType().name();
                    }
                    return segment.getId();
                })
                .orElse(segmentId == null ? "segment" : segmentId);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean isDuplicate(CodeValueUniquenessChecker checker,
                                ResolvedCodeRule resolved,
                                String generatedValue,
                                Map<String, Object> context) {
        return checker != null && checker.exists(resolved, generatedValue, context);
    }
}
