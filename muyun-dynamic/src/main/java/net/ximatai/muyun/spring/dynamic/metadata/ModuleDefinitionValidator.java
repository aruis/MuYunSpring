package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.formula.FormulaEvaluationException;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ModuleDefinitionValidator {
    private static final String IDENTIFIER_PATTERN = "[a-z][a-z0-9_]{0,62}";
    private static final Set<String> STANDARD_COLUMNS = Set.copyOf(StandardEntitySchema.columnNames());
    private static final Set<String> STANDARD_FIELDS = Set.copyOf(StandardEntitySchema.fieldNames());
    private final FormulaEngine formulaEngine = new FormulaEngine();

    public void validate(ModuleDefinition module) {
        if (module == null) {
            throw new ModuleDefinitionException("module must not be null");
        }
        requireModuleAlias(module.moduleAlias(), "module alias");
        requireText(module.name(), "module name");
        Set<String> entityAliases = new HashSet<>();
        Set<String> tableNames = new HashSet<>();
        for (EntityDefinition entity : module.entities()) {
            validateEntity(entity);
            requireUnique(entityAliases, entity.alias(), "entity alias");
            requireUnique(tableNames, physicalTableKey(entity), "table name");
        }
        Map<String, EntityDefinition> entities = module.entities().stream()
                .collect(Collectors.toMap(EntityDefinition::alias, Function.identity()));
        if (module.mainEntityAlias() != null) {
            requireEntity(entities, module.mainEntityAlias(), "module main entity");
        }
        Set<String> relationCodes = new HashSet<>();
        for (EntityRelationDefinition relation : module.relations()) {
            validateRelation(relation, entities);
            requireUnique(relationCodes, relation.parentEntityAlias() + "." + relation.code(), "relation code");
        }
        validateFormulaRuleTargets(module, entities);
        for (EntityReferenceDefinition reference : module.references()) {
            validateReference(reference, entities, module.moduleAlias());
        }
        Set<String> associationViewKeys = new HashSet<>();
        for (EntityAssociationViewDefinition view : module.associationViews()) {
            validateAssociationView(view, entities, module.moduleAlias(), module.relations(), module.references());
            requireUnique(associationViewKeys, view.sourceEntityAlias() + "." + view.code(), "association view");
        }
        Set<String> actionKeys = new HashSet<>();
        for (EntityActionDefinition action : module.actions()) {
            validateAction(action, entities, module.relations());
            requireUnique(actionKeys, action.entityAlias() + "." + action.actionCode(), "action");
        }
        validateActionAuthInherits(module.actions(), entities);
        Set<String> viewKeys = new HashSet<>();
        for (EntityViewDefinition view : module.views()) {
            validateView(view, entities);
            requireUnique(viewKeys, view.entityAlias() + "." + view.viewType(), "view");
        }
    }

    public void validateEntity(EntityDefinition entity) {
        if (entity == null) {
            throw new ModuleDefinitionException("entity must not be null");
        }
        requireIdentifier(entity.alias(), "entity alias");
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
            throw new ModuleDefinitionException("dynamic entity requires CRUD capability: " + entity.alias());
        }
        if (sortableFields > 1) {
            throw new ModuleDefinitionException("entity can only have one sortable field: " + entity.alias());
        }
        if (titleFields > 1) {
            throw new ModuleDefinitionException("entity can only have one title field: " + entity.alias());
        }
        if (sortableFields > 0 && !entity.supports(EntityCapability.SORT)) {
            throw new ModuleDefinitionException("sortable field requires SORT capability: " + entity.alias());
        }
        if (entity.supports(EntityCapability.TREE)) {
            requireTreeParentField(entity, treeParentField);
        }
        if (entity.supports(EntityCapability.SORT)) {
            requireSortField(entity, sortableField);
        }
        if (titleFields > 0 && !entity.supports(EntityCapability.REFERENCE)) {
            throw new ModuleDefinitionException("title field requires REFERENCE capability: " + entity.alias());
        }
        if (enabledField != null && !entity.supports(EntityCapability.ENABLE)) {
            throw new ModuleDefinitionException("enabled field requires ENABLE capability: " + entity.alias());
        }
        if (entity.supports(EntityCapability.REFERENCE)) {
            requireTitleField(entity, titleField);
        }
        if (entity.supports(EntityCapability.ENABLE)) {
            requireEnabledField(entity, enabledField);
        }
        FieldCompanionRules.validateEntity(entity);
        validateFormulaRules(entity);
    }

    public void validateFormulaRules(EntityDefinition entity) {
        Set<String> ruleCodes = new HashSet<>();
        for (EntityFormulaRuleDefinition rule : entity.formulaRules()) {
            validateFormulaRule(entity, rule);
            requireUnique(ruleCodes, rule.code(), "formula rule code");
        }
    }

    public void validateFormulaRule(EntityDefinition entity, EntityFormulaRuleDefinition rule) {
        if (rule == null) {
            throw new ModuleDefinitionException("formula rule must not be null: " + entity.alias());
        }
        requireActionCode(rule.code(), "formula rule code");
        requireText(rule.expression(), "formula expression");
        requireFormulaExpression(rule);
        if (rule.kind() == null) {
            throw new ModuleDefinitionException("formula rule kind must not be null: " + rule.code());
        }
        if (rule.phase() == null) {
            throw new ModuleDefinitionException("formula rule phase must not be null: " + rule.code());
        }
        if (rule.severity() == null) {
            throw new ModuleDefinitionException("formula rule severity must not be null: " + rule.code());
        }
        if (rule.targetField() != null && !rule.targetField().contains(".")) {
            requireFieldName(rule.targetField(), "formula target field");
            requireField(entity, rule.targetField(), "formula target field");
        }
        if (rule.targetField() != null && rule.targetField().contains(".")) {
            String[] parts = rule.targetField().split("\\.");
            if (parts.length != 2) {
                throw new ModuleDefinitionException("invalid formula target field: " + rule.targetField());
            }
            requireIdentifier(parts[0], "formula target relation");
            requireFieldName(parts[1], "formula target field");
        }
    }

    private void validateFormulaRuleTargets(ModuleDefinition module, Map<String, EntityDefinition> entities) {
        for (EntityDefinition entity : module.entities()) {
            for (EntityFormulaRuleDefinition rule : entity.formulaRules()) {
                if (rule == null || rule.targetField() == null || !rule.targetField().contains(".")) {
                    continue;
                }
                String[] parts = rule.targetField().split("\\.");
                if (parts.length != 2) {
                    continue;
                }
                EntityRelationDefinition relation = module.relations().stream()
                        .filter(candidate -> entity.alias().equals(candidate.parentEntityAlias())
                                && parts[0].equals(candidate.code()))
                        .findFirst()
                        .orElseThrow(() -> new ModuleDefinitionException("unknown formula target relation: "
                                + entity.alias() + "." + parts[0]));
                EntityDefinition childEntity = requireEntity(entities, relation.childEntityAlias(), "formula target child entity");
                requireField(childEntity, parts[1], "formula target field");
            }
        }
    }

    private void requireFormulaExpression(EntityFormulaRuleDefinition rule) {
        try {
            Object parsed = formulaEngine.parse(rule.code(), rule.expression());
            if (parsed == null) {
                throw new ModuleDefinitionException("invalid formula expression: " + rule.code());
            }
        } catch (FormulaEvaluationException e) {
            throw new ModuleDefinitionException("invalid formula expression: " + rule.code() + ", " + e.getMessage());
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
        if (field.dictionaryBinding() != null) {
            if (field.dictionaryBinding().selectionMode() == OptionSelectionMode.MULTIPLE) {
                if (field.type() != FieldType.JSON) {
                    throw new ModuleDefinitionException("multiple dictionary binding requires JSON field: " + field.code());
                }
            } else if (field.type() != FieldType.STRING && field.type() != FieldType.TEXT) {
                throw new ModuleDefinitionException("dictionary binding requires string field: " + field.code());
            }
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
        try {
            FieldBehaviorSupport.validateBehavior(field.type(), field.behavior(), field.code());
        } catch (RuntimeException e) {
            throw new ModuleDefinitionException(e.getMessage());
        }
    }

    public void validateRelation(EntityRelationDefinition relation, Map<String, EntityDefinition> entities) {
        if (relation == null) {
            throw new ModuleDefinitionException("relation must not be null");
        }
        requireIdentifier(relation.code(), "relation code");
        EntityDefinition parent = requireEntity(entities, relation.parentEntityAlias(), "relation parent entity");
        EntityDefinition child = requireEntity(entities, relation.childEntityAlias(), "relation child entity");
        requireFieldName(relation.childForeignKeyField(), "relation child foreign key field");
        if (parent.alias().equals(child.alias())) {
            throw new ModuleDefinitionException("child relation must use different parent and child entities: " + relation.code());
        }
        FieldDefinition childForeignKeyField = requireField(child, relation.childForeignKeyField(), "relation child foreign key field");
        if (childForeignKeyField.type() != FieldType.STRING) {
            throw new ModuleDefinitionException("relation child foreign key field must be STRING: "
                    + child.alias() + "." + relation.childForeignKeyField());
        }
    }

    public void validateReference(EntityReferenceDefinition reference, Map<String, EntityDefinition> entities) {
        validateReference(reference, entities, null);
    }

    public void validateView(EntityViewDefinition view, Map<String, EntityDefinition> entities) {
        if (view == null) {
            throw new ModuleDefinitionException("view must not be null");
        }
        EntityDefinition entity = requireEntity(entities, view.entityAlias(), "view entity");
        if (view.viewType() == null) {
            throw new ModuleDefinitionException("view type must not be null: " + view.entityAlias());
        }
        requireText(view.title(), "view title");
        Set<String> fieldNames = new HashSet<>();
        for (EntityViewFieldDefinition field : view.fields()) {
            if (field == null) {
                throw new ModuleDefinitionException("view field must not be null: " + view.entityAlias());
            }
            requireFieldName(field.fieldName(), "view field name");
            requireField(entity, field.fieldName(), "view field");
            requireUnique(fieldNames, field.fieldName(), "view field");
            if (field.title() != null && field.title().isBlank()) {
                throw new ModuleDefinitionException("view field title must not be blank: "
                        + view.entityAlias() + "." + field.fieldName());
            }
        }
    }

    public void validateAction(EntityActionDefinition action, Map<String, EntityDefinition> entities) {
        validateAction(action, entities, List.of());
    }

    public void validateAction(EntityActionDefinition action,
                               Map<String, EntityDefinition> entities,
                               List<EntityRelationDefinition> relations) {
        if (action == null) {
            throw new ModuleDefinitionException("action must not be null");
        }
        EntityDefinition entity = requireEntity(entities, action.entityAlias(), "action entity");
        requireActionCode(action.actionCode(), "action code");
        requireText(action.title(), "action title");
        if (action.kind() == null) {
            throw new ModuleDefinitionException("action kind must not be null: " + action.actionCode());
        }
        if (action.level() == null) {
            throw new ModuleDefinitionException("action level must not be null: " + action.actionCode());
        }
        if (action.style() == null) {
            throw new ModuleDefinitionException("action style must not be null: " + action.actionCode());
        }
        if (action.category() == null) {
            throw new ModuleDefinitionException("action category must not be null: " + action.actionCode());
        }
        if (action.accessMode() == null) {
            throw new ModuleDefinitionException("action access mode must not be null: " + action.actionCode());
        }
        if (action.executorType() == null) {
            throw new ModuleDefinitionException("action executor type must not be null: " + action.actionCode());
        }
        if (action.executorKey() != null && action.executorKey().isBlank()) {
            throw new ModuleDefinitionException("action executor key must not be blank: " + action.actionCode());
        }
        if (action.category() != EntityActionCategory.STANDARD
                && DynamicActionPathRules.isReservedWebActionCode(action.actionCode())) {
            throw new ModuleDefinitionException("custom action conflicts with reserved web action path: "
                    + entity.alias() + "." + action.actionCode());
        }
        validateActionAccessPolicy(action);
        validateActionAvailability(action, entity, entities, relations);
        EntityActionKind standardKind = EntityStandardActionCatalog.standardKind(entity, action.actionCode());
        if (standardKind == null && action.kind() != EntityActionKind.CUSTOM) {
            throw new ModuleDefinitionException("standard action is not supported by entity: "
                    + entity.alias() + "." + action.actionCode());
        }
        if (standardKind != null && action.kind() == EntityActionKind.CUSTOM) {
            throw new ModuleDefinitionException("custom action conflicts with standard action: "
                    + entity.alias() + "." + action.actionCode());
        }
        if (standardKind != null && standardKind != action.kind()) {
            throw new ModuleDefinitionException("standard action kind mismatch: "
                    + entity.alias() + "." + action.actionCode());
        }
    }

    private void validateActionAccessPolicy(EntityActionDefinition action) {
        if (action.accessMode() == EntityActionAccessMode.ANONYMOUS_ALLOWED) {
            if (action.actionAuth() || action.dataAuth() || action.authInheritActionCode() != null) {
                throw new ModuleDefinitionException("anonymous action must not require auth policy: "
                        + action.entityAlias() + "." + action.actionCode());
            }
            return;
        }
        if (action.accessMode() == EntityActionAccessMode.LOGIN_REQUIRED) {
            if (action.actionAuth() || action.dataAuth() || action.authInheritActionCode() != null) {
                throw new ModuleDefinitionException("login-only action must not require auth policy: "
                        + action.entityAlias() + "." + action.actionCode());
            }
            return;
        }
        if (!action.actionAuth()) {
            throw new ModuleDefinitionException("auth-required action must enable action auth: "
                    + action.entityAlias() + "." + action.actionCode());
        }
        if (action.authInheritActionCode() != null && !action.actionAuth()) {
            throw new ModuleDefinitionException("action auth inherit requires action auth: "
                    + action.entityAlias() + "." + action.actionCode());
        }
    }

    private void validateActionAvailability(EntityActionDefinition action,
                                            EntityDefinition entity,
                                            Map<String, EntityDefinition> entities,
                                            List<EntityRelationDefinition> relations) {
        if (action.availableExpression() == null) {
            return;
        }
        requireText(action.availableExpression(), "action available expression");
        try {
            Object parsed = formulaEngine.parse(action.actionCode(), action.availableExpression());
            if (parsed == null) {
                throw new ModuleDefinitionException("invalid action available expression: " + action.actionCode());
            }
            if (formulaEngine.containsAssignment(action.availableExpression())) {
                throw new ModuleDefinitionException("action available expression must not assign fields: "
                        + action.actionCode());
            }
            validateActionAvailabilityFields(action, entity, entities, relations);
        } catch (FormulaEvaluationException e) {
            throw new ModuleDefinitionException("invalid action available expression: "
                    + action.actionCode() + ", " + e.getMessage());
        }
        if (action.unavailableMessage() != null && action.unavailableMessage().isBlank()) {
            throw new ModuleDefinitionException("action unavailable message must not be blank: "
                    + action.actionCode());
        }
    }

    private void validateActionAvailabilityFields(EntityActionDefinition action,
                                                  EntityDefinition entity,
                                                  Map<String, EntityDefinition> entities,
                                                  List<EntityRelationDefinition> relations) {
        for (String fieldPath : formulaEngine.referencedFields(action.availableExpression())) {
            if (!fieldPath.contains(".")) {
                requireField(entity, fieldPath, "action available expression field");
                continue;
            }
            String[] parts = fieldPath.split("\\.");
            if (parts.length != 2) {
                throw new ModuleDefinitionException("invalid action available expression field: "
                        + action.actionCode() + "." + fieldPath);
            }
            EntityRelationDefinition relation = relations.stream()
                    .filter(candidate -> entity.alias().equals(candidate.parentEntityAlias())
                            && parts[0].equals(candidate.code()))
                    .findFirst()
                    .orElseThrow(() -> new ModuleDefinitionException("unknown action available expression relation: "
                            + action.actionCode() + "." + parts[0]));
            EntityDefinition childEntity = requireEntity(entities, relation.childEntityAlias(),
                    "action available expression child entity");
            requireField(childEntity, parts[1], "action available expression field");
        }
    }

    private void validateActionAuthInherits(List<EntityActionDefinition> actions,
                                            Map<String, EntityDefinition> entities) {
        Map<String, Set<String>> configuredByEntity = actions.stream()
                .collect(Collectors.groupingBy(
                        EntityActionDefinition::entityAlias,
                        Collectors.mapping(EntityActionDefinition::actionCode, Collectors.toSet())
                ));
        for (EntityActionDefinition action : actions) {
            if (action.authInheritActionCode() == null) {
                continue;
            }
            requireActionCode(action.authInheritActionCode(), "action auth inherit code");
            if (action.actionCode().equals(action.authInheritActionCode())) {
                throw new ModuleDefinitionException("action auth inherit code must not point to self: "
                        + action.entityAlias() + "." + action.actionCode());
            }
            if (!actionExists(action.entityAlias(), action.authInheritActionCode(), entities, configuredByEntity)) {
                throw new ModuleDefinitionException("action auth inherit target is not configured: "
                        + action.entityAlias() + "." + action.authInheritActionCode());
            }
            detectActionAuthInheritCycle(action, actions, entities, configuredByEntity);
        }
    }

    private boolean actionExists(String entityAlias,
                                 String actionCode,
                                 Map<String, EntityDefinition> entities,
                                 Map<String, Set<String>> configuredByEntity) {
        EntityDefinition entity = entities.get(entityAlias);
        return configuredByEntity.getOrDefault(entityAlias, Set.of()).contains(actionCode)
                || EntityStandardActionCatalog.standardKind(entity, actionCode) != null;
    }

    private void detectActionAuthInheritCycle(EntityActionDefinition start,
                                              List<EntityActionDefinition> actions,
                                              Map<String, EntityDefinition> entities,
                                              Map<String, Set<String>> configuredByEntity) {
        Map<String, EntityActionDefinition> configured = actions.stream()
                .filter(action -> start.entityAlias().equals(action.entityAlias()))
                .collect(Collectors.toMap(EntityActionDefinition::actionCode, Function.identity(), (left, ignored) -> left));
        Set<String> visited = new HashSet<>();
        visited.add(start.actionCode());
        String current = start.authInheritActionCode();
        while (current != null) {
            if (!visited.add(current)) {
                throw new ModuleDefinitionException("action auth inherit cycle: "
                        + start.entityAlias() + "." + start.actionCode());
            }
            if (!actionExists(start.entityAlias(), current, entities, configuredByEntity)) {
                throw new ModuleDefinitionException("action auth inherit target is not configured: "
                        + start.entityAlias() + "." + current);
            }
            EntityActionDefinition next = configured.get(current);
            if (next != null && (!next.actionAuth() || next.accessMode() != EntityActionAccessMode.AUTH_REQUIRED)) {
                throw new ModuleDefinitionException("action auth inherit target must require action auth: "
                        + start.entityAlias() + "." + current);
            }
            current = next == null ? null : next.authInheritActionCode();
        }
    }

    public void validateAssociationView(EntityAssociationViewDefinition view,
                                        Map<String, EntityDefinition> entities,
                                        String moduleAlias,
                                        java.util.List<EntityRelationDefinition> relations,
                                        java.util.List<EntityReferenceDefinition> references) {
        if (view == null) {
            throw new ModuleDefinitionException("association view must not be null");
        }
        requireAssociationViewCode(view.code(), "association view code");
        EntityDefinition source = requireEntity(entities, view.sourceEntityAlias(), "association view source entity");
        requireModuleAlias(view.targetModuleAlias(), "association view target module alias");
        requireIdentifier(view.targetEntityAlias(), "association view target entity");
        if (view.displayMode() == null) {
            throw new ModuleDefinitionException("association view display mode must not be null: " + view.code());
        }
        if (view.viewType() == null) {
            throw new ModuleDefinitionException("association view type must not be null: " + view.code());
        }
        if (moduleAlias != null && moduleAlias.equals(view.targetModuleAlias())) {
            requireEntity(entities, view.targetEntityAlias(), "association view target entity");
        }
        boolean hasRelation = view.relationCode() != null && !view.relationCode().isBlank();
        boolean hasReference = view.referenceField() != null && !view.referenceField().isBlank();
        if (hasRelation == hasReference) {
            throw new ModuleDefinitionException("association view requires exactly one relationCode or referenceField: "
                    + view.code());
        }
        if (hasRelation) {
            requireIdentifier(view.relationCode(), "association view relation code");
            if (moduleAlias != null && !moduleAlias.equals(view.targetModuleAlias())) {
                throw new ModuleDefinitionException("association view relation target module must be current module: "
                        + view.code());
            }
            requireMatchingRelation(view, relations);
        }
        if (hasReference) {
            requireFieldName(view.referenceField(), "association view reference field");
            requireField(source, view.referenceField(), "association view reference field");
            EntityReferenceDefinition reference = requireMatchingReference(view, references);
            requireMatchingReferenceDisplay(view, reference);
        }
    }

    public void validateReference(EntityReferenceDefinition reference, Map<String, EntityDefinition> entities, String moduleAlias) {
        if (reference == null) {
            throw new ModuleDefinitionException("reference must not be null");
        }
        EntityDefinition source = requireEntity(entities, reference.sourceEntityAlias(), "reference source entity");
        requireFieldName(reference.sourceField(), "reference source field");
        ReferenceTarget target;
        try {
            target = reference.target();
        } catch (RuntimeException e) {
            throw new ModuleDefinitionException("invalid reference target qualified name: " + reference.targetQualifiedName());
        }
        requireModuleAlias(target.moduleAlias(), "reference target module alias");
        requireIdentifier(target.entityAlias(), "reference target entity alias");
        requireField(source, reference.sourceField(), "reference source field");
        if (moduleAlias != null && moduleAlias.equals(target.moduleAlias())) {
            requireEntity(entities, target.entityAlias(), "reference target entity");
        }
        Set<String> outputFields = new HashSet<>();
        ReferencePlan plan = referencePlan(reference);
        if (!reference.projections().isEmpty()) {
            if (moduleAlias != null && !moduleAlias.equals(target.moduleAlias())) {
                throw new ModuleDefinitionException("reference projection requires same module target: " + target.qualifiedName());
            }
            EntityDefinition targetEntity = requireEntity(entities, target.entityAlias(), "reference target entity");
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
            EntityDefinition targetEntity = requireEntity(entities, target.entityAlias(), "reference target entity");
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
            throw new ModuleDefinitionException(name + " conflicts with entity field: " + source.alias() + "." + outputField);
        }
    }

    private void requireMatchingRelation(EntityAssociationViewDefinition view,
                                         java.util.List<EntityRelationDefinition> relations) {
        boolean exists = relations.stream().anyMatch(relation ->
                view.relationCode().equals(relation.code())
                        && view.sourceEntityAlias().equals(relation.parentEntityAlias())
                        && view.targetEntityAlias().equals(relation.childEntityAlias()));
        if (!exists) {
            throw new ModuleDefinitionException("association view relation does not match module relation: "
                    + view.sourceEntityAlias() + "." + view.code());
        }
    }

    private EntityReferenceDefinition requireMatchingReference(EntityAssociationViewDefinition view,
                                                              java.util.List<EntityReferenceDefinition> references) {
        Optional<EntityReferenceDefinition> found = references.stream().filter(reference -> {
            if (!view.sourceEntityAlias().equals(reference.sourceEntityAlias())
                    || !view.referenceField().equals(reference.sourceField())) {
                return false;
            }
            ReferenceTarget target;
            try {
                target = reference.target();
            } catch (RuntimeException e) {
                return false;
            }
            String effectiveModuleAlias = target.moduleAlias();
            return view.targetModuleAlias().equals(effectiveModuleAlias)
                    && view.targetEntityAlias().equals(target.entityAlias());
        }).findFirst();
        if (found.isEmpty()) {
            throw new ModuleDefinitionException("association view reference does not match module reference: "
                    + view.sourceEntityAlias() + "." + view.code());
        }
        return found.get();
    }

    private void requireMatchingReferenceDisplay(EntityAssociationViewDefinition view, EntityReferenceDefinition reference) {
        if (reference.cardinality() == net.ximatai.muyun.spring.ability.reference.ReferenceCardinality.MANY) {
            if (view.displayMode() != AssociationViewDisplayMode.LINKED_LIST || view.viewType() != EntityViewType.LIST) {
                throw new ModuleDefinitionException("many reference association view requires LINKED_LIST LIST: "
                        + view.sourceEntityAlias() + "." + view.code());
            }
            return;
        }
        if (view.displayMode() != AssociationViewDisplayMode.LINKED_RECORD || view.viewType() != EntityViewType.FORM) {
            throw new ModuleDefinitionException("single reference association view requires LINKED_RECORD FORM: "
                    + view.sourceEntityAlias() + "." + view.code());
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

    private void requireAssociationViewCode(String value, String name) {
        requireText(value, name);
        if (!value.matches("[a-z][A-Za-z0-9_]{0,63}")) {
            throw new ModuleDefinitionException("invalid " + name + ": " + value);
        }
    }

    private String physicalTableKey(EntityDefinition entity) {
        String schemaName = entity.schemaName() == null || entity.schemaName().isBlank() ? "" : entity.schemaName();
        return schemaName + "." + entity.tableName();
    }

    private void requireSortField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("SORT capability requires standard field sortOrder: " + entity.alias());
        }
        if (!PlatformAbilityFields.SORT_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.SORT_COLUMN.equals(field.columnName())
                || field.type() != FieldType.INTEGER) {
            throw new ModuleDefinitionException("SORT capability requires standard field sortOrder/sort_order: " + entity.alias());
        }
    }

    private void requireTreeParentField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("TREE capability requires standard field parentId: " + entity.alias());
        }
        if (!PlatformAbilityFields.TREE_PARENT_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.TREE_PARENT_COLUMN.equals(field.columnName())
                || field.type() != FieldType.STRING
                || !Integer.valueOf(PlatformAbilityFields.TREE_PARENT_LENGTH).equals(field.length())) {
            throw new ModuleDefinitionException("TREE capability requires standard field parentId/parent_id: " + entity.alias());
        }
    }

    private void requireTitleField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("REFERENCE capability requires standard field title: " + entity.alias());
        }
        if (!PlatformAbilityFields.TITLE_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.TITLE_COLUMN.equals(field.columnName())
                || field.type() != FieldType.STRING) {
            throw new ModuleDefinitionException("REFERENCE capability requires standard field title/title: " + entity.alias());
        }
    }

    private void requireEnabledField(EntityDefinition entity, FieldDefinition field) {
        if (field == null) {
            throw new ModuleDefinitionException("ENABLE capability requires standard field enabled: " + entity.alias());
        }
        if (!PlatformAbilityFields.ENABLED_FIELD.equals(field.fieldName())
                || !PlatformAbilityFields.ENABLED_COLUMN.equals(field.columnName())
                || field.type() != FieldType.BOOLEAN) {
            throw new ModuleDefinitionException("ENABLE capability requires standard field enabled/enabled: " + entity.alias());
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
                .orElseThrow(() -> new ModuleDefinitionException("unknown " + name + ": " + entity.alias() + "." + fieldName));
    }
}
