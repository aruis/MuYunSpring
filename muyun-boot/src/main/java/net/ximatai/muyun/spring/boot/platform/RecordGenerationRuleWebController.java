package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRule;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = RecordGenerationRuleService.MODULE_ALIAS,
        title = "平台生单规则")
@RequestMapping("/platform.module/{moduleAlias}/generation-rules")
public class RecordGenerationRuleWebController
        extends ModuleScopedRuleTreeWebSupport<RecordGenerationRule, RecordGenerationRuleService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "sourceModuleAlias", "targetModuleAlias", "actionCode",
            "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    public RecordGenerationRuleWebController() {
        super(QUERY_FIELDS, "sourceModuleAlias");
    }

    @GetMapping("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看生单规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public RecordGenerationRule viewTree(HttpServletRequest request, @PathVariable String id) {
        return webScope(() -> {
            requireScopedRecord(request, id);
            return service().viewRuleTree(id);
        });
    }

    @PostMapping("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存生单规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public RecordGenerationRule saveTree(HttpServletRequest request, @RequestBody RecordGenerationRule rule) {
        return webScope(() -> {
            if (rule == null) {
                throw new IllegalArgumentException("generation rule tree must not be null");
            }
            requireExistingRuleInScope(request, rule);
            rule.setSourceModuleAlias(moduleAlias(request));
            return service().saveRuleTree(rule);
        });
    }

    @Override
    protected String scopeValue(RecordGenerationRule record) {
        return record.getSourceModuleAlias();
    }
}
