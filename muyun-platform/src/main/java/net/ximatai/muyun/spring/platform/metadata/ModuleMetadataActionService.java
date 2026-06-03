package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.formula.FormulaEvaluationException;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionStyle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ModuleMetadataActionService extends AbstractAbilityService<ModuleMetadataAction> implements
        SoftDeleteAbility<ModuleMetadataAction>,
        EnableAbility<ModuleMetadataAction>,
        SortAbility<ModuleMetadataAction> {
    public static final String MODULE_ALIAS = "platform.module_metadata_action";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;
    private final FormulaEngine formulaEngine = new FormulaEngine();
    private final MetadataFormulaFieldValidator fieldValidator;

    public ModuleMetadataActionService(BaseDao<ModuleMetadataAction, String> actionDao,
                                       ModuleMetadataRelationService relationService,
                                       MetadataFieldService fieldService) {
        super(MODULE_ALIAS, ModuleMetadataAction.class, actionDao);
        this.relationService = relationService;
        this.fieldValidator = new MetadataFormulaFieldValidator(relationService, fieldService);
    }

    @Override
    public void beforeInsert(ModuleMetadataAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public void beforeUpdate(ModuleMetadataAction action) {
        normalizeAndValidate(action);
    }

    @Override
    public Criteria sortScope(ModuleMetadataAction action) {
        return Criteria.of().eq("relationId", action.getRelationId());
    }

    @Override
    public void validateSortScope(ModuleMetadataAction left, ModuleMetadataAction right) {
        if (!Objects.equals(left.getRelationId(), right.getRelationId())) {
            throw new PlatformException("Metadata action sort can only move records within the same relation");
        }
    }

    public List<ModuleMetadataAction> listByRelationIds(List<String> relationIds) {
        if (relationIds == null || relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc("sortOrder"));
    }

    private void normalizeAndValidate(ModuleMetadataAction action) {
        ModuleMetadataRelation relation = action.getRelationId() == null || action.getRelationId().isBlank()
                ? null
                : relationService.select(action.getRelationId());
        if (relation == null) {
            throw new PlatformException("Metadata action requires existing relation: " + action.getRelationId());
        }
        if (action.getActionCode() == null || !action.getActionCode().matches("[a-z][A-Za-z0-9]{0,63}")) {
            throw new PlatformException("Module metadata action requires valid actionCode: " + action.getActionCode());
        }
        if (action.getActionKind() == null) {
            throw new PlatformException("Metadata action requires actionKind");
        }
        if (action.getCategory() == null) {
            action.setCategory(EntityActionDefinition.defaultCategory(action.getActionKind()));
        }
        if (action.getActionLevel() == null) {
            action.setActionLevel(EntityActionDefinition.defaultLevel(action.getActionCode(), action.getActionKind()));
        }
        if (action.getActionStyle() == null) {
            action.setActionStyle(EntityActionStyle.NORMAL);
        }
        if (action.getAccessMode() == null) {
            action.setAccessMode(EntityActionAccessMode.AUTH_REQUIRED);
        }
        if (action.getActionAuth() == null) {
            action.setActionAuth(action.getAccessMode() == EntityActionAccessMode.AUTH_REQUIRED);
        }
        if (action.getDataAuth() == null) {
            action.setDataAuth(false);
        }
        if (action.getAuthInheritActionCode() != null && action.getAuthInheritActionCode().isBlank()) {
            action.setAuthInheritActionCode(null);
        }
        if (action.getAvailableExpression() != null && action.getAvailableExpression().isBlank()) {
            action.setAvailableExpression(null);
        }
        validateAvailableExpression(action, relation);
        if (action.getUnavailableMessage() != null && action.getUnavailableMessage().isBlank()) {
            action.setUnavailableMessage(null);
        }
        if (action.getExecutorType() == null) {
            action.setExecutorType(EntityActionDefinition.defaultExecutorType(action.getCategory()));
        }
        if (action.getExecutorKey() != null && action.getExecutorKey().isBlank()) {
            action.setExecutorKey(null);
        }
        if (action.getTargetMetadataId() != null && action.getTargetMetadataId().isBlank()) {
            action.setTargetMetadataId(null);
        }
        if (action.getConfigId() != null && action.getConfigId().isBlank()) {
            action.setConfigId(null);
        }
        if (action.getSystemManaged() == null) {
            action.setSystemManaged(false);
        }
        if (action.getTitle() == null || action.getTitle().isBlank()) {
            action.setTitle(action.getActionCode());
        }
        rejectDuplicate(action, Criteria.of()
                        .eq("relationId", action.getRelationId())
                        .eq("actionCode", action.getActionCode()),
                "module metadata action must be unique in relation: " + action.getRelationId() + "." + action.getActionCode());
    }

    private void validateAvailableExpression(ModuleMetadataAction action, ModuleMetadataRelation relation) {
        if (action.getAvailableExpression() == null) {
            return;
        }
        try {
            if (formulaEngine.parse(action.getActionCode(), action.getAvailableExpression()) == null) {
                throw new PlatformException("Metadata action availableExpression is invalid: " + action.getActionCode());
            }
            if (formulaEngine.containsAssignment(action.getAvailableExpression())) {
                throw new PlatformException("Metadata action availableExpression must not assign fields: " + action.getActionCode());
            }
            fieldValidator.validateExpressionFields(formulaEngine.referencedFields(action.getAvailableExpression()),
                    relation, "Metadata action availableExpression");
        } catch (FormulaEvaluationException exception) {
            throw new PlatformException("Metadata action availableExpression is invalid: "
                    + action.getActionCode() + ", " + exception.getMessage(), exception);
        }
    }

}
