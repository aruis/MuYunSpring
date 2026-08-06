-- One-time PostgreSQL migration for schemas created before FieldSpec/FieldUiControl.
-- Run this while the application is stopped, then start a version without the legacy bridge.
-- The script is idempotent for the supported public-schema upgrade path.

SET search_path TO public;

DO $$ BEGIN
  IF to_regclass('public.platform_field_type') IS NOT NULL
      AND to_regclass('public.platform_field_spec') IS NULL THEN
    ALTER TABLE platform_field_type RENAME TO platform_field_spec;
  END IF;
  IF to_regclass('public.platform_field_ui_type') IS NOT NULL
      AND to_regclass('public.platform_field_ui_control') IS NULL THEN
    ALTER TABLE platform_field_ui_type RENAME TO platform_field_ui_control;
  END IF;
  IF to_regclass('public.platform_field_ui_type_attribute') IS NOT NULL
      AND to_regclass('public.platform_field_ui_control_attribute') IS NULL THEN
    ALTER TABLE platform_field_ui_type_attribute RENAME TO platform_field_ui_control_attribute;
  END IF;
  IF to_regclass('public.platform_field_ui_type_field_mapping') IS NOT NULL
      AND to_regclass('public.platform_field_ui_control_field_mapping') IS NULL THEN
    ALTER TABLE platform_field_ui_type_field_mapping RENAME TO platform_field_ui_control_field_mapping;
  END IF;
END $$;

DO $$
DECLARE
  item record;
BEGIN
  FOR item IN
    SELECT * FROM (VALUES
      ('platform_field_spec', 'default_ui_type_alias', 'default_ui_control_alias'),
      ('platform_field_spec', 'ui_type_aliases', 'ui_control_aliases'),
      ('platform_field_ui_control', 'default_field_type_alias', 'default_field_spec_alias'),
      ('platform_field_ui_control', 'control_type', 'renderer_type'),
      ('platform_field_ui_control_attribute', 'field_ui_type_alias', 'field_ui_control_alias'),
      ('platform_field_ui_control_attribute', 'value_field_type_alias', 'value_field_spec_alias'),
      ('platform_field_ui_control_field_mapping', 'field_ui_type_alias', 'field_ui_control_alias'),
      ('platform_field_ui_control_field_mapping', 'source_key', 'value_key'),
      ('platform_metadata_field', 'field_type_alias', 'field_spec_alias'),
      ('platform_metadata_view_field', 'field_ui_type_alias', 'field_ui_control_alias'),
      ('platform_ui_config_field', 'field_ui_type_alias', 'field_ui_control_alias')
    ) AS renamed(table_name, old_column, new_column)
  LOOP
    IF to_regclass('public.' || item.table_name) IS NOT NULL
        AND EXISTS (SELECT 1 FROM information_schema.columns
                    WHERE table_schema = 'public' AND table_name = item.table_name AND column_name = item.old_column)
        AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = item.table_name AND column_name = item.new_column) THEN
      EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', item.table_name, item.old_column, item.new_column);
    END IF;
  END LOOP;
END $$;

ALTER TABLE IF EXISTS platform_field_ui_control ADD COLUMN IF NOT EXISTS value_shape varchar(16);
ALTER TABLE IF EXISTS platform_field_ui_control ADD COLUMN IF NOT EXISTS primary_value_key varchar(64);
ALTER TABLE IF EXISTS platform_field_ui_control ADD COLUMN IF NOT EXISTS query_mode varchar(16);
ALTER TABLE IF EXISTS platform_field_ui_control_field_mapping ADD COLUMN IF NOT EXISTS value_field_spec_alias varchar(64);

DO $$ BEGIN
  IF to_regclass('public.platform_metadata_field') IS NOT NULL
      AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public'
                  AND table_name = 'platform_metadata_field' AND column_name = 'field_type_alias')
      AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public'
                  AND table_name = 'platform_metadata_field' AND column_name = 'field_spec_alias') THEN
    UPDATE platform_metadata_field SET field_spec_alias = field_type_alias WHERE field_spec_alias IS NULL;
    ALTER TABLE platform_metadata_field DROP COLUMN field_type_alias;
  END IF;
END $$;

UPDATE platform_field_ui_control
SET value_shape = CASE alias
  WHEN 'multi_select' THEN 'COLLECTION'
  WHEN 'date_range' THEN 'COMPOSITE'
  WHEN 'date_time_range' THEN 'COMPOSITE'
  WHEN 'date_time_with_time_zone' THEN 'COMPOSITE'
  ELSE 'SCALAR'
END
WHERE value_shape IS NULL;

UPDATE platform_field_ui_control
SET primary_value_key = CASE alias
  WHEN 'date_range' THEN 'start'
  WHEN 'date_time_range' THEN 'start'
  WHEN 'date_time_with_time_zone' THEN 'dateTime'
  ELSE NULL
END
WHERE primary_value_key IS NULL;

UPDATE platform_field_ui_control
SET query_mode = CASE alias
  WHEN 'date_range' THEN 'BETWEEN'
  WHEN 'date_time_range' THEN 'BETWEEN'
  ELSE 'DEFAULT'
END
WHERE query_mode IS NULL;

UPDATE platform_field_ui_control_field_mapping
SET value_field_spec_alias = CASE field_ui_control_alias
  WHEN 'date_range' THEN 'date'
  WHEN 'date_time_range' THEN 'datetime'
  WHEN 'date_time_with_time_zone' THEN 'string'
  ELSE 'string'
END
WHERE value_field_spec_alias IS NULL;

ALTER TABLE IF EXISTS platform_field_ui_control ALTER COLUMN value_shape SET NOT NULL;
ALTER TABLE IF EXISTS platform_field_ui_control ALTER COLUMN query_mode SET NOT NULL;
ALTER TABLE IF EXISTS platform_field_ui_control_field_mapping ALTER COLUMN value_field_spec_alias SET NOT NULL;
