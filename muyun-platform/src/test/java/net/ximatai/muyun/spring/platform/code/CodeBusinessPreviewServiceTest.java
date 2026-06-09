package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodeBusinessPreviewServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-08T01:02:03Z"), ZoneOffset.UTC);

    @Test
    void shouldExposeStableEffectiveAtText() {
        CodeRuleService ruleService = ruleService();
        ruleService.saveRuleTree(rule());
        CodeBusinessPreviewService service = new CodeBusinessPreviewService(
                ruleService,
                new CodePreviewService(new FormulaEngine(clock), clock),
                clock
        );

        List<CodeBusinessPreviewItem> items = service.preview(
                "crm.order",
                "main",
                Map.of(),
                null,
                LocalDateTime.parse("2026-06-08T10:00:00.123456789"),
                null
        );

        assertThat(items).singleElement()
                .satisfies(item -> {
                    assertThat(item.value()).isEqualTo("SO");
                    assertThat(item.effectiveAt()).isEqualTo("2026-06-08T10:00:00");
                });
    }

    private CodeRuleService ruleService() {
        return new CodeRuleService(
                new TestMemoryDao<>(),
                new CodeRuleSegmentService(new TestMemoryDao<>()),
                new CodeSequencePolicyService(new TestMemoryDao<>()),
                new CodeValueMappingService(new TestMemoryDao<>())
        );
    }

    private CodeRule rule() {
        CodeRule rule = new CodeRule();
        rule.setModuleAlias("crm.order");
        rule.setEntityAlias("main");
        rule.setFieldName("orderNo");
        rule.setFieldRole(CodeFieldRole.NORMAL);
        rule.setMode(CodeMode.AUTO);
        rule.setEnabled(Boolean.TRUE);
        rule.setSegments(List.of(segment()));
        return rule;
    }

    private CodeRuleSegment segment() {
        CodeRuleSegment segment = new CodeRuleSegment();
        segment.setSegmentType(CodeSegmentType.CONSTANT);
        segment.setFixedValue("SO");
        return segment;
    }
}
