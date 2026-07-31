package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = RecordWriteBackRuleService.MODULE_ALIAS,
        title = "平台回写规则", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.module/{moduleAlias}/write-back-rules")
public class RecordWriteBackRuleWebController
        extends ModuleScopedRuleTreeWebSupport<RecordWriteBackRule, RecordWriteBackRuleService> {

    public RecordWriteBackRuleWebController() {
        super("triggerModuleAlias");
    }

    @GetMapping("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看回写规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public RecordWriteBackRule viewTree(HttpServletRequest request, @PathVariable String id) {
        return webScope(() -> {
            requireScopedRecord(request, id);
            return service().viewRuleTree(id);
        });
    }

    @PostMapping("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存回写规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public RecordWriteBackRule saveTree(HttpServletRequest request, @RequestBody RecordWriteBackRule rule) {
        return webScope(() -> {
            if (rule == null) {
                throw new IllegalArgumentException("write-back rule tree must not be null");
            }
            requireExistingRuleInScope(request, rule);
            rule.setTriggerModuleAlias(moduleAlias(request));
            return service().saveRuleTree(rule);
        });
    }

    @Override
    protected String scopeValue(RecordWriteBackRule record) {
        return record.getTriggerModuleAlias();
    }
}
