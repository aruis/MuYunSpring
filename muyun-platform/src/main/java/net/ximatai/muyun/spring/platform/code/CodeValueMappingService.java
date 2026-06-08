package net.ximatai.muyun.spring.platform.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CodeValueMappingService extends AbstractAbilityService<CodeValueMapping> implements
        SoftDeleteAbility<CodeValueMapping>,
        EnableAbility<CodeValueMapping>,
        SortAbility<CodeValueMapping> {
    public static final String MODULE_ALIAS = "platform.code_value_mapping";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    public CodeValueMappingService(BaseDao<CodeValueMapping, String> mappingDao) {
        super(MODULE_ALIAS, CodeValueMapping.class, mappingDao);
    }

    public List<CodeValueMapping> selectBySegmentId(String segmentId) {
        return list(Criteria.of().eq("segmentId", segmentId), ALL, Sort.asc("sortOrder"));
    }

    @Override
    public void beforeInsert(CodeValueMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public void beforeUpdate(CodeValueMapping mapping) {
        normalizeAndValidate(mapping);
    }

    @Override
    public Criteria sortScope(CodeValueMapping mapping) {
        return Criteria.of().eq("segmentId", mapping.getSegmentId());
    }

    @Override
    public void validateSortScope(CodeValueMapping left, CodeValueMapping right) {
        if (!Objects.equals(left.getSegmentId(), right.getSegmentId())) {
            throw new PlatformException("Code value mapping sort can only move records within the same segment");
        }
    }

    private void normalizeAndValidate(CodeValueMapping mapping) {
        if (mapping.getSegmentId() == null || mapping.getSegmentId().isBlank()) {
            throw new PlatformException("Code value mapping requires segmentId");
        }
        if (mapping.getDefaultMapping() == null) {
            mapping.setDefaultMapping(Boolean.FALSE);
        }
        if (!Boolean.TRUE.equals(mapping.getDefaultMapping())
                && (mapping.getSourceValue() == null || mapping.getSourceValue().isBlank())) {
            throw new PlatformException("Code value mapping requires sourceValue");
        }
        if (mapping.getTargetValue() == null) {
            throw new PlatformException("Code value mapping requires targetValue");
        }
        if (mapping.getTitle() == null || mapping.getTitle().isBlank()) {
            mapping.setTitle(mapping.getTargetValue());
        }
        if (mapping.getOrgScopeType() == null) {
            mapping.setOrgScopeType(CodeOrgScopeType.GLOBAL);
        }
    }
}
