package net.ximatai.muyun.spring.boot.bootstrap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldCatalogLegacySchemaBridgeTest {
    @Test
    void shouldBridgeRenamedFieldCatalogTablesColumnsAndRequiredSemanticBackfills() {
        assertThat(FieldCatalogLegacySchemaBridge.sqlStatements())
                .anyMatch(sql -> sql.contains("platform_field_type") && sql.contains("platform_field_spec"))
                .anyMatch(sql -> sql.contains("field_ui_type_alias") && sql.contains("field_ui_control_alias"))
                .anyMatch(sql -> sql.contains("value_shape varchar(16)"))
                .anyMatch(sql -> sql.contains("query_mode") && sql.contains("BETWEEN"))
                .anyMatch(sql -> sql.contains("value_field_spec_alias") && sql.contains("SET NOT NULL"));
    }
}
