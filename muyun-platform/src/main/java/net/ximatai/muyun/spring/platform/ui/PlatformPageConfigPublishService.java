package net.ximatai.muyun.spring.platform.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformPageConfigPublishService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlatformUiSetService uiSetService;
    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiConfigFieldService uiConfigFieldService;
    private final PlatformQueryTemplateService queryTemplateService;
    private final PlatformQueryItemService queryItemService;

    public PlatformPageConfigPublishService(PlatformUiSetService uiSetService,
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

    public void publishUiConfig(String uiConfigId) {
        PlatformUiConfig uiConfig = validateUiConfigPublishable(uiConfigId);
        try (PlatformPageConfigPublishContext.Scope ignored = PlatformPageConfigPublishContext.open()) {
            uiConfigService.update(copyForPublish(uiConfig, Boolean.TRUE));
        }
    }

    public void unpublishUiConfig(String uiConfigId) {
        PlatformUiConfig uiConfig = uiConfigService.requireUiConfig(uiConfigId);
        try (PlatformPageConfigPublishContext.Scope ignored = PlatformPageConfigPublishContext.open()) {
            uiConfigService.update(copyForPublish(uiConfig, Boolean.FALSE));
        }
    }

    public PlatformUiConfig validateUiConfigPublishable(String uiConfigId) {
        PlatformUiConfig uiConfig = uiConfigService.requireUiConfig(uiConfigId);
        PlatformUiSet uiSet = uiSetService.requireUiSet(uiConfig.getUiSetId());
        if (!Boolean.TRUE.equals(uiSet.getEnabled()) || !Boolean.TRUE.equals(uiConfig.getEnabled())) {
            throw new PlatformException("UI config publish requires enabled set and config: " + uiConfigId);
        }
        uiConfigFieldService.validateUiConfigFields(uiConfig.getId());
        List<PlatformUiConfigField> fields = uiConfigFieldService.listByUiConfigIds(List.of(uiConfig.getId()));
        boolean hasVisibleField = fields.stream().anyMatch(field -> Boolean.TRUE.equals(field.getVisible()));
        if (!hasVisibleField) {
            throw new PlatformException("UI config publish requires at least one visible field: " + uiConfigId);
        }
        validateLayoutJson(uiConfig);
        return uiConfig;
    }

    public void publishQueryTemplate(String queryTemplateId) {
        PlatformQueryTemplate template = validateQueryTemplatePublishable(queryTemplateId);
        try (PlatformPageConfigPublishContext.Scope ignored = PlatformPageConfigPublishContext.open()) {
            queryTemplateService.update(copyForPublish(template, Boolean.TRUE));
        }
    }

    public void unpublishQueryTemplate(String queryTemplateId) {
        PlatformQueryTemplate template = queryTemplateService.requireQueryTemplate(queryTemplateId);
        try (PlatformPageConfigPublishContext.Scope ignored = PlatformPageConfigPublishContext.open()) {
            queryTemplateService.update(copyForPublish(template, Boolean.FALSE));
        }
    }

    public PlatformQueryTemplate validateQueryTemplatePublishable(String queryTemplateId) {
        PlatformQueryTemplate template = queryTemplateService.requireQueryTemplate(queryTemplateId);
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new PlatformException("Query template publish requires enabled template: " + queryTemplateId);
        }
        queryItemService.compile(template.getId());
        return template;
    }

    private void validateLayoutJson(PlatformUiConfig uiConfig) {
        String layoutJson = uiConfig.getLayoutJson();
        if (layoutJson == null || layoutJson.isBlank()) {
            return;
        }
        try {
            OBJECT_MAPPER.readTree(layoutJson);
        } catch (JsonProcessingException exception) {
            throw new PlatformException("UI config layout JSON cannot be decoded: " + uiConfig.getId());
        }
    }

    private PlatformUiConfig copyForPublish(PlatformUiConfig source, boolean published) {
        PlatformUiConfig target = new PlatformUiConfig();
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setVersion(source.getVersion());
        target.setUiSetId(source.getUiSetId());
        target.setClientType(source.getClientType());
        target.setLayoutJson(source.getLayoutJson());
        target.setTitle(source.getTitle());
        target.setEnabled(source.getEnabled());
        target.setSortOrder(source.getSortOrder());
        target.setPublished(published);
        return target;
    }

    private PlatformQueryTemplate copyForPublish(PlatformQueryTemplate source, boolean published) {
        PlatformQueryTemplate target = new PlatformQueryTemplate();
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setVersion(source.getVersion());
        target.setModuleAlias(source.getModuleAlias());
        target.setAlias(source.getAlias());
        target.setDefaultTemplate(source.getDefaultTemplate());
        target.setTitle(source.getTitle());
        target.setEnabled(source.getEnabled());
        target.setSortOrder(source.getSortOrder());
        target.setPublished(published);
        return target;
    }
}
