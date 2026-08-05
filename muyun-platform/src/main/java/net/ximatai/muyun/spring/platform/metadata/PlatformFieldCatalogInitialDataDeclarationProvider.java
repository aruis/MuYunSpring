package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;

import java.util.ArrayList;
import java.util.List;

/** Installs the platform field vocabulary before metadata is edited or compiled. */
public class PlatformFieldCatalogInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private final FieldSpecService fieldTypes;
    private final FieldUiControlService uiTypes;
    private final FieldUiControlPropertyService properties;
    private final FieldUiControlBindingService mappings;

    public PlatformFieldCatalogInitialDataDeclarationProvider(FieldSpecService fieldTypes,
                                                              FieldUiControlService uiTypes,
                                                              FieldUiControlPropertyService properties,
                                                              FieldUiControlBindingService mappings) {
        this.fieldTypes = fieldTypes;
        this.uiTypes = uiTypes;
        this.properties = properties;
        this.mappings = mappings;
    }

    @Override public String name() { return "platform.field-catalog"; }
    @Override public int order() { return 10; }

    @Override
    public List<InitialDataDeclaration<?>> declarations() {
        List<InitialDataDeclaration<?>> values = new ArrayList<>();
        // The catalog is bidirectionally linked. Create UI controls without their reverse default
        // first, then create FieldSpecs with their authoritative default UI control. Once both
        // ends exist, reconcile platform-owned UI semantic defaults for both fresh and historic
        // installations. Custom controls are not part of this catalog and remain untouched.
        FieldUiControlPresetCatalog.fieldUiControls().stream()
                .map(this::uiTypeWithoutDefaultFieldType)
                .forEach(value -> values.add(InitialDataDeclaration.createIfMissing(uiTypes, value)));
        FieldUiControlPresetCatalog.fieldTypes().forEach(value -> values.add(InitialDataDeclaration.createIfMissing(fieldTypes, value)));
        FieldUiControlPresetCatalog.fieldUiControls().forEach(
                value -> values.add(InitialDataDeclaration.reconcileManaged(uiTypes, value)));
        FieldUiControlPresetCatalog.properties().forEach(value -> values.add(InitialDataDeclaration.createIfMissing(properties, value)));
        FieldUiControlPresetCatalog.bindings().forEach(value -> values.add(InitialDataDeclaration.createIfMissing(mappings, value)));
        return List.copyOf(values);
    }

    private FieldUiControl uiTypeWithoutDefaultFieldType(FieldUiControl source) {
        FieldUiControl value = new FieldUiControl();
        value.setId(source.getId());
        value.setAlias(source.getAlias());
        value.setTitle(source.getTitle());
        value.setValueShape(source.getValueShape());
        value.setPrimaryValueKey(source.getPrimaryValueKey());
        value.setQueryMode(source.getQueryMode());
        value.setRendererType(source.getRendererType());
        value.setIcon(source.getIcon());
        value.setEnabled(source.getEnabled());
        value.setSortOrder(source.getSortOrder());
        return value;
    }
}
