package net.ximatai.muyun.spring.starter.bootstrap;

import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

/** Upgrades the pre-FieldSpec local development schema before managed baseline data is reconciled. */
final class FieldCatalogLegacySchemaBridge implements PlatformBootstrapTask {
    private final Jdbi jdbi;

    FieldCatalogLegacySchemaBridge(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public String name() {
        return "platform.field-catalog-legacy-schema";
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public void run() {
        jdbi.useHandle(handle -> sqlStatements().forEach(handle::execute));
    }

    static List<String> sqlStatements() {
        return List.of(
                renameTable("platform_field_type", "platform_field_spec"),
                renameTable("platform_field_ui_type", "platform_field_ui_control"),
                renameTable("platform_field_ui_type_attribute", "platform_field_ui_control_attribute"),
                renameTable("platform_field_ui_type_field_mapping", "platform_field_ui_control_field_mapping"),
                renameColumn("platform_field_spec", "default_ui_type_alias", "default_ui_control_alias"),
                renameColumn("platform_field_spec", "ui_type_aliases", "ui_control_aliases"),
                renameColumn("platform_field_ui_control", "default_field_type_alias", "default_field_spec_alias"),
                renameColumn("platform_field_ui_control", "control_type", "renderer_type"),
                renameColumn("platform_field_ui_control_attribute", "field_ui_type_alias", "field_ui_control_alias"),
                renameColumn("platform_field_ui_control_attribute", "value_field_type_alias", "value_field_spec_alias"),
                renameColumn("platform_field_ui_control_field_mapping", "field_ui_type_alias", "field_ui_control_alias"),
                renameColumn("platform_field_ui_control_field_mapping", "source_key", "value_key"),
                renameColumn("platform_metadata_field", "field_type_alias", "field_spec_alias"),
                renameColumn("platform_metadata_view_field", "field_ui_type_alias", "field_ui_control_alias"),
                renameColumn("platform_ui_config_field", "field_ui_type_alias", "field_ui_control_alias"),
                addColumn("platform_field_ui_control", "value_shape varchar(16)"),
                addColumn("platform_field_ui_control", "primary_value_key varchar(64)"),
                addColumn("platform_field_ui_control", "query_mode varchar(16)"),
                addColumn("platform_field_ui_control_field_mapping", "value_field_spec_alias varchar(64)"),
                reconcileThenDropLegacyColumn("platform_metadata_field", "field_type_alias", "field_spec_alias"),
                "UPDATE platform_field_ui_control SET value_shape = CASE alias "
                        + "WHEN 'multi_select' THEN 'COLLECTION' WHEN 'date_range' THEN 'COMPOSITE' "
                        + "WHEN 'date_time_range' THEN 'COMPOSITE' WHEN 'date_time_with_time_zone' THEN 'COMPOSITE' "
                        + "ELSE 'SCALAR' END WHERE value_shape IS NULL",
                "UPDATE platform_field_ui_control SET primary_value_key = CASE alias "
                        + "WHEN 'date_range' THEN 'start' WHEN 'date_time_range' THEN 'start' "
                        + "WHEN 'date_time_with_time_zone' THEN 'dateTime' ELSE NULL END WHERE primary_value_key IS NULL",
                "UPDATE platform_field_ui_control SET query_mode = CASE alias WHEN 'date_range' THEN 'BETWEEN' "
                        + "WHEN 'date_time_range' THEN 'BETWEEN' ELSE 'DEFAULT' END WHERE query_mode IS NULL",
                "UPDATE platform_field_ui_control_field_mapping SET value_field_spec_alias = CASE field_ui_control_alias "
                        + "WHEN 'date_range' THEN 'date' WHEN 'date_time_range' THEN 'datetime' "
                        + "WHEN 'date_time_with_time_zone' THEN 'string' ELSE 'string' END "
                        + "WHERE value_field_spec_alias IS NULL",
                setNotNull("platform_field_ui_control", "value_shape"),
                setNotNull("platform_field_ui_control", "query_mode"),
                setNotNull("platform_field_ui_control_field_mapping", "value_field_spec_alias"));
    }

    private static String renameTable(String oldName, String newName) {
        return "DO $$ BEGIN IF to_regclass('public." + oldName + "') IS NOT NULL AND "
                + "to_regclass('public." + newName + "') IS NULL THEN ALTER TABLE " + oldName
                + " RENAME TO " + newName + "; END IF; END $$";
    }

    private static String renameColumn(String table, String oldName, String newName) {
        return "DO $$ BEGIN IF to_regclass('public." + table + "') IS NOT NULL AND EXISTS (SELECT 1 "
                + "FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + table
                + "' AND column_name = '" + oldName + "') AND NOT EXISTS (SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = '" + table + "' AND column_name = '" + newName
                + "') THEN ALTER TABLE " + table + " RENAME COLUMN " + oldName + " TO " + newName + "; END IF; END $$";
    }

    private static String addColumn(String table, String definition) {
        return "ALTER TABLE IF EXISTS " + table + " ADD COLUMN IF NOT EXISTS " + definition;
    }

    private static String reconcileThenDropLegacyColumn(String table, String oldName, String newName) {
        return "DO $$ BEGIN IF to_regclass('public." + table + "') IS NOT NULL AND EXISTS (SELECT 1 "
                + "FROM information_schema.columns WHERE table_schema = 'public' AND table_name = '" + table
                + "' AND column_name = '" + oldName + "') AND EXISTS (SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = '" + table + "' AND column_name = '" + newName
                + "') THEN UPDATE " + table + " SET " + newName + " = " + oldName + " WHERE " + newName
                + " IS NULL; ALTER TABLE " + table + " DROP COLUMN " + oldName + "; END IF; END $$";
    }

    private static String setNotNull(String table, String column) {
        return "ALTER TABLE IF EXISTS " + table + " ALTER COLUMN " + column + " SET NOT NULL";
    }
}
