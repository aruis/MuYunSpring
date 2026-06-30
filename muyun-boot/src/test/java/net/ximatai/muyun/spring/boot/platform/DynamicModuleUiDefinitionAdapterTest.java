package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigSnapshot;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedUiField;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiFixedPosition;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSet;
import net.ximatai.muyun.spring.platform.ui.PlatformUiSetType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicModuleUiDefinitionAdapterTest {
    @Test
    void shouldConvertPublishedDynamicSnapshotToModuleUiDefinition() {
        PlatformUiSet listSet = uiSet("set-list", "crm.customer", "customer_list", PlatformUiSetType.LIST);
        PlatformUiSet formSet = uiSet("set-form", "crm.customer", "customer_form", PlatformUiSetType.FORM);
        PlatformUiConfig listConfig = uiConfig("ui-list-web", "set-list", "客户列表", true, 10);
        PlatformUiConfig formConfig = uiConfig("ui-form-web", "set-form", "客户表单", true, 20);
        PlatformUiConfig draftConfig = uiConfig("ui-draft-web", "set-list", "草稿列表", false, 30);
        PlatformUiConfig appConfig = uiConfig("ui-list-app", "set-list", "客户列表 APP", true, 40);
        appConfig.setClientType(PlatformUiClientType.APP);
        PlatformPageConfigSnapshot snapshot = new PlatformPageConfigSnapshot(
                "crm.customer",
                List.of(listSet, formSet),
                List.of(listConfig, formConfig, draftConfig, appConfig),
                List.of(),
                List.of(),
                List.of()
        );
        PlatformResolvedPageConfig resolved = new PlatformResolvedPageConfig(
                List.of(
                        resolvedField("ui-list-web", "field-name", null, "name", "客户名称",
                                "text", true, false, null, 180, "left", PlatformUiFixedPosition.LEFT),
                        resolvedField("ui-list-web", "field-owner", "owner", "title", "负责人",
                                "reference", true, false, null, 160, null, null),
                        resolvedField("ui-form-web", "field-name", null, "name", "客户名称",
                                "input", true, false, true, null, null, null),
                        resolvedField("ui-draft-web", "field-draft", null, "draftOnly", "草稿字段",
                                "input", true, false, null, null, null, null)
                ),
                List.of()
        );

        ModuleUiDefinition definition = DynamicModuleUiDefinitionAdapter.fromPublishedSnapshot(snapshot, resolved);

        assertThat(definition.moduleAlias()).isEqualTo("crm.customer");
        assertThat(definition.views()).extracting(ViewDefinition::viewCode)
                .containsExactly("customer_list", "customer_form");
        ViewDefinition listView = definition.views().get(0);
        assertThat(listView.viewKind()).isEqualTo(ModuleViewKind.LIST);
        assertThat(listView.title()).isEqualTo("客户列表");
        assertThat(listView.fields()).extracting(field -> field.fieldRef().fieldId())
                .containsExactly("field-name", "field-owner");
        assertThat(listView.fields()).extracting(field -> field.fieldRef().fieldName())
                .containsExactly("name", "title");
        assertThat(listView.fields()).extracting(field -> field.fieldRef().relationCode())
                .containsExactly(null, "owner");
        assertThat(listView.fields().get(0).uiType()).isEqualTo("text");
        assertThat(listView.fields().get(0).width()).isEqualTo("180px");
        assertThat(listView.fields().get(0).align()).isEqualTo("left");
        assertThat(listView.fields().get(0).fixed()).isTrue();

        ViewDefinition formView = definition.views().get(1);
        assertThat(formView.viewKind()).isEqualTo(ModuleViewKind.FORM);
        assertThat(formView.fields()).hasSize(1);
        assertThat(formView.fields().get(0).required().constant()).isTrue();
        assertThat(formView.fields().get(0).readOnly().constant()).isFalse();
    }

    private PlatformUiSet uiSet(String id, String moduleAlias, String alias, PlatformUiSetType setType) {
        PlatformUiSet uiSet = new PlatformUiSet();
        uiSet.setId(id);
        uiSet.setModuleAlias(moduleAlias);
        uiSet.setAlias(alias);
        uiSet.setSetType(setType);
        uiSet.setTitle(alias);
        uiSet.setEnabled(Boolean.TRUE);
        return uiSet;
    }

    private PlatformUiConfig uiConfig(String id, String uiSetId, String title, boolean published, int sortOrder) {
        PlatformUiConfig config = new PlatformUiConfig();
        config.setId(id);
        config.setUiSetId(uiSetId);
        config.setTitle(title);
        config.setClientType(PlatformUiClientType.WEB);
        config.setPublished(published);
        config.setEnabled(Boolean.TRUE);
        config.setSortOrder(sortOrder);
        return config;
    }

    private PlatformResolvedUiField resolvedField(String uiConfigId,
                                                  String moduleMetadataFieldId,
                                                  String relationAlias,
                                                  String fieldName,
                                                  String fieldTitle,
                                                  String fieldUiTypeAlias,
                                                  Boolean visible,
                                                  Boolean readOnly,
                                                  Boolean required,
                                                  Integer width,
                                                  String align,
                                                  PlatformUiFixedPosition fixedPosition) {
        return new PlatformResolvedUiField(
                uiConfigId,
                moduleMetadataFieldId,
                relationAlias,
                "customer",
                fieldName,
                fieldName,
                fieldTitle,
                "string",
                "NORMAL",
                fieldUiTypeAlias,
                visible,
                readOnly,
                required,
                null,
                null,
                width,
                align,
                fixedPosition
        );
    }
}
