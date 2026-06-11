package net.ximatai.muyun.spring.platform.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicAssociationViewDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlatformPageConfigPublishService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final java.util.Set<String> SUMMARY_AGGREGATES = java.util.Set.of(
            "sum", "avg", "max", "min", "count", "distinctCount");

    private final PlatformUiSetService uiSetService;
    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiConfigFieldService uiConfigFieldService;
    private final PlatformQueryTemplateService queryTemplateService;
    private final PlatformQueryItemService queryItemService;
    private final DynamicRecordService recordService;

    public PlatformPageConfigPublishService(PlatformUiSetService uiSetService,
                                            PlatformUiConfigService uiConfigService,
                                            PlatformUiConfigFieldService uiConfigFieldService,
                                            PlatformQueryTemplateService queryTemplateService,
                                            PlatformQueryItemService queryItemService) {
        this(uiSetService, uiConfigService, uiConfigFieldService, queryTemplateService, queryItemService, null);
    }

    @Autowired
    public PlatformPageConfigPublishService(PlatformUiSetService uiSetService,
                                            PlatformUiConfigService uiConfigService,
                                            PlatformUiConfigFieldService uiConfigFieldService,
                                            PlatformQueryTemplateService queryTemplateService,
                                            PlatformQueryItemService queryItemService,
                                            DynamicRecordService recordService) {
        this.uiSetService = uiSetService;
        this.uiConfigService = uiConfigService;
        this.uiConfigFieldService = uiConfigFieldService;
        this.queryTemplateService = queryTemplateService;
        this.queryItemService = queryItemService;
        this.recordService = recordService;
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
        validateLayoutJson(uiSet.getModuleAlias(), uiConfig);
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

    private void validateLayoutJson(String moduleAlias, PlatformUiConfig uiConfig) {
        String layoutJson = uiConfig.getLayoutJson();
        if (layoutJson == null || layoutJson.isBlank()) {
            return;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(layoutJson);
            validateLayoutRoot(moduleAlias, root, uiConfig.getId());
        } catch (JsonProcessingException exception) {
            throw new PlatformException("UI config layout JSON cannot be decoded: " + uiConfig.getId());
        }
    }

    private void validateLayoutRoot(String moduleAlias, JsonNode root, String uiConfigId) {
        if (root == null || !root.isObject()) {
            throw new PlatformException("UI config layout JSON root must be object: " + uiConfigId);
        }
        validateSummaryPanel(root.get("summaryPanel"), uiConfigId);
        validateReferenceCandidate(root.get("referenceCandidate"), "referenceCandidate", uiConfigId);
        validateReferenceCandidateArray(root.get("referenceCandidates"), "referenceCandidates", uiConfigId);
        validateChildSections(root.get("children"), "children", uiConfigId);
        validateChildSections(root.get("childSections"), "childSections", uiConfigId);
        validateKnownBlocks(moduleAlias, root.get("blocks"), uiConfigId);
    }

    private void validateSummaryPanel(JsonNode summaryPanel, String uiConfigId) {
        if (summaryPanel == null || summaryPanel.isNull()) {
            return;
        }
        if (!summaryPanel.isObject()) {
            throw layoutException(uiConfigId, "summaryPanel must be object");
        }
        JsonNode items = summaryPanel.get("items");
        if (items == null || items.isNull()) {
            return;
        }
        if (!items.isArray()) {
            throw layoutException(uiConfigId, "summaryPanel.items must be array");
        }
        for (int i = 0; i < items.size(); i++) {
            JsonNode item = items.get(i);
            if (!item.isObject()) {
                throw layoutException(uiConfigId, "summaryPanel.items[" + i + "] must be object");
            }
            JsonNode aggregate = item.get("aggregate");
            if (aggregate == null || !aggregate.isTextual() || aggregate.asText().isBlank()) {
                throw layoutException(uiConfigId, "summaryPanel.items[" + i + "].aggregate is required");
            }
            if (!SUMMARY_AGGREGATES.contains(aggregate.asText())) {
                throw layoutException(uiConfigId, "summaryPanel.items[" + i + "].aggregate is unsupported");
            }
            JsonNode fieldName = item.get("fieldName");
            if (fieldName != null && !fieldName.isNull() && !fieldName.isTextual()) {
                throw layoutException(uiConfigId, "summaryPanel.items[" + i + "].fieldName must be string");
            }
        }
    }

    private void validateReferenceCandidateArray(JsonNode candidates, String path, String uiConfigId) {
        if (candidates == null || candidates.isNull()) {
            return;
        }
        if (!candidates.isArray()) {
            throw layoutException(uiConfigId, path + " must be array");
        }
        for (int i = 0; i < candidates.size(); i++) {
            validateReferenceCandidate(candidates.get(i), path + "[" + i + "]", uiConfigId);
        }
    }

    private void validateReferenceCandidate(JsonNode candidate, String path, String uiConfigId) {
        if (candidate == null || candidate.isNull()) {
            return;
        }
        if (!candidate.isObject()) {
            throw layoutException(uiConfigId, path + " must be object");
        }
        validateOptionalText(candidate, "sourceUiConfigId", path, uiConfigId);
        validateOptionalText(candidate, "uiConfigId", path, uiConfigId);
        validateOptionalText(candidate, "queryTemplateId", path, uiConfigId);
    }

    private void validateChildSections(JsonNode sections, String path, String uiConfigId) {
        if (sections == null || sections.isNull()) {
            return;
        }
        if (!sections.isArray()) {
            throw layoutException(uiConfigId, path + " must be array");
        }
        for (int i = 0; i < sections.size(); i++) {
            JsonNode section = sections.get(i);
            String sectionPath = path + "[" + i + "]";
            if (!section.isObject()) {
                throw layoutException(uiConfigId, sectionPath + " must be object");
            }
            JsonNode relationCode = section.get("relationCode");
            if (relationCode == null || !relationCode.isTextual() || relationCode.asText().isBlank()) {
                throw layoutException(uiConfigId, sectionPath + ".relationCode is required");
            }
            validateOptionalText(section, "uiConfigId", sectionPath, uiConfigId);
        }
    }

    private void validateKnownBlocks(String moduleAlias, JsonNode blocks, String uiConfigId) {
        if (blocks == null || blocks.isNull()) {
            return;
        }
        if (!blocks.isArray()) {
            throw layoutException(uiConfigId, "blocks must be array");
        }
        for (int i = 0; i < blocks.size(); i++) {
            JsonNode block = blocks.get(i);
            String path = "blocks[" + i + "]";
            if (!block.isObject()) {
                throw layoutException(uiConfigId, path + " must be object");
            }
            validateOptionalText(block, "type", path, uiConfigId);
            validateOptionalText(block, "key", path, uiConfigId);
            validateAssociationBlock(moduleAlias, block, path, uiConfigId);
            validateLocalEditBlock(block, path, uiConfigId);
        }
    }

    private void validateAssociationBlock(String moduleAlias, JsonNode block, String path, String uiConfigId) {
        JsonNode type = block.get("type");
        if (type == null || type.isNull() || !"associationView".equals(type.asText())) {
            return;
        }
        JsonNode viewCode = block.get("viewCode");
        if (viewCode == null || !viewCode.isTextual() || viewCode.asText().isBlank()) {
            throw layoutException(uiConfigId, path + ".viewCode is required");
        }
        validateAssociationViewCode(moduleAlias, viewCode.asText(), path, uiConfigId);
        validateOptionalText(block, "title", path, uiConfigId);
        validateOptionalText(block, "uiConfigId", path, uiConfigId);
        validateOptionalText(block, "queryTemplateId", path, uiConfigId);
    }

    private void validateAssociationViewCode(String moduleAlias, String viewCode, String path, String uiConfigId) {
        if (recordService == null) {
            return;
        }
        DynamicAssociationViewDescriptor view = recordService.describe(moduleAlias).associationViews().stream()
                .filter(item -> item.code().equals(viewCode))
                .findFirst()
                .orElseThrow(() -> layoutException(uiConfigId, path + ".viewCode is unknown"));
        if (!view.queryable()) {
            throw layoutException(uiConfigId, path + ".viewCode is not queryable");
        }
    }

    private void validateLocalEditBlock(JsonNode block, String path, String uiConfigId) {
        JsonNode type = block.get("type");
        if (type == null || type.isNull() || !"localEdit".equals(type.asText())) {
            return;
        }
        JsonNode actionCode = block.get("actionCode");
        if (actionCode == null || !actionCode.isTextual() || actionCode.asText().isBlank()) {
            throw layoutException(uiConfigId, path + ".actionCode is required");
        }
        validateOptionalText(block, "title", path, uiConfigId);
    }

    private void validateOptionalText(JsonNode node, String field, String path, String uiConfigId) {
        JsonNode value = node.get(field);
        if (value != null && !value.isNull() && !value.isTextual()) {
            throw layoutException(uiConfigId, path + "." + field + " must be string");
        }
    }

    private PlatformException layoutException(String uiConfigId, String message) {
        return new PlatformException("UI config layout JSON invalid at " + message + ": " + uiConfigId);
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
