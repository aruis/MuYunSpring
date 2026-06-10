package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PlatformPageBootstrapService {
    private final MenuService menuService;
    private final PlatformPageConfigSnapshotService snapshotService;
    private final ModuleMetadataFieldService moduleFieldService;

    public PlatformPageBootstrapService(MenuService menuService,
                                        PlatformPageConfigSnapshotService snapshotService) {
        this(menuService, snapshotService, null);
    }

    @Autowired
    public PlatformPageBootstrapService(MenuService menuService,
                                        PlatformPageConfigSnapshotService snapshotService,
                                        ModuleMetadataFieldService moduleFieldService) {
        this.menuService = menuService;
        this.snapshotService = snapshotService;
        this.moduleFieldService = moduleFieldService;
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
                resolveConfig(snapshot, requestedClientType)
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
                resolveConfig(snapshot, requestedClientType)
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
                throw new PlatformException("Default query template is not enabled in module snapshot: "
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
                                                     PlatformUiClientType clientType) {
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
        return new PlatformResolvedPageConfig(uiFields, queryItems);
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
}
