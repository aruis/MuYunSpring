package net.ximatai.muyun.spring.platform.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttribute;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeAttributeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMapping;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeFieldMappingService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlatformPageBootstrapService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MenuService menuService;
    private final PlatformPageConfigSnapshotService snapshotService;
    private final ModuleMetadataFieldService moduleFieldService;
    private final PlatformFieldUiTypeService fieldUiTypeService;
    private final PlatformFieldUiTypeAttributeService fieldUiTypeAttributeService;
    private final PlatformFieldUiTypeFieldMappingService fieldUiTypeFieldMappingService;

    public PlatformPageBootstrapService(MenuService menuService,
                                        PlatformPageConfigSnapshotService snapshotService) {
        this(menuService, snapshotService, null);
    }

    public PlatformPageBootstrapService(MenuService menuService,
                                        PlatformPageConfigSnapshotService snapshotService,
                                        ModuleMetadataFieldService moduleFieldService) {
        this(menuService, snapshotService, moduleFieldService, null, null, null);
    }

    @Autowired
    public PlatformPageBootstrapService(MenuService menuService,
                                        PlatformPageConfigSnapshotService snapshotService,
                                        ModuleMetadataFieldService moduleFieldService,
                                        PlatformFieldUiTypeService fieldUiTypeService,
                                        PlatformFieldUiTypeAttributeService fieldUiTypeAttributeService,
                                        PlatformFieldUiTypeFieldMappingService fieldUiTypeFieldMappingService) {
        this.menuService = menuService;
        this.snapshotService = snapshotService;
        this.moduleFieldService = moduleFieldService;
        this.fieldUiTypeService = fieldUiTypeService;
        this.fieldUiTypeAttributeService = fieldUiTypeAttributeService;
        this.fieldUiTypeFieldMappingService = fieldUiTypeFieldMappingService;
    }

    public PlatformPageBootstrap bootstrapByMenu(String menuId) {
        return bootstrapByMenu(menuId, PlatformUiClientType.WEB);
    }

    public PlatformPageBootstrap bootstrapByMenu(String menuId, PlatformUiClientType clientType) {
        PlatformUiClientType requestedClientType = clientType == null ? PlatformUiClientType.WEB : clientType;
        Menu menu = menuService.currentUserVisibleMenu(menuId);
        if (menu == null) {
            throw new PlatformException("Menu is not visible or does not exist: " + menuId);
        }
        if (menu.getMenuType() != MenuType.MODULE) {
            throw new PlatformException("Page bootstrap requires MODULE menu: " + menuId);
        }
        PlatformPageConfigSnapshot snapshot = snapshotService.snapshot(menu.getModuleAlias());
        MenuPageMode pageMode = menu.getPageMode() == null ? MenuPageMode.LIST : menu.getPageMode();
        return new PlatformPageBootstrap(
                PlatformPageEntryContext.from(menu,
                        resolveDefaultUiConfigId(snapshot, menu.getDefaultUiConfigId(), pageMode, requestedClientType),
                        resolveDefaultQueryTemplateId(snapshot, menu.getDefaultQueryTemplateId())),
                requestedClientType,
                resolveConfig(snapshot, requestedClientType,
                        resolveDefaultUiConfigId(snapshot, menu.getDefaultUiConfigId(), pageMode, requestedClientType))
        );
    }

    public PlatformPageBootstrap bootstrapByModule(String moduleAlias) {
        return bootstrapByModule(moduleAlias, PlatformUiClientType.WEB);
    }

    public PlatformPageBootstrap bootstrapByModule(String moduleAlias, PlatformUiClientType clientType) {
        PlatformUiClientType requestedClientType = clientType == null ? PlatformUiClientType.WEB : clientType;
        String validAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        Menu menu = menuService.currentUserVisibleModuleMenu(validAlias);
        if (menu == null) {
            throw new PlatformException("Module menu is not visible or does not exist: " + validAlias);
        }
        PlatformPageConfigSnapshot snapshot = snapshotService.snapshot(validAlias);
        MenuPageMode pageMode = menu.getPageMode() == null ? MenuPageMode.LIST : menu.getPageMode();
        return new PlatformPageBootstrap(
                PlatformPageEntryContext.from(menu,
                        resolveDefaultUiConfigId(snapshot, menu.getDefaultUiConfigId(), pageMode, requestedClientType),
                        resolveDefaultQueryTemplateId(snapshot, menu.getDefaultQueryTemplateId())),
                requestedClientType,
                resolveConfig(snapshot, requestedClientType,
                        resolveDefaultUiConfigId(snapshot, menu.getDefaultUiConfigId(), pageMode, requestedClientType))
        );
    }

    private String resolveDefaultUiConfigId(PlatformPageConfigSnapshot snapshot,
                                            String requestedUiConfigId,
                                            MenuPageMode pageMode,
                                            PlatformUiClientType clientType) {
        if (requestedUiConfigId != null && !requestedUiConfigId.isBlank()) {
            boolean exists = snapshot.uiConfigs().stream().anyMatch(config -> Objects.equals(config.getId(), requestedUiConfigId));
            if (!exists) {
                throw new PlatformException("Default UI config is not published in module snapshot: " + requestedUiConfigId);
            }
            PlatformUiConfig config = snapshot.uiConfigs().stream()
                    .filter(item -> Objects.equals(item.getId(), requestedUiConfigId))
                    .findFirst()
                    .orElseThrow();
            PlatformUiSet set = snapshot.uiSets().stream()
                    .filter(item -> Objects.equals(item.getId(), config.getUiSetId()))
                    .findFirst()
                    .orElseThrow();
            if (set.getSetType() != uiSetType(pageMode)) {
                throw new PlatformException("Default UI config type must match page mode: " + pageMode);
            }
            if (config.getClientType() != clientType) {
                throw new PlatformException("Default UI config client type must match requested client type: "
                        + clientType);
            }
            return requestedUiConfigId;
        }
        PlatformUiSetType targetType = uiSetType(pageMode);
        return snapshot.uiSets().stream()
                .filter(set -> set.getSetType() == targetType)
                .filter(set -> Boolean.TRUE.equals(set.getDefaultSet()))
                .flatMap(set -> snapshot.uiConfigs().stream()
                        .filter(config -> Objects.equals(config.getUiSetId(), set.getId()))
                        .filter(config -> config.getClientType() == clientType))
                .map(PlatformUiConfig::getId)
                .findFirst()
                .orElse(null);
    }

    private String resolveDefaultQueryTemplateId(PlatformPageConfigSnapshot snapshot, String requestedTemplateId) {
        if (requestedTemplateId != null && !requestedTemplateId.isBlank()) {
            boolean exists = snapshot.queryTemplates().stream()
                    .anyMatch(template -> Objects.equals(template.getId(), requestedTemplateId));
            if (!exists) {
                throw new PlatformException("Default query template is not published or enabled in module snapshot: "
                        + requestedTemplateId);
            }
            return requestedTemplateId;
        }
        return snapshot.queryTemplates().stream()
                .filter(template -> Boolean.TRUE.equals(template.getDefaultTemplate()))
                .map(PlatformQueryTemplate::getId)
                .findFirst()
                .orElse(null);
    }

    private PlatformUiSetType uiSetType(MenuPageMode pageMode) {
        if (pageMode == MenuPageMode.FORM) {
            return PlatformUiSetType.FORM;
        }
        if (pageMode == MenuPageMode.DETAIL) {
            return PlatformUiSetType.DETAIL;
        }
        return PlatformUiSetType.LIST;
    }

    private PlatformResolvedPageConfig resolveConfig(PlatformPageConfigSnapshot snapshot,
                                                     PlatformUiClientType clientType,
                                                     String defaultUiConfigId) {
        if (moduleFieldService == null) {
            return PlatformResolvedPageConfig.empty();
        }
        Set<String> clientUiConfigIds = snapshot.uiConfigs().stream()
                .filter(config -> config.getClientType() == clientType)
                .map(PlatformUiConfig::getId)
                .collect(java.util.stream.Collectors.toSet());
        List<PlatformResolvedUiField> uiFields = snapshot.uiFields().stream()
                .filter(field -> clientUiConfigIds.contains(field.getUiConfigId()))
                .map(this::resolvedUiField)
                .toList();
        List<PlatformResolvedQueryItem> queryItems = snapshot.queryItems().stream()
                .map(this::resolvedQueryItem)
                .toList();
        return new PlatformResolvedPageConfig(uiFields, queryItems, resolvedFieldUiTypes(uiFields),
                associationBlocks(snapshot, clientType, defaultUiConfigId));
    }

    private List<PlatformAssociationBlock> associationBlocks(PlatformPageConfigSnapshot snapshot,
                                                             PlatformUiClientType clientType,
                                                             String defaultUiConfigId) {
        if (defaultUiConfigId == null || defaultUiConfigId.isBlank()) {
            return List.of();
        }
        return snapshot.uiConfigs().stream()
                .filter(config -> config.getClientType() == clientType)
                .filter(config -> Objects.equals(config.getId(), defaultUiConfigId))
                .flatMap(config -> associationBlocks(snapshot.moduleAlias(), config).stream())
                .toList();
    }

    private List<PlatformAssociationBlock> associationBlocks(String moduleAlias, PlatformUiConfig config) {
        String layoutJson = config.getLayoutJson();
        if (layoutJson == null || layoutJson.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(layoutJson);
        } catch (JsonProcessingException exception) {
            throw new PlatformException("UI config layout JSON cannot be decoded: " + config.getId());
        }
        JsonNode blocks = root.get("blocks");
        if (blocks == null || !blocks.isArray()) {
            return List.of();
        }
        java.util.ArrayList<PlatformAssociationBlock> resolved = new java.util.ArrayList<>();
        for (JsonNode block : blocks) {
            if (block == null || !block.isObject() || !"associationView".equals(text(block, "type"))) {
                continue;
            }
            String viewCode = text(block, "viewCode");
            if (viewCode == null) {
                continue;
            }
            resolved.add(new PlatformAssociationBlock(
                    config.getId(),
                    text(block, "key"),
                    viewCode,
                    text(block, "title"),
                    text(block, "uiConfigId"),
                    text(block, "queryTemplateId"),
                    "/" + moduleAlias + "/view/{id}/associations/" + viewCode + "/query"
            ));
        }
        return resolved;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    private PlatformResolvedUiField resolvedUiField(PlatformUiConfigField field) {
        ResolvedModuleMetadataField resolved = moduleFieldService.resolve(field.getModuleMetadataFieldId());
        return new PlatformResolvedUiField(
                field.getUiConfigId(),
                field.getModuleMetadataFieldId(),
                resolved.relationAlias(),
                resolved.metadataAlias(),
                resolved.fieldName(),
                resolved.columnName(),
                resolved.fieldTitle(),
                resolved.fieldTypeAlias(),
                field.getFieldUiTypeAlias(),
                field.getVisible(),
                field.getReadOnly(),
                field.getRequiredOverride(),
                field.getPlaceholder(),
                field.getDefaultValue(),
                field.getWidth(),
                field.getAlign(),
                field.getFixedPosition()
        );
    }

    private PlatformResolvedQueryItem resolvedQueryItem(PlatformQueryItem item) {
        ResolvedModuleMetadataField resolved = item.getModuleMetadataFieldId() == null
                || item.getModuleMetadataFieldId().isBlank()
                ? null
                : moduleFieldService.resolve(item.getModuleMetadataFieldId());
        return new PlatformResolvedQueryItem(
                item.getQueryTemplateId(),
                item.getId(),
                item.getParentId(),
                item.getGroupOperator(),
                item.getModuleMetadataFieldId(),
                resolved == null ? null : resolved.relationAlias(),
                resolved == null ? null : resolved.metadataAlias(),
                resolved == null ? null : resolved.fieldName(),
                resolved == null ? null : resolved.fieldTitle(),
                resolved == null ? null : resolved.fieldTypeAlias(),
                item.getOperator(),
                item.getDefaultValue(),
                item.getAllowExternalValue(),
                item.getExternalValueKey(),
                item.getTimeZone()
        );
    }

    private List<PlatformResolvedFieldUiType> resolvedFieldUiTypes(List<PlatformResolvedUiField> uiFields) {
        if (fieldUiTypeService == null || fieldUiTypeAttributeService == null || fieldUiTypeFieldMappingService == null
                || uiFields.isEmpty()) {
            return List.of();
        }
        List<String> aliases = uiFields.stream()
                .map(PlatformResolvedUiField::fieldUiTypeAlias)
                .filter(alias -> alias != null && !alias.isBlank())
                .distinct()
                .toList();
        if (aliases.isEmpty()) {
            return List.of();
        }
        Map<String, List<PlatformFieldUiTypeAttribute>> attributesByType =
                fieldUiTypeAttributeService.listByFieldUiTypeAliases(aliases)
                        .stream()
                        .collect(Collectors.groupingBy(PlatformFieldUiTypeAttribute::getFieldUiTypeAlias));
        Map<String, List<PlatformFieldUiTypeFieldMapping>> mappingsByType =
                fieldUiTypeFieldMappingService.listByFieldUiTypeAliases(aliases)
                        .stream()
                        .collect(Collectors.groupingBy(PlatformFieldUiTypeFieldMapping::getFieldUiTypeAlias));
        List<PlatformFieldUiType> fieldUiTypes = fieldUiTypeService.listEnabledByAliases(aliases);
        Set<String> resolvedAliases = fieldUiTypes.stream()
                .map(PlatformFieldUiType::getAlias)
                .collect(Collectors.toSet());
        List<String> missingAliases = aliases.stream()
                .filter(alias -> !resolvedAliases.contains(alias))
                .toList();
        if (!missingAliases.isEmpty()) {
            throw new PlatformException("Resolved page config references disabled or missing field UI types: "
                    + missingAliases);
        }
        return fieldUiTypes.stream()
                .map(type -> resolvedFieldUiType(type, attributesByType.get(type.getAlias()),
                        mappingsByType.get(type.getAlias())))
                .toList();
    }

    private PlatformResolvedFieldUiType resolvedFieldUiType(PlatformFieldUiType type,
                                                            List<PlatformFieldUiTypeAttribute> attributes,
                                                            List<PlatformFieldUiTypeFieldMapping> mappings) {
        return new PlatformResolvedFieldUiType(
                type.getAlias(),
                type.getTitle(),
                type.getDefaultFieldTypeAlias(),
                type.getControlType(),
                type.getIcon(),
                attributes == null ? List.of() : attributes.stream()
                        .map(attribute -> new PlatformResolvedFieldUiTypeAttribute(
                                attribute.getAttributeAlias(),
                                attribute.getTitle(),
                                attribute.getValueFieldTypeAlias(),
                                attribute.getDefaultValue()))
                        .toList(),
                mappings == null ? List.of() : mappings.stream()
                        .map(mapping -> new PlatformResolvedFieldUiTypeFieldMapping(
                                mapping.getSourceKey(),
                                mapping.getTitle()))
                        .toList()
        );
    }
}
