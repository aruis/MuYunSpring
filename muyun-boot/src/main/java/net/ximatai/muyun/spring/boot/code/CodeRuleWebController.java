package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.code.CodePreviewResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.code.CodeRule;
import net.ximatai.muyun.spring.platform.code.CodeRuleService;
import net.ximatai.muyun.spring.platform.code.PreviewCodeRuleCommand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeRuleService.MODULE_ALIAS, title = "编码规则")
@RequestMapping({"/platform.code_rule", "/platform/code/rule"})
public class CodeRuleWebController extends WebSupport<CodeRuleService> implements
        ReadOnlyWeb<CodeRule, CodeRuleService>,
        EnableWeb<CodeRule, CodeRuleService>,
        SortWeb<CodeRule, CodeRuleService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "moduleAlias", "entityAlias", "metadataFieldId", "fieldName", "fieldRole", "mode",
            "orgScopeType", "orgScopeId", "globalDefault", "enabled", "effectiveFrom", "effectiveTo");

    private final CodePreviewService previewService;

    public CodeRuleWebController(CodePreviewService previewService) {
        this.previewService = previewService;
    }

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }

    @GetMapping("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeRule viewTree(@PathVariable String id) {
        return webScope(() -> service().viewRuleTree(id));
    }

    @PostMapping("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public CodeRule saveTree(@RequestBody CodeRule rule) {
        return webScope(() -> service().saveRuleTree(rule));
    }

    @PostMapping("/preview")
    @CustomActionEndpoint(value = "preview", title = "预览编码",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public CodePreviewResult preview(@RequestBody PreviewRequest request) {
        return webScope(() -> {
            PreviewRequest normalized = request == null ? PreviewRequest.empty() : request;
            CodeRule rule = normalized.rule();
            if (normalized.ruleId() != null && !normalized.ruleId().isBlank()) {
                rule = service().viewRuleTree(normalized.ruleId());
            }
            return previewService.previewDraft(new PreviewCodeRuleCommand(
                    rule,
                    normalized.context(),
                    normalized.organizationId(),
                    normalized.at(),
                    normalized.sequenceValue()
            ));
        });
    }

    public record PreviewRequest(
            String ruleId,
            CodeRule rule,
            Map<String, Object> context,
            String organizationId,
            LocalDateTime at,
            Long sequenceValue
    ) {
        static PreviewRequest empty() {
            return new PreviewRequest(null, null, Map.of(), null, null, null);
        }
    }
}
