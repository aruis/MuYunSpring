package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class StaticProjectionQueryPlanner {
    private StaticProjectionQueryPlanner() {
    }

    public static StaticProjectionSqlPlan plan(StaticModuleDefinition definition,
                                               RecordReadProjection projection,
                                               DBInfo.Type databaseType) {
        if (definition == null) {
            throw new IllegalArgumentException("static module definition must not be null");
        }
        if (projection == null) {
            throw new IllegalArgumentException("record read projection must not be null");
        }
        if (!definition.moduleAlias().equals(projection.moduleAlias())) {
            throw new IllegalArgumentException("projection module alias mismatch: "
                    + definition.moduleAlias() + " != " + projection.moduleAlias());
        }
        DBInfo.Type dbType = databaseType == null ? DBInfo.Type.POSTGRESQL : databaseType;
        if (definition.entities().isEmpty() || definition.projectionJoins().isEmpty()) {
            return emptyPlan(definition, dbType);
        }

        EntityDefinition mainEntity = definition.entities().getFirst();
        Map<String, EntityDefinition> entities = entitiesByAlias(definition.entities());
        Map<String, StaticProjectionJoinDefinition> joins = joinsByRelation(definition.projectionJoins());
        LinkedHashSet<ViewFieldRef> relationFields = projection.outputFields().stream()
                .filter(field -> field.relationCode() != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (relationFields.isEmpty()) {
            return emptyPlan(definition, dbType);
        }

        LinkedHashMap<String, SelectField> selectFields = new LinkedHashMap<>();
        for (FieldDefinition field : mainEntity.fields()) {
            addSelectField(selectFields, new SelectField(
                    StaticProjectionSqlNames.MAIN_ALIAS,
                    field.columnName(),
                    field.fieldName()
            ));
        }
        addStandardMainFields(selectFields);

        LinkedHashSet<String> requiredRelations = new LinkedHashSet<>();
        for (ViewFieldRef field : relationFields) {
            EntityDefinition target = entities.get(field.relationCode());
            StaticProjectionJoinDefinition join = joins.get(field.relationCode());
            if (target == null || join == null) {
                throw new IllegalArgumentException("projection relation is not declared: "
                        + definition.moduleAlias() + "." + field.relationCode());
            }
            FieldDefinition targetField = target.fields().stream()
                    .filter(item -> item.fieldName().equals(field.fieldName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("projection relation field is not declared: "
                            + definition.moduleAlias() + "." + field.relationCode() + "." + field.fieldName()));
            addSelectField(selectFields, new SelectField(
                    field.relationCode(),
                    targetField.columnName(),
                    targetField.fieldName()
            ));
            requiredRelations.add(field.relationCode());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append(selectFields.values().stream()
                .map(field -> qualified(field.tableAlias(), field.columnName(), dbType)
                        + " as " + quote(field.outputName(), dbType))
                .collect(java.util.stream.Collectors.joining(", ")));
        sql.append(" from ")
                .append(qualifiedTable(mainEntity, dbType))
                .append(" ")
                .append(quote(StaticProjectionSqlNames.MAIN_ALIAS, dbType));
        for (String relationCode : requiredRelations) {
            appendJoin(sql, params, joins.get(relationCode), dbType);
        }
        return new StaticProjectionSqlPlan(
                sql.toString(),
                params,
                selectFields.keySet(),
                List.copyOf(relationFields)
        );
    }

    private static StaticProjectionSqlPlan emptyPlan(StaticModuleDefinition definition, DBInfo.Type databaseType) {
        return new StaticProjectionSqlPlan("select * from " + qualifiedTable(definition.entities().getFirst(), databaseType),
                Map.of(), java.util.Set.of(), List.of());
    }

    private static void addStandardMainFields(LinkedHashMap<String, SelectField> selectFields) {
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.ID_COLUMN, StandardEntitySchema.ID_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.TENANT_ID_COLUMN, StandardEntitySchema.TENANT_ID_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.VERSION_COLUMN, StandardEntitySchema.VERSION_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.DELETED_COLUMN, StandardEntitySchema.DELETED_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.DELETED_AT_COLUMN, StandardEntitySchema.DELETED_AT_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.CREATED_BY_COLUMN, StandardEntitySchema.CREATED_BY_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.CREATED_AT_COLUMN, StandardEntitySchema.CREATED_AT_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.UPDATED_BY_COLUMN, StandardEntitySchema.UPDATED_BY_FIELD));
        addSelectField(selectFields, new SelectField(StaticProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.UPDATED_AT_COLUMN, StandardEntitySchema.UPDATED_AT_FIELD));
    }

    private static void addSelectField(LinkedHashMap<String, SelectField> selectFields, SelectField field) {
        PlatformNameRules.requireFieldName(field.outputName(), "projectionOutputField");
        StaticProjectionSqlNames.requireColumn(field.columnName(), "columnName");
        SelectField existing = selectFields.putIfAbsent(field.outputName(), field);
        if (existing != null && (!existing.tableAlias().equals(field.tableAlias())
                || !existing.columnName().equals(field.columnName()))) {
            throw new IllegalArgumentException("projection output field conflicts: " + field.outputName());
        }
    }

    private static void appendJoin(StringBuilder sql,
                                   Map<String, Object> params,
                                   StaticProjectionJoinDefinition join,
                                   DBInfo.Type databaseType) {
        for (StaticProjectionJoinStep step : join.steps()) {
            int filterIndex = 0;
            sql.append(" left join ")
                    .append(qualifiedTable(step.schemaName(), step.tableName(), databaseType))
                    .append(" ")
                    .append(quote(step.tableAlias(), databaseType))
                    .append(" on ");
            List<String> predicates = new ArrayList<>();
            for (StaticProjectionJoinCondition condition : step.conditions()) {
                predicates.add(qualified(condition.leftAlias(), condition.leftColumn(), databaseType)
                        + " = "
                        + qualified(condition.rightAlias(), condition.rightColumn(), databaseType));
            }
            for (StaticProjectionJoinFilter filter : step.filters()) {
                String paramName = "__join_" + join.relationCode() + "_" + step.tableAlias() + "_" + filterIndex++;
                predicates.add(qualified(filter.tableAlias(), filter.columnName(), databaseType) + " = :" + paramName);
                params.put(paramName, filter.value());
            }
            sql.append(String.join(" and ", predicates));
        }
    }

    private static Map<String, EntityDefinition> entitiesByAlias(List<EntityDefinition> entities) {
        LinkedHashMap<String, EntityDefinition> byAlias = new LinkedHashMap<>();
        for (EntityDefinition entity : entities) {
            byAlias.putIfAbsent(entity.alias(), entity);
        }
        return java.util.Collections.unmodifiableMap(byAlias);
    }

    private static Map<String, StaticProjectionJoinDefinition> joinsByRelation(List<StaticProjectionJoinDefinition> joins) {
        LinkedHashMap<String, StaticProjectionJoinDefinition> byRelation = new LinkedHashMap<>();
        for (StaticProjectionJoinDefinition join : joins) {
            if (byRelation.putIfAbsent(join.relationCode(), join) != null) {
                throw new IllegalArgumentException("duplicate projection relation: " + join.relationCode());
            }
        }
        return java.util.Collections.unmodifiableMap(byRelation);
    }

    private static String qualifiedTable(EntityDefinition entity, DBInfo.Type databaseType) {
        return qualifiedTable(entity.schemaName(), entity.tableName(), databaseType);
    }

    private static String qualifiedTable(String schemaName, String tableName, DBInfo.Type databaseType) {
        String schema = schemaName == null || schemaName.isBlank() ? EntityDefinition.DEFAULT_SCHEMA_NAME : schemaName;
        return quote(schema, databaseType) + "." + quote(tableName, databaseType);
    }

    static String qualified(String tableAlias, String columnName, DBInfo.Type databaseType) {
        return quote(tableAlias, databaseType) + "." + quote(columnName, databaseType);
    }

    static String quote(String identifier, DBInfo.Type databaseType) {
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("invalid SQL identifier: " + identifier);
        }
        if (databaseType == DBInfo.Type.MYSQL) {
            return "`" + identifier + "`";
        }
        return "\"" + identifier + "\"";
    }

    private record SelectField(String tableAlias, String columnName, String outputName) {
    }
}
