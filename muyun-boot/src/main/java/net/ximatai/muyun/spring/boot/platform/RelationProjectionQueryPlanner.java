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

public final class RelationProjectionQueryPlanner {
    private RelationProjectionQueryPlanner() {
    }

    public static RelationProjectionSqlPlan plan(StaticModuleDefinition definition,
                                                 RecordReadProjection projection,
                                                 DBInfo.Type databaseType,
                                                 java.util.Set<String> requiredMainFields) {
        return plan(List.of(definition), definition, projection, databaseType, requiredMainFields);
    }

    public static RelationProjectionSqlPlan plan(List<StaticModuleDefinition> definitions,
                                                 StaticModuleDefinition definition,
                                                 RecordReadProjection projection,
                                                 DBInfo.Type databaseType,
                                                 java.util.Set<String> requiredMainFields) {
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
        if (definition.entities().isEmpty()) {
            return emptyPlan(definition, dbType);
        }
        java.util.Set<String> requiredFields = requiredMainFields == null ? java.util.Set.of()
                : java.util.Set.copyOf(requiredMainFields);

        EntityDefinition mainEntity = definition.entities().getFirst();
        LinkedHashSet<ViewFieldRef> relationFields = projection.outputFields().stream()
                .filter(field -> field.relationCode() != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<ViewFieldRef> readProjectionFields = projection.outputFields().stream()
                .filter(field -> field.relationCode() == null)
                .filter(field -> readProjection(definition, field.fieldName()) != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (relationFields.isEmpty() && readProjectionFields.isEmpty()) {
            return emptyPlan(definition, dbType);
        }
        LinkedHashSet<ViewFieldRef> referenceFields = new LinkedHashSet<>(relationFields);
        referenceFields.addAll(readProjectionFields);
        RelationProjectionSqlPlan referencePlan = referencePlan(
                definitions == null || definitions.isEmpty() ? List.of(definition) : definitions,
                definition,
                projection,
                referenceFields,
                dbType,
                requiredFields
        );
        if (referencePlan != null) {
            return referencePlan;
        }
        if (definition.projectionJoins().isEmpty()) {
            return emptyPlan(definition, dbType);
        }

        Map<String, EntityDefinition> entities = entitiesByAlias(definition.entities());
        Map<String, RelationProjectionJoinDefinition> joins = joinsByRelation(definition.projectionJoins());

        Map<String, FieldDefinition> mainFields = fieldsByName(mainEntity);
        LinkedHashMap<String, SelectField> selectFields = new LinkedHashMap<>();
        addMainProjectionFields(selectFields, mainFields, projection, requiredFields);
        addStandardMainFields(selectFields);
        LinkedHashSet<String> queryableFields = new LinkedHashSet<>(selectFields.keySet());
        LinkedHashSet<String> sortableFields = new LinkedHashSet<>(queryableFields);

        LinkedHashSet<String> responseFields = new LinkedHashSet<>();
        responseFields.add(StandardEntitySchema.ID_FIELD);
        projection.outputFields().stream()
                .map(ViewFieldRef::fieldName)
                .forEach(responseFields::add);
        LinkedHashSet<String> requiredRelations = new LinkedHashSet<>();
        for (ViewFieldRef field : relationFields) {
            EntityDefinition target = entities.get(field.relationCode());
            RelationProjectionJoinDefinition join = joins.get(field.relationCode());
            if (target == null || join == null) {
                throw new IllegalArgumentException("projection relation is not declared: "
                        + definition.moduleAlias() + "." + field.relationCode());
            }
            if (!join.cardinality().safeForPageJoin()) {
                throw new IllegalArgumentException("projection relation cardinality is not safe for page join: "
                        + definition.moduleAlias() + "." + field.relationCode() + "." + join.cardinality());
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
                .append(quote(RelationProjectionSqlNames.MAIN_ALIAS, dbType));
        for (String relationCode : requiredRelations) {
            appendJoin(sql, params, joins.get(relationCode), dbType);
        }
        return new RelationProjectionSqlPlan(
                sql.toString(),
                params,
                queryableFields,
                sortableFields,
                responseFields,
                List.copyOf(relationFields),
                dbType
        );
    }

    public static RelationProjectionSqlPlan plan(StaticModuleDefinition definition,
                                                 RecordReadProjection projection,
                                                 DBInfo.Type databaseType) {
        return plan(definition, projection, databaseType, java.util.Set.of());
    }

    private static RelationProjectionSqlPlan referencePlan(List<StaticModuleDefinition> definitions,
                                                           StaticModuleDefinition definition,
                                                           RecordReadProjection projection,
                                                           LinkedHashSet<ViewFieldRef> relationFields,
                                                           DBInfo.Type dbType,
                                                           java.util.Set<String> requiredFields) {
        Map<String, StaticModuleDefinition> modules = modulesByAlias(definitions);
        if (!modules.containsKey(definition.moduleAlias())) {
            LinkedHashMap<String, StaticModuleDefinition> merged = new LinkedHashMap<>(modules);
            merged.put(definition.moduleAlias(), definition);
            modules = java.util.Collections.unmodifiableMap(merged);
        }
        Map<String, FieldDefinition> mainFields = fieldsByName(definition.entities().getFirst());
        LinkedHashMap<String, SelectField> selectFields = new LinkedHashMap<>();
        addMainProjectionFields(selectFields, mainFields, projection, requiredFields);
        addStandardMainFields(selectFields);
        LinkedHashSet<String> queryableFields = new LinkedHashSet<>(selectFields.keySet());
        LinkedHashSet<String> sortableFields = new LinkedHashSet<>(queryableFields);

        LinkedHashSet<String> responseFields = new LinkedHashSet<>();
        responseFields.add(StandardEntitySchema.ID_FIELD);
        projection.outputFields().stream()
                .map(ViewFieldRef::fieldName)
                .forEach(responseFields::add);

        LinkedHashMap<String, StaticModuleReferencePathResolver.JoinStep> joins = new LinkedHashMap<>();
        for (ViewFieldRef field : relationFields) {
            StaticModuleReadProjectionDefinition readProjection = field.relationCode() == null
                    ? readProjection(definition, field.fieldName())
                    : null;
            String path = readProjection == null
                    ? field.relationCode() + "." + field.fieldName()
                    : readProjection.path();
            int lastSeparator = path.lastIndexOf('.');
            if (lastSeparator < 0) {
                if (readProjection != null) {
                    throw new IllegalArgumentException("projection reference path is invalid: "
                            + definition.moduleAlias() + "." + readProjection.outputField() + "." + path);
                }
                return null;
            }
            String relationPath = path.substring(0, lastSeparator);
            String targetFieldName = path.substring(lastSeparator + 1);
            StaticModuleReferencePathResolver.Traversal traversal =
                    StaticModuleReferencePathResolver.resolve(modules, definition, relationPath);
            if (traversal == null) {
                if (readProjection != null) {
                    throw new IllegalArgumentException("projection reference path is not declared: "
                            + definition.moduleAlias() + "." + readProjection.outputField() + "." + relationPath);
                }
                return null;
            }
            for (StaticModuleReferencePathResolver.JoinStep join : traversal.joins()) {
                joins.putIfAbsent(join.tableAlias(), join);
            }
            addSelectField(selectFields, new SelectField(
                    traversal.tableAlias(),
                    columnName(traversal.entity(), targetFieldName),
                    field.fieldName()
            ));
            if (readProjection != null) {
                if (readProjection.filterable()) {
                    queryableFields.add(field.fieldName());
                }
                if (readProjection.sortable()) {
                    sortableFields.add(field.fieldName());
                }
            }
        }
        if (joins.isEmpty()) {
            return null;
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder();
        sql.append("select ");
        sql.append(selectFields.values().stream()
                .map(field -> qualified(field.tableAlias(), field.columnName(), dbType)
                        + " as " + quote(field.outputName(), dbType))
                .collect(java.util.stream.Collectors.joining(", ")));
        sql.append(" from ")
                .append(qualifiedTable(definition.entities().getFirst(), dbType))
                .append(" ")
                .append(quote(RelationProjectionSqlNames.MAIN_ALIAS, dbType));
        for (StaticModuleReferencePathResolver.JoinStep join : joins.values()) {
            appendReferenceJoin(sql, params, join, dbType);
        }
        return new RelationProjectionSqlPlan(
                sql.toString(),
                params,
                queryableFields,
                sortableFields,
                responseFields,
                List.copyOf(relationFields),
                dbType
        );
    }

    private static void appendReferenceJoin(StringBuilder sql,
                                            Map<String, Object> params,
                                            StaticModuleReferencePathResolver.JoinStep join,
                                            DBInfo.Type databaseType) {
        sql.append(" left join ")
                .append(qualifiedTable(join.entity(), databaseType))
                .append(" ")
                .append(quote(join.tableAlias(), databaseType))
                .append(" on ");
        List<String> predicates = new ArrayList<>();
        for (RelationProjectionJoinCondition condition : join.conditions()) {
            predicates.add(qualified(condition.leftAlias(), condition.leftColumn(), databaseType)
                    + " = "
                    + qualified(condition.rightAlias(), condition.rightColumn(), databaseType));
        }
        String paramName = "__join_" + join.tableAlias() + "_deleted";
        predicates.add(qualified(join.tableAlias(), StandardEntitySchema.DELETED_COLUMN, databaseType) + " = :" + paramName);
        params.put(paramName, Boolean.FALSE);
        sql.append(String.join(" and ", predicates));
    }

    private static RelationProjectionSqlPlan emptyPlan(StaticModuleDefinition definition, DBInfo.Type databaseType) {
        return new RelationProjectionSqlPlan("select * from " + qualifiedTable(definition.entities().getFirst(), databaseType),
                Map.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), List.of(), databaseType);
    }

    private static void addMainProjectionFields(LinkedHashMap<String, SelectField> selectFields,
                                                Map<String, FieldDefinition> mainFields,
                                                RecordReadProjection projection,
                                                java.util.Set<String> requiredMainFields) {
        LinkedHashSet<String> fieldNames = new LinkedHashSet<>(projection.internalReadFields());
        projection.outputFields().stream()
                .filter(field -> field.relationCode() == null)
                .map(ViewFieldRef::fieldName)
                .forEach(fieldNames::add);
        fieldNames.addAll(requiredMainFields);
        for (String fieldName : fieldNames) {
            FieldDefinition field = mainFields.get(fieldName);
            if (field == null) {
                continue;
            }
            addSelectField(selectFields, new SelectField(
                    RelationProjectionSqlNames.MAIN_ALIAS,
                    field.columnName(),
                    field.fieldName()
            ));
        }
    }

    private static void addStandardMainFields(LinkedHashMap<String, SelectField> selectFields) {
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.ID_COLUMN, StandardEntitySchema.ID_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.TENANT_ID_COLUMN, StandardEntitySchema.TENANT_ID_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.VERSION_COLUMN, StandardEntitySchema.VERSION_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.DELETED_COLUMN, StandardEntitySchema.DELETED_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.DELETED_AT_COLUMN, StandardEntitySchema.DELETED_AT_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.CREATED_BY_COLUMN, StandardEntitySchema.CREATED_BY_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.CREATED_AT_COLUMN, StandardEntitySchema.CREATED_AT_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.UPDATED_BY_COLUMN, StandardEntitySchema.UPDATED_BY_FIELD));
        addSelectField(selectFields, new SelectField(RelationProjectionSqlNames.MAIN_ALIAS,
                StandardEntitySchema.UPDATED_AT_COLUMN, StandardEntitySchema.UPDATED_AT_FIELD));
    }

    private static void addSelectField(LinkedHashMap<String, SelectField> selectFields, SelectField field) {
        PlatformNameRules.requireFieldName(field.outputName(), "projectionOutputField");
        RelationProjectionSqlNames.requireColumn(field.columnName(), "columnName");
        SelectField existing = selectFields.putIfAbsent(field.outputName(), field);
        if (existing != null && (!existing.tableAlias().equals(field.tableAlias())
                || !existing.columnName().equals(field.columnName()))) {
            throw new IllegalArgumentException("projection output field conflicts: " + field.outputName());
        }
    }

    private static void appendJoin(StringBuilder sql,
                                   Map<String, Object> params,
                                   RelationProjectionJoinDefinition join,
                                   DBInfo.Type databaseType) {
        for (RelationProjectionJoinStep step : join.steps()) {
            int filterIndex = 0;
            sql.append(" left join ")
                    .append(qualifiedTable(step.schemaName(), step.tableName(), databaseType))
                    .append(" ")
                    .append(quote(step.tableAlias(), databaseType))
                    .append(" on ");
            List<String> predicates = new ArrayList<>();
            for (RelationProjectionJoinCondition condition : step.conditions()) {
                predicates.add(qualified(condition.leftAlias(), condition.leftColumn(), databaseType)
                        + " = "
                        + qualified(condition.rightAlias(), condition.rightColumn(), databaseType));
            }
            for (RelationProjectionJoinFilter filter : step.filters()) {
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

    private static Map<String, FieldDefinition> fieldsByName(EntityDefinition entity) {
        LinkedHashMap<String, FieldDefinition> byName = new LinkedHashMap<>();
        for (FieldDefinition field : entity.fields()) {
            byName.putIfAbsent(field.fieldName(), field);
        }
        return java.util.Collections.unmodifiableMap(byName);
    }

    private static Map<String, RelationProjectionJoinDefinition> joinsByRelation(List<RelationProjectionJoinDefinition> joins) {
        LinkedHashMap<String, RelationProjectionJoinDefinition> byRelation = new LinkedHashMap<>();
        for (RelationProjectionJoinDefinition join : joins) {
            if (byRelation.putIfAbsent(join.relationCode(), join) != null) {
                throw new IllegalArgumentException("duplicate projection relation: " + join.relationCode());
            }
        }
        return java.util.Collections.unmodifiableMap(byRelation);
    }

    private static Map<String, StaticModuleDefinition> modulesByAlias(List<StaticModuleDefinition> definitions) {
        LinkedHashMap<String, StaticModuleDefinition> byAlias = new LinkedHashMap<>();
        for (StaticModuleDefinition definition : definitions) {
            if (definition != null) {
                byAlias.putIfAbsent(definition.moduleAlias(), definition);
            }
        }
        return java.util.Collections.unmodifiableMap(byAlias);
    }

    private static StaticModuleReadProjectionDefinition readProjection(StaticModuleDefinition definition,
                                                                       String outputField) {
        if (definition == null || outputField == null || outputField.isBlank()) {
            return null;
        }
        return definition.readProjections().stream()
                .filter(projection -> projection.outputField().equals(outputField))
                .findFirst()
                .orElse(null);
    }

    static String columnName(EntityDefinition entity, String fieldName) {
        return switch (fieldName) {
            case StandardEntitySchema.ID_FIELD -> StandardEntitySchema.ID_COLUMN;
            case StandardEntitySchema.TENANT_ID_FIELD -> StandardEntitySchema.TENANT_ID_COLUMN;
            case StandardEntitySchema.VERSION_FIELD -> StandardEntitySchema.VERSION_COLUMN;
            case StandardEntitySchema.DELETED_FIELD -> StandardEntitySchema.DELETED_COLUMN;
            case StandardEntitySchema.DELETED_AT_FIELD -> StandardEntitySchema.DELETED_AT_COLUMN;
            case StandardEntitySchema.CREATED_BY_FIELD -> StandardEntitySchema.CREATED_BY_COLUMN;
            case StandardEntitySchema.CREATED_AT_FIELD -> StandardEntitySchema.CREATED_AT_COLUMN;
            case StandardEntitySchema.UPDATED_BY_FIELD -> StandardEntitySchema.UPDATED_BY_COLUMN;
            case StandardEntitySchema.UPDATED_AT_FIELD -> StandardEntitySchema.UPDATED_AT_COLUMN;
            case net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.TITLE_FIELD ->
                    net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.TITLE_COLUMN;
            case net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.ENABLED_FIELD ->
                    net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.ENABLED_COLUMN;
            case net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.SORT_FIELD ->
                    net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.SORT_COLUMN;
            case net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.TREE_PARENT_FIELD ->
                    net.ximatai.muyun.spring.common.schema.PlatformAbilityFields.TREE_PARENT_COLUMN;
            default -> entity.fields().stream()
                    .filter(field -> field.fieldName().equals(fieldName))
                    .map(FieldDefinition::columnName)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("projection reference field is not declared: "
                            + entity.alias() + "." + fieldName));
        };
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
