package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataFieldService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class PlatformUiConfigScaffoldService {
    private final PlatformUiSetService uiSetService;
    private final PlatformUiConfigService uiConfigService;
    private final PlatformUiConfigFieldService uiConfigFieldService;
    private final ModuleMetadataFieldService moduleFieldService;
    private final PlatformFieldTypeService fieldTypeService;
    private final PlatformFieldUiTypeService fieldUiTypeService;

    public PlatformUiConfigScaffoldService(PlatformUiSetService uiSetService,
                                           PlatformUiConfigService uiConfigService,
                                           PlatformUiConfigFieldService uiConfigFieldService,
                                           ModuleMetadataFieldService moduleFieldService,
                                           PlatformFieldTypeService fieldTypeService,
                                           PlatformFieldUiTypeService fieldUiTypeService) {
        this.uiSetService = uiSetService;
        this.uiConfigService = uiConfigService;
        this.uiConfigFieldService = uiConfigFieldService;
        this.moduleFieldService = moduleFieldService;
        this.fieldTypeService = fieldTypeService;
        this.fieldUiTypeService = fieldUiTypeService;
    }

    public List<String> scaffoldDefaultClientConfigs(String uiSetId) {
        List<String> ids = new ArrayList<>();
        ids.add(scaffoldClientConfig(uiSetId, PlatformUiClientType.WEB));
        ids.add(scaffoldClientConfig(uiSetId, PlatformUiClientType.APP));
        return List.copyOf(ids);
    }

    public String scaffoldClientConfig(String uiSetId, PlatformUiClientType clientType) {
        PlatformUiSet uiSet = uiSetService.requireUiSet(uiSetId);
        PlatformUiClientType requestedClientType = clientType == null ? PlatformUiClientType.WEB : clientType;
        PlatformUiConfig existing = existingConfig(uiSet.getId(), requestedClientType);
        if (existing != null) {
            return existing.getId();
        }
        PlatformUiConfig uiConfig = new PlatformUiConfig();
        uiConfig.setUiSetId(uiSet.getId());
        uiConfig.setClientType(requestedClientType);
        String uiConfigId = uiConfigService.insert(uiConfig);
        for (ModuleMetadataField moduleField : moduleFieldService.listMainByModuleAlias(uiSet.getModuleAlias())) {
            ResolvedModuleMetadataField resolved = moduleFieldService.resolve(moduleField.getId());
            PlatformUiConfigField field = new PlatformUiConfigField();
            field.setUiConfigId(uiConfigId);
            field.setModuleMetadataFieldId(resolved.moduleMetadataFieldId());
            field.setFieldUiTypeAlias(defaultUiTypeAlias(resolved));
            field.setVisible(Boolean.TRUE);
            field.setReadOnly(uiSet.getSetType() == PlatformUiSetType.DETAIL);
            field.setSortOrder(moduleField.getSortOrder());
            uiConfigFieldService.insert(field);
        }
        return uiConfigId;
    }

    private PlatformUiConfig existingConfig(String uiSetId, PlatformUiClientType clientType) {
        return uiConfigService.findByUiSetAndClient(uiSetId, clientType);
    }

    private String defaultUiTypeAlias(ResolvedModuleMetadataField resolved) {
        PlatformFieldType fieldType = fieldTypeService.requireFieldType(resolved.fieldTypeAlias());
        if (fieldType.getDefaultUiTypeAlias() != null && !fieldType.getDefaultUiTypeAlias().isBlank()) {
            return fieldType.getDefaultUiTypeAlias();
        }
        List<String> allowedAliases = fieldType.getUiTypeAliases() == null
                ? List.of()
                : fieldType.getUiTypeAliases().stream().filter(Objects::nonNull).toList();
        List<PlatformFieldUiType> candidates = allowedAliases.isEmpty()
                ? fieldUiTypeService.listEnabledForDefaultFieldType(fieldType.getAlias())
                : fieldUiTypeService.listEnabledByAliases(allowedAliases).stream()
                .filter(type -> type.getDefaultFieldTypeAlias() == null
                        || type.getDefaultFieldTypeAlias().isBlank()
                        || Objects.equals(type.getDefaultFieldTypeAlias(), fieldType.getAlias()))
                .sorted(Comparator.comparing(PlatformFieldUiType::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (candidates.isEmpty()) {
            throw new PlatformException("Cannot scaffold UI config field without field UI type: "
                    + resolved.fieldTypeAlias() + "." + resolved.fieldName());
        }
        return candidates.getFirst().getAlias();
    }
}
