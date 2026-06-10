package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PlatformUiConfigService extends AbstractAbilityService<PlatformUiConfig> implements
        SoftDeleteAbility<PlatformUiConfig>,
        EnableAbility<PlatformUiConfig>,
        SortAbility<PlatformUiConfig> {
    public static final String MODULE_ALIAS = "platform.uiConfig";
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformUiSetService uiSetService;

    public PlatformUiConfigService(BaseDao<PlatformUiConfig, String> uiConfigDao,
                                   PlatformUiSetService uiSetService) {
        super(MODULE_ALIAS, PlatformUiConfig.class, uiConfigDao);
        this.uiSetService = uiSetService;
    }

    @Override
    public void beforeInsert(PlatformUiConfig uiConfig) {
        normalizeAndValidate(uiConfig);
    }

    @Override
    public void beforeUpdate(PlatformUiConfig uiConfig) {
        normalizeAndValidate(uiConfig);
        PlatformUiConfig existing = selectIncludingDeleted(uiConfig.getId());
        rejectChanged(existing, uiConfig, "UI config set", PlatformUiConfig::getUiSetId);
        rejectChanged(existing, uiConfig, "UI config client", PlatformUiConfig::getClientType);
    }

    @Override
    public Criteria sortScope(PlatformUiConfig uiConfig) {
        return Criteria.of().eq("uiSetId", uiConfig.getUiSetId());
    }

    @Override
    public void validateSortScope(PlatformUiConfig left, PlatformUiConfig right) {
        if (!Objects.equals(left.getUiSetId(), right.getUiSetId())) {
            throw new PlatformException("UI config sort can only move records within the same UI set");
        }
    }

    public PlatformUiConfig requireUiConfig(String id) {
        PlatformUiConfig uiConfig = id == null || id.isBlank() ? null : select(id);
        if (uiConfig == null) {
            throw new PlatformException("UI config requires existing config: " + id);
        }
        return uiConfig;
    }

    public List<PlatformUiConfig> listByUiSetIds(List<String> uiSetIds) {
        if (uiSetIds == null || uiSetIds.isEmpty()) {
            return List.of();
        }
        return list(enabledCriteria(Criteria.of().in("uiSetId", uiSetIds)), ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<PlatformUiConfig> listPublishedByUiSetIds(List<String> uiSetIds) {
        if (uiSetIds == null || uiSetIds.isEmpty()) {
            return List.of();
        }
        return list(enabledCriteria(Criteria.of()
                        .in("uiSetId", uiSetIds)
                        .eq("published", Boolean.TRUE)),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private void normalizeAndValidate(PlatformUiConfig uiConfig) {
        PlatformUiSet uiSet = uiSetService.requireUiSet(uiConfig.getUiSetId());
        if (uiConfig.getClientType() == null) {
            uiConfig.setClientType(PlatformUiClientType.WEB);
        }
        if (uiConfig.getPublished() == null) {
            uiConfig.setPublished(Boolean.FALSE);
        }
        if (uiConfig.getTitle() == null || uiConfig.getTitle().isBlank()) {
            uiConfig.setTitle(uiSet.getTitle() + "-" + uiConfig.getClientType().name());
        }
        rejectDuplicate(uiConfig, Criteria.of()
                        .eq("uiSetId", uiSet.getId())
                        .eq("clientType", uiConfig.getClientType()),
                "UI config client must be unique in UI set: " + uiSet.getId() + "." + uiConfig.getClientType());
        uiConfig.setUiSetId(uiSet.getId());
    }
}
