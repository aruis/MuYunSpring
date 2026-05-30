package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModuleDefinitionValidator {
    private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]{0,62}";
    private static final Set<String> STANDARD_COLUMNS = Set.copyOf(StandardEntitySchema.columnNames());
    private static final Set<String> STANDARD_FIELDS = Set.copyOf(StandardEntitySchema.fieldNames());

    public void validate(ModuleDefinition module) {
        if (module == null) {
            throw new ModuleDefinitionException("module must not be null");
        }
        requireModuleAlias(module.moduleAlias(), "module alias");
        requireText(module.name(), "module name");
        Set<String> entityCodes = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (EntityDefinition entity : module.entities()) {
            validateEntity(entity);
            requireUnique(entityCodes, entity.code(), "entity code");
            requireUnique(tableNames, physicalTableKey(entity), "table name");
        }
        Map<String, EntityDefinition> entities = module.entities().stream()
                .collect(Collectors.toMap(EntityDefinition::code, Function.identity()));
        Set<String> relationCodes = new HashSet<>();
        for (EntityRelationDefinition relation : module.relations()) {
            validateRelation(relation, entities);
            requireUnique(relationCodes, relation.parentEntity() + "." + relation.code(), "relation code");
        }
        for (EntityReferenceDefinition reference : module.references()) {
            validateReference(reference, entities, module.moduleAlias());
        }
        Set<String> actionKeys = new HashSet<>();
        for (EntityActionDefinition action : module.actions()) {
            validateAction(action, entities);
            requireUnique(actionKeys, action.entityCode() + "." + action.actionCode(), "action");
        }
        Set<String> viewKeys = new HashSet<>();
        for (EntityViewDefinition view : module.views()) {
            validateView(view, entities);
            requireUnique(viewKeys, view.entityCode() + "." + view.viewType(), "view");
        }
    }

    public void validateEntity(EntityDefinition entity) {
        if (entity == null) {
            throw new ModuleDefinitionException("entity must not be null");
        }
        requireIdentifier(entity.code(), "entity code");
        if (entity.schemaName() != null && !entity.schemaName().isBlank()) {
            requireIdentifier(entity.schemaName(), "schema name");
        }
        requireIdentifier(entity.tableName(), "table name");
        requireText(entity.name(), "entity name");
        Set<String> fieldCodes = new HashSet<>();
        Set<String> columnNames = new HashSet<>();
        int sortableFields = 0;
        int titleFields = 0;
        FieldDefinition sortableField = null;
        FieldDefinition titleField = null;
        FieldDefinition treeParentField = null;
        FieldDefinition enabledField = null;
        for (FieldDefinition field : entity.fields()) {
            validateField(field);
            requireUnique(fieldCodes, field.code(), "field code");
            requireUnique(columnNames, field.columnName(), "column name");
            if (PlatformAbilityFields.TREE_PARENT_FIELD.equals(field.fieldName())) {
                treeParentField = field;
            }
            if (PlatformAbilityFields.ENABLED_FIELD.equals(field.fieldName())
                    || PlatformAbilityFields.ENABLED_COLUMN.equals(field.columnName())) {
                enabledField = field;
            }
            if (field.isSortable()) {
                sortableFields++;
                sortableField = field;
            }
            if (field.isTitle()) {
                titleFields++;
                titleField = field;
            }
        }
        if (!entity.supports(EntityCapability.CRUD)) {
            throw new ModuleDefinitionException("dynamic entity requires CRUD capability: " + entity.code());
        }
        if (sortableFields > 1) {
            throw new ModuleDefinitionException("entity can only have one sortable field: " + entity.code());
        }
        if (titleFields > 1) {
            throw new ModuleDefinitionException("entity can only have one title field: " + entity.code());
        }
        if (sortableFields > 0 && !entity.supports(EntityCapability.SORT)) {
            throw new ModuleDefinitionException("sortable field requires SORT capability: " + entity.code());
        }
        if (entity.supports(EntityCapability.TREE)) {
            requireTreeParentField(entity, treeParentField);
        }
        if (entity.supports(EntityCapability.SORT)) {
            requireSortField(entity, sortableField);
        }
        if (titleFields > 0 && !entity.supports(EntityCapability.REFERENCE)) {
            throw new ModuleDefinitionException("title field requires REFERENCE capability: " + entity.code());
        }
        if (enabledField != null && !entity.supports(EntityCapability.ENABLE)) {
            throw new ModuleDefinitionException("enabled field requires ENABLE capability: " + entity.code());
        }
        if (entity.supports(EntityCapability.REFERENCE)) {
            requireTitleField(entity, titleField);
        }
        if (entity.supports(EntityCapability.ENABLE)) {
            requireEnabledField(entity, enabledField);
        }
    }

    public void validateField(FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("field must not be null");
        }
        requireFieldName(field.fieldName(), "field name");
        if (STANDARD_FIELDS.contains(field.fieldName())) {
            throw new ModuleDefinitionException("field name conflicts with standard field: " + field.fieldName());
        }
        requireIdentifier(field.columnName(), "column name");
        requireText(field.name(), "field title");
        if (field.type() == null) {
            throw new ModuleDefinitionException("field type must not be null: " + field.code());
        }
        if (field.dictionaryBinding() != null
                && field.type() != FieldType.STRING
                && field.type() != FieldType.TEXT) {
            throw new ModuleDefinitionException("dictionary binding requires string field: " + field.code());
        }
        if (STANDARD_COLUMNS.contains(field.columnName())) {
            throw new ModuleDefinitionException("field column conflicts with standard column: " + field.columnName());
        }
        if (field.length() != null && field.length() <= 0) {
            throw new ModuleDefinitionException("field length must be positive: " + field.code());
        }
        if (field.length() != null && field.type() != FieldType.STRING && field.type() != FieldType.TEXT) {
            throw new ModuleDefinitionException("field length only applies to string fields: " + field.code());
        }
        if (field.precision() != null && field.precision() <= 0) {
            throw new ModuleDefinitionException("field precision must be positive: " + field.code());
        }
        if (field.scale() != null && field.scale() < 0) {
            throw new ModuleDefinitionException("field scale must not be negative: " + field.code());
        }
        if ((field.precision() != null || field.scale() != null) && field.type() != FieldType.DECIMAL) {
            throw new ModuleDefinitionException("field precision and scale only apply to decimal fields: " + field.code());
        }
        if (field.scale() != null && field.precision() == null) {
            throw new ModuleDefinitionException("field scale requires precision: " + field.code());
        }
        if (field.scale() != null && field.scale() > field.precision()) {
            throw new ModuleDefinitionException("field scale must not exceed precision: " + field.code());
        }
        if (field.isSortable() && field.type() != FieldType.INTEGER && field.type() != FieldType.LONG) {
            throw new ModuleDefinitionException("sortable field must be an integer type: " + field.code());
        }
        if (field.isTitle() && field.type() != FieldType.STRING && field.type() != FieldType.TEXT) {
            throw new ModuleDefinitionException("title field must be a text type: " + field.code());
        }
    }

    public void validateRelation(EntityRelationDefinition relation, Map<String, EntityDefinition> entities) {
        if (relation == null) {
            throw new ModuleDefinitionException("relation must not be null");
        }
        requireIdentifier(relation.code(), "relation code");
        EntityDefinition parent = requireEntity(entities, relation.parentEntity(), "relation parent entity");
        EntityDefinition child = requireEntity(entities, relation.childEntity(), "relation child entity");
        requireFieldName(relation.childForeignKeyField(), "relation child foreign key field");
        if (parent.code().equals(child.code())) {
            throw new ModuleDefinitionException("child relation must use different parent and child entities: " + relation.code());
        }
        FieldDefinition childForeignKeyField = requireField(child, relation.childForeignKeyField(), "relation child foreign key field");
        if (childForeignKeyField.type() != FieldType.STRING) {
            throw new ModuleDefinitionException("relation child foreign key field must be STRING: "
                    + child.code() + "." + relation.childForeignKeyField());
        }
    }

    public void validateReference(EntityReferenceDefinition reference, Map<String, EntityDefinition> entities) {
        validateReference(reference, entities, null);
    }

    public void validateView(EntityViewDefinition view, Map<String, EntityDefinition> entities) {
        if (view == null) {
            throw new ModuleDefinitionException("view must not be null");
        }
        EntityDefinition entity = requireEntity(entities, view.entityCode(), "view entity");
        if (view.viewType() == null) {
            throw new ModuleDefinitionException("view type must not be null: " + view.entityCode());
        }
        requireText(view.title(), "view title");
        Set<String> fieldNames = new HashSet<>();
        for (EntityViewFieldDefinition field : view.fields()) {
            if (field == null) {
                throw new ModuleDefinitionException("view field must not be null: " + view.entityCode());
            }
            requireFieldName(field.fieldName(), "view field name");
            requireField(entity, field.fieldName(), "view field");
            requireUnique(fieldNames, field.fieldName(), "view field");
            if (field.title() != null && field.title().isBlank()) {
                throw new ModuleDefinitionException("view field title must not be blank: "
                        + view.entityCode() + "." + field.fieldName());
            }
        }
    }

    public void validateAction(EntityActionDefinition action, Map<String, EntityDefinition> entities) {
        if (action == null) {
            throw new ModuleDefinitionException("action must not be null");
        }
        EntityDefinition entity = requireEntity(entities, action.entityCode(), "action entity");
        requireActionCode(action.actionCode(), "action code");
        requireText(action.title(), "action title");
        if (action.kind() == null) {
            throw new ModuleDefinitionException("action kind must not be null: " + action.actionCode());
        }
        if (action.level() == null) {
            throw new ModuleDefinitionException("action level must not be null: " + action.actionCode());
        }
        if (action.permissionCode() != null && action.permissionCode().isBlank()) {
            throw new ModuleDefinitionException("action permission code must not be blank: " + action.actionCode());
        }
        EntityActionKind standardKind = EntityStandardActionCatalog.standardKind(entity, action.actionCode());
        if (standardKind == null && action.kind() != EntityActionKind.CUSTOM) {
            throw new ModuleDefinitionException("standard action is not supported by entity: "
                    + entity.code() + "." + action.actionCode());
        }
        if (standardKind != null && action.kind() == EntityActionKind.CUSTOM) {
            throw new ModuleDefinitionException("custom action conflicts with standard action: "
                    + entity.code() + "." + action.actionCode());
        }
        if (standardKind != null && standardKind != action.kind()) {
            throw new ModuleDefinitionException("standard action kind mismatch: "
                    + entity.code() + "." + action.actionCode());
        }
    }

    public void validateReference(EntityReferenceDefinition reference, Map<String, EntityDefinition> entities, String moduleAlias) {
        if (reference == null) {
            throw new ModuleDefinitionException("reference must not be null");
        }
        EntityDefinition source = requireEntity(entities, reference.sourceEntity(), "reference source entity");
        requireFieldName(reference.sourceField(), "reference source field");
        ReferenceTarget target;
        try {
            target = reference.target();
        } catch (RuntimeException e) {
            throw new ModuleDefinitionException("invalid reference target qualified name: " + reference.targetQualifiedName());
        }
        requireModuleAlias(target.moduleAlias(), "reference target module alias");
        requireIdentifier(target.entityCode(), "reference target entity code");
        requireField(source, reference.sourceField(), "reference source field");
        if (moduleAlias != null && moduleAlias.equals(target.moduleAlias())) {
            requireEntity(entities, target.entityCode(), "reference target entity");
        }
        Set<String> outputFields = new HashSet<>();
        ReferencePlan plan = referencePlan(reference);
        if (!reference.projections().isEmpty()) {
            if (moduleAlias != null && !moduleAlias.equals(target.moduleAlias())) {
                throw new ModuleDefinitionException("reference projection requires same module target: " + target.qualifiedName());
            }
            EntityDefinition targetEntity = requireEntity(entities, target.entityCode(), "reference target entity");
            requireReferenceTargetCapability(targetEntity, target);
            for (ReferenceProjection projection : reference.projections()) {
                requireField(targetEntity, projection.targetField(), "reference projection target field");
                requireFieldName(projection.outputField(), "reference projection output field");
                requireReferenceOutputField(source, projection.outputField(), "reference projection output field");
                requireUnique(outputFields, projection.outputField(), "reference output field");
            }
        }
        if (reference.autoTitle()) {
            if (moduleAlias != null && !moduleAlias.equals(target.moduleAlias())) {
                throw new ModuleDefinitionException("reference auto title requires same module target: " + target.qualifiedName());
            }
            EntityDefinition targetEntity = requireEntity(entities, target.entityCode(), "reference target entity");
            requireReferenceTargetCapability(targetEntity, target);
            String outputField = plan.titleOutputField();
            requireFieldName(outputField, "reference title output field");
            requireReferenceOutputField(source, outputField, "reference title output field");
            requireUnique(outputFields, outputField, "reference output field");
        }
    }

    private ReferencePlan referencePlan(EntityReferenceDefinition reference) {
        try {
            return reference.plan();
        } catch (PlatformException e) {
            throw new ModuleDefinitionException("reference output field invalid: " + e.getMessage());
        }
    }

    private void requireReferenceTargetCapability(EntityDefinition targetEntity, ReferenceTarget target) {
        if (!targetEntity.supports(EntityCapability.REFERENCE)) {
            throw new ModuleDefinitionException("reference display target requires REFERENCE capability: "
                    + target.qualifiedName());
        }
    }

    private void requireReferenceOutputField(EntityDefinition source, String outputField, String name) {
        if (STANDARD_FIELDS.contains(outputField) || source.fields().stream().anyMatch(field -> field.fieldName().equals(outputField))) {
            throw new ModuleDefinitionException(name + " conflicts with entity field: " + source.code() + "." + outputField);
        }
    }

    private void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ModuleDefinitionException(name + " must not be blank");
        }
    }

    private void requireIdentifier(String value, String name) {
        requireText(value, name);
        if (!value.matches(IDENTIFIER_PATTERN)) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private void requireFieldName(String value, String name) {
        requireText(value, name);
        if (!value.matches("[a-z][A-Za-z0-9]{0,62}")) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private void requireActionCode(String value, String name) {
        requireText(value, name);
        if (!value.matches("[a-z][A-Za-z0-9]{0,63}")) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private String physicalTableKey(EntityDefinition entity) {
        String schemaName = entity.schemaName() == null || entity.schemaName().isBlank() ? "" : entity.schemaName();
        return schemaName + "." + entity.tableName();
    }

    private void requireSortField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("SORT capability requires standard field sortOrder: " + entity.code());
        }
        if (!PlatformAbilityFields.SORT_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.SORT_COLUMN.equals(field.columnName())
                || field.type() != FieldType.INTEGER) {
            throw new ModuleDefinitionException("SORT capability requires standard field sortOrder/sort_order: " + entity.code());
        }
    }

    private void requireTreeParentField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("TREE capability requires standard field parentId: " + entity.code());
        }
        if (!PlatformAbilityFields.TREE_PARENT_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.TREE_PARENT_COLUMN.equals(field.columnName())
                || field.type() != FieldType.STRING
                || !Integer.valueOf(PlatformAbilityFields.TREE_PARENT_LENGTH).equals(field.length())) {
            throw new ModuleDefinitionException("TREE capability requires standard field parentId/parent_id: " + entity.code());
        }
    }

    private void requireTitleField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("REFERENCE capability requires standard field title: " + entity.code());
        }
        if (!PlatformAbilityFields.TITLE_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.TITLE_COLUMN.equals(field.columnName())
                || field.type() != FieldType.STRING) {
            throw new ModuleDefinitionException("REFERENCE capability requires standard field title/title: " + entity.code());
        }
    }

    private void requireEnabledField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("ENABLE capability requires standard field enabled: " + entity.code());
        }
        if (!PlatformAbilityFields.ENABLED_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.ENABLED_COLUMN.equals(field.columnName())
                || field.type() != FieldType.BOOLEAN) {
            throw new ModuleDefinitionException("ENABLE capability requires standard field enabled/enabled: " + entity.code());
        }
    }

    private void requireModuleAlias(String value, String name) {
        try {
            PlatformNameRules.requireModuleAlias(value);
        } catch (RuntimeException e) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private void requireUnique(Set<String> values, String value, String name) {
        if (!values.add(value)) {
            throw new ModuleDefinitionException("duplicate " + name + ": " + value);
        }
    }

    private EntityDefinition requireEntity(Map<String, EntityDefinition> entities, String code, String name) {
        requireIdentifier(code, name);
        EntityDefinition entity = entities.get(code);
        if (entity == null) {
            throw new ModuleDefinitionException("unknown " + name + ": " + code);
        }
        return entity;
    }

    private FieldDefinition requireField(EntityDefinition entity, String fieldName, String name) {
        return entity.fields().stream()
                .filter(field -> field.fieldName().equals(fieldName))
                .findFirst()
                .orElseThrow(() -> new ModuleDefinitionException("unknown " + name + ": " + entity.code() + "." + fieldName));
    }
}
