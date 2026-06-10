package net.ximatai.muyun.spring.platform.writeback;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecordWriteBackMatchRuleService extends AbstractAbilityService<RecordWriteBackMatchRule> implements
        SoftDeleteAbility<RecordWriteBackMatchRule>,
        SortAbility<RecordWriteBackMatchRule> {
    public static final String MODULE_ALIAS = "platform.record_write_back_match_rule";
    private static final PageRequest ALL = PageRequest.of(1, 500);

    public RecordWriteBackMatchRuleService(BaseDao<RecordWriteBackMatchRule, String> matchRuleDao) {
        super(MODULE_ALIAS, RecordWriteBackMatchRule.class, matchRuleDao);
    }

    public List<RecordWriteBackMatchRule> selectByRuleId(String ruleId) {
        return list(Criteria.of().eq("ruleId", requireText(ruleId, "ruleId")), ALL, Sort.asc("sortOrder"));
    }

    @Override
    public void beforeInsert(RecordWriteBackMatchRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public void beforeUpdate(RecordWriteBackMatchRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public Criteria sortScope(RecordWriteBackMatchRule rule) {
        return Criteria.of().eq("ruleId", rule.getRuleId());
    }

    private void normalizeAndValidate(RecordWriteBackMatchRule rule) {
        if (rule == null) {
            throw new PlatformException("Record write-back match rule must not be null");
        }
        rule.setRuleId(requireText(rule.getRuleId(), "ruleId"));
        rule.setSourceField(requireText(rule.getSourceField(), "sourceField"));
        rule.setTargetField(requireText(rule.getTargetField(), "targetField"));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("Record write-back match rule " + fieldName + " must not be blank");
        }
        return value.trim();
    }
}
