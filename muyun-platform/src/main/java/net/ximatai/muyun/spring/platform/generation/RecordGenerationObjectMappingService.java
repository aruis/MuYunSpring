package net.ximatai.muyun.spring.platform.generation;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class RecordGenerationObjectMappingService extends AbstractAbilityService<RecordGenerationObjectMapping> implements
        SoftDeleteAbility<RecordGenerationObjectMapping>,
        SortAbility<RecordGenerationObjectMapping> {
    public static final String MODULE_ALIAS = "platform.record_generation_object_mapping";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    public RecordGenerationObjectMappingService(BaseDao<RecordGenerationObjectMapping, String> objectMappingDao) {
        super(MODULE_ALIAS, RecordGenerationObjectMapping.class, objectMappingDao);
    }

    public List<RecordGenerationObjectMapping> selectByRuleId(String ruleId) {
        if (ruleId == null || ruleId.isBlank()) {
            return List.of();
        }
        return list(Criteria.of().eq("ruleId", ruleId), ALL, Sort.asc("sortOrder"));
    }

    @Override
    public void beforeInsert(RecordGenerationObjectMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public void beforeUpdate(RecordGenerationObjectMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public Criteria sortScope(RecordGenerationObjectMapping mapping) {
        return Criteria.of().eq("ruleId", mapping.getRuleId());
    }

    @Override
    public void validateSortScope(RecordGenerationObjectMapping left, RecordGenerationObjectMapping right) {
        if (!Objects.equals(left.getRuleId(), right.getRuleId())) {
            throw new PlatformException("Object mapping sort can only move records within the same generation rule");
        }
    }

    private void normalizeAndValidate(RecordGenerationObjectMapping mapping) {
        if (mapping.getRuleId() == null || mapping.getRuleId().isBlank()) {
            throw new PlatformException("Object mapping requires ruleId");
        }
        if (mapping.getSourceObjectAlias() != null && mapping.getSourceObjectAlias().isBlank()) {
            mapping.setSourceObjectAlias(null);
        }
        if (mapping.getSourceObjectAlias() != null) {
            mapping.setSourceObjectAlias(PlatformNameRules.requireIdentifier(
                    mapping.getSourceObjectAlias(), "sourceObjectAlias"));
        }
        mapping.setTargetObjectAlias(PlatformNameRules.requireIdentifier(
                mapping.getTargetObjectAlias(), "targetObjectAlias"));
        if (mapping.getSplitDriver() == null) {
            mapping.setSplitDriver(Boolean.FALSE);
        }
        if (mapping.getTitle() == null || mapping.getTitle().isBlank()) {
            mapping.setTitle(mapping.getTargetObjectAlias());
        }
    }
}
