package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CodeBusinessPreviewService {
    private static final DateTimeFormatter EFFECTIVE_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final CodeRuleService ruleService;
    private final CodePreviewService previewService;
    private final Clock clock;

    public CodeBusinessPreviewService(CodeRuleService ruleService, CodePreviewService previewService) {
        this(ruleService, previewService, Clock.systemDefaultZone());
    }

    public CodeBusinessPreviewService(CodeRuleService ruleService, CodePreviewService previewService, Clock clock) {
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService must not be null");
        this.previewService = Objects.requireNonNull(previewService, "previewService must not be null");
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
    }

    public List<CodeBusinessPreviewItem> preview(String moduleAlias,
                                                 String entityAlias,
                                                 Map<String, Object> context,
                                                 String organizationId,
                                                 LocalDateTime at,
                                                 Long sequenceValue) {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new PlatformException("Code business preview requires moduleAlias");
        }
        if (entityAlias == null || entityAlias.isBlank()) {
            throw new PlatformException("Code business preview requires entityAlias");
        }
        LocalDateTime effectiveAt = at == null ? LocalDateTime.now(clock) : at;
        Map<String, Object> safeContext = context == null ? Map.of() : new LinkedHashMap<>(context);
        return ruleService.resolveRules(new ResolveCodeRuleCommand(
                        moduleAlias,
                        entityAlias,
                        null,
                        null,
                        organizationId,
                        effectiveAt
                ))
                .stream()
                .map(resolved -> previewOne(resolved, safeContext, organizationId, effectiveAt, sequenceValue))
                .toList();
    }

    private CodeBusinessPreviewItem previewOne(ResolvedCodeRule resolved,
                                               Map<String, Object> context,
                                               String organizationId,
                                               LocalDateTime at,
                                               Long sequenceValue) {
        CodeRule rule = resolved.rule();
        CodePreviewResult result = previewService.previewDraft(new PreviewCodeRuleCommand(
                rule,
                context,
                organizationId,
                at,
                sequenceValue
        ));
        return new CodeBusinessPreviewItem(
                rule.getId(),
                rule.getMetadataFieldId(),
                rule.getFieldName(),
                rule.getFieldRole(),
                result.value(),
                resolved.resolvedOrganizationId(),
                at == null ? null : EFFECTIVE_AT_FORMATTER.format(at)
        );
    }
}
