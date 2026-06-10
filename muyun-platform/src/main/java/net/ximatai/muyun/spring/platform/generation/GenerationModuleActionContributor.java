package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.ModuleActionBindingType;
import net.ximatai.muyun.spring.platform.module.ModuleActionContribution;
import net.ximatai.muyun.spring.platform.module.ModuleActionContributionRegistrar;
import net.ximatai.muyun.spring.platform.module.ModuleActionSourceType;
import org.springframework.stereotype.Service;

@Service
public class GenerationModuleActionContributor {
    private final ModuleActionContributionRegistrar actionRegistrar;

    public GenerationModuleActionContributor(ModuleActionContributionRegistrar actionRegistrar) {
        this.actionRegistrar = actionRegistrar;
    }

    public void syncRuleAction(RecordGenerationRule rule) {
        if (rule == null || rule.getId() == null || rule.getId().isBlank()) {
            return;
        }
        if (Boolean.TRUE.equals(rule.getEnabled())) {
            actionRegistrar.register(contribution(rule));
        } else {
            disableRuleAction(rule.getId());
        }
    }

    public void disableRuleAction(String ruleId) {
        actionRegistrar.disableBySource(ModuleActionSourceType.RECORD_GENERATION_RULE, ruleId);
    }

    public ModuleActionContribution contribution(RecordGenerationRule rule) {
        if (rule == null) {
            return null;
        }
        if (rule.getId() == null || rule.getId().isBlank()) {
            throw new PlatformException("generation action contribution requires rule id");
        }
        String sourceModuleAlias = PlatformNameRules.requireModuleAlias(rule.getSourceModuleAlias());
        String actionCode = PlatformNameRules.requireActionCode(rule.getActionCode(), "actionCode");
        String title = rule.getTitle() == null || rule.getTitle().isBlank()
                ? "Generate " + rule.getTargetModuleAlias()
                : rule.getTitle();
        return new ModuleActionContribution(
                sourceModuleAlias,
                null,
                actionCode,
                actionCode,
                title,
                EntityActionCategory.GENERATE,
                EntityActionLevel.RECORD,
                EntityActionAccessMode.AUTH_REQUIRED,
                true,
                true,
                ActionDefaultGrantPolicy.NONE,
                null,
                null,
                EntityActionExecutorType.GENERATE,
                RecordGenerationActionExecutor.EXECUTOR_KEY,
                ModuleActionSourceType.RECORD_GENERATION_RULE,
                rule.getId(),
                null,
                ModuleActionBindingType.RECORD_GENERATION_RULE,
                rule.getId(),
                actionCode,
                Boolean.TRUE.equals(rule.getEnabled())
        );
    }
}
