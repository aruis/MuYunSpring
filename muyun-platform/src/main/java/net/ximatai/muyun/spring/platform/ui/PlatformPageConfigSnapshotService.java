package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformPageConfigSnapshotService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformUiSetService uiSetService;
    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiConfigFieldService uiConfigFieldService;
    private final PlatformQueryTemplateService queryTemplateService;
    private final PlatformQueryItemService queryItemService;

    public PlatformPageConfigSnapshotService(PlatformUiSetService uiSetService,
                                             PlatformUiConfigService uiConfigService,
                                             PlatformUiConfigFieldService uiConfigFieldService,
                                             PlatformQueryTemplateService queryTemplateService,
                                             PlatformQueryItemService queryItemService) {
        this.uiSetService = uiSetService;
        this.uiConfigService = uiConfigService;
        this.uiConfigFieldService = uiConfigFieldService;
        this.queryTemplateService = queryTemplateService;
        this.queryItemService = queryItemService;
    }

    public PlatformPageConfigSnapshot snapshot(String moduleAlias) {
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        List<PlatformUiSet> uiSets = uiSetService.list(uiSetService.enabledCriteria(Criteria.of().eq("moduleAlias", validAlias)),
                ALL, Sort.asc(PlatformAbilityFields.SORT_FIELD));
        List<PlatformUiConfig> uiConfigs = uiConfigService.listPublishedByUiSetIds(uiSets.stream()
                .map(PlatformUiSet::getId)
                .toList());
        List<PlatformUiConfigField> uiFields = uiConfigFieldService.listByUiConfigIds(uiConfigs.stream()
                .map(PlatformUiConfig::getId)
                .toList());
        List<PlatformQueryTemplate> queryTemplates = queryTemplateService.listPublishedByModule(validAlias);
        List<PlatformQueryItem> queryItems = queryItemService.listByTemplateIds(queryTemplates.stream()
                .map(PlatformQueryTemplate::getId)
                .toList());
        return new PlatformPageConfigSnapshot(validAlias, uiSets, uiConfigs, uiFields, queryTemplates, queryItems);
    }
}
