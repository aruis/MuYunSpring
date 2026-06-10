package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldReferenceGenerateRuleValidator;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RecordGenerationReferenceGenerateRuleValidator implements ModuleMetadataFieldReferenceGenerateRuleValidator {
    private final BaseDao<RecordGenerationRule, String> ruleDao;

    public RecordGenerationReferenceGenerateRuleValidator(BaseDao<RecordGenerationRule, String> ruleDao) {
        this.ruleDao = Objects.requireNonNull(ruleDao, "ruleDao must not be null");
    }

    @Override
    public void validateReferenceGenerateRule(String ruleId,
                                              String referenceModuleAlias,
                                              String ownerModuleAlias) {
        RecordGenerationRule rule = ruleId == null || ruleId.isBlank() ? null : ruleDao.findById(ruleId);
        if (rule == null) {
            throw new PlatformException("referenceGenerateRuleId requires existing generation rule: " + ruleId);
        }
        String expectedSource = PlatformNameRules.requireModuleAlias(referenceModuleAlias);
        String expectedTarget = PlatformNameRules.requireModuleAlias(ownerModuleAlias);
        if (!Objects.equals(rule.getSourceModuleAlias(), expectedSource)
                || !Objects.equals(rule.getTargetModuleAlias(), expectedTarget)) {
            throw new PlatformException("referenceGenerateRuleId requires generation rule direction: "
                    + expectedSource + " -> " + expectedTarget);
        }
    }
}
