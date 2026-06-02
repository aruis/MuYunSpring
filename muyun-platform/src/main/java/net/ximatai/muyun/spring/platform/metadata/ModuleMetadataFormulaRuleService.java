package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.formula.FormulaEvaluationException;
import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ModuleMetadataFormulaRuleService extends AbstractAbilityService<ModuleMetadataFormulaRule> implements
        SoftDeleteAbility<ModuleMetadataFormulaRule>,
        EnableAbility<ModuleMetadataFormulaRule>,
        SortAbility<ModuleMetadataFormulaRule> {
    public static final String MODULE_ALIAS = "platform.module_metadata_formula_rule";

    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final ModuleMetadataRelationService relationService;
    private final FormulaEngine formulaEngine = new FormulaEngine();
    private final MetadataFormulaFieldValidator fieldValidator;

    public ModuleMetadataFormulaRuleService(BaseDao<ModuleMetadataFormulaRule, String> formulaRuleDao,
                                            ModuleMetadataRelationService relationService,
                                            MetadataFieldService fieldService) {
        super(MODULE_ALIAS, ModuleMetadataFormulaRule.class, formulaRuleDao);
        this.relationService = relationService;
        this.fieldValidator = new MetadataFormulaFieldValidator(relationService, fieldService);
    }

    @Override
    public void beforeInsert(ModuleMetadataFormulaRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public void beforeUpdate(ModuleMetadataFormulaRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public Criteria sortScope(ModuleMetadataFormulaRule rule) {
        return Criteria.of().eq("relationId", rule.getRelationId());
    }

    @Override
    public void validateSortScope(ModuleMetadataFormulaRule left, ModuleMetadataFormulaRule right) {
        if (!Objects.equals(left.getRelationId(), right.getRelationId())) {
            throw new PlatformException("Metadata formula rule sort can only move records within the same relation");
        }
    }

    public List<ModuleMetadataFormulaRule> listByRelationIds(List<String> relationIds) {
        if (relationIds == null || relationIds.isEmpty()) {
            return List.of();
        }
        return list(Criteria.of().in("relationId", relationIds), ALL, Sort.asc("sortOrder"));
    }

    public EntityFormulaRuleDefinition compile(ModuleMetadataFormulaRule rule) {
        return new EntityFormulaRuleDefinition(
                rule.getAlias(),
                rule.getExpression(),
                rule.getRuleKind(),
                rule.getRulePhase(),
                rule.getTargetField(),
                rule.getSeverity(),
                rule.getMessageTemplate(),
                Boolean.TRUE.equals(rule.getStopOnError()),
                Boolean.TRUE.equals(rule.getEnabled()),
                rule.getSortOrder() == null ? 0 : rule.getSortOrder()
        );
    }

    private void normalizeAndValidate(ModuleMetadataFormulaRule rule) {
        ModuleMetadataRelation relation = rule.getRelationId() == null || rule.getRelationId().isBlank()
                ? null
                : relationService.select(rule.getRelationId());
        if (relation == null) {
            throw new PlatformException("Metadata formula rule requires existing relation: " + rule.getRelationId());
        }
        if (rule.getAlias() == null || !rule.getAlias().matches("[a-z][A-Za-z0-9]{0,63}")) {
            throw new PlatformException("Metadata formula rule requires valid alias: " + rule.getAlias());
        }
        if (rule.getRuleKind() == null) {
            rule.setRuleKind(FormulaRuleKind.VALIDATION);
        }
        if (rule.getRulePhase() == null) {
            rule.setRulePhase(FormulaRulePhase.BEFORE_SAVE);
        }
        if (rule.getExpression() == null || rule.getExpression().isBlank()) {
            throw new PlatformException("Metadata formula rule requires expression: " + rule.getAlias());
        }
        rule.setExpression(rule.getExpression().trim());
        if (rule.getTargetField() != null && rule.getTargetField().isBlank()) {
            rule.setTargetField(null);
        }
        if (rule.getSeverity() == null) {
            rule.setSeverity(FormulaIssueLevel.ERROR);
        }
        if (rule.getMessageTemplate() != null && rule.getMessageTemplate().isBlank()) {
            rule.setMessageTemplate(null);
        }
        if (rule.getStopOnError() == null) {
            rule.setStopOnError(rule.getRuleKind() == FormulaRuleKind.VALIDATION);
        }
        validateFormula(rule, relation);
        rejectDuplicate(rule, Criteria.of()
                        .eq("relationId", rule.getRelationId())
                        .eq("alias", rule.getAlias()),
                "module metadata formula rule must be unique in relation: "
                        + rule.getRelationId() + "." + rule.getAlias());
    }

    private void validateFormula(ModuleMetadataFormulaRule rule, ModuleMetadataRelation relation) {
        try {
            if (formulaEngine.parse(rule.getAlias(), rule.getExpression()) == null) {
                throw new PlatformException("Metadata formula expression is invalid: " + rule.getAlias());
            }
            if (rule.getRuleKind() != FormulaRuleKind.CALCULATION
                    && formulaEngine.containsAssignment(rule.getExpression())) {
                throw new PlatformException("Metadata formula expression must not assign fields: " + rule.getAlias());
            }
            if (rule.getRuleKind() == FormulaRuleKind.CALCULATION
                    && rule.getTargetField() != null
                    && rule.getTargetField().contains(".")) {
                throw new PlatformException("Metadata formula targetField cannot be child field: " + rule.getAlias());
            }
            fieldValidator.validateTargetField(rule.getTargetField(), relation, "Metadata formula");
            fieldValidator.validateExpressionFields(formulaEngine.referencedFields(rule.getExpression()), relation,
                    "Metadata formula");
        } catch (FormulaEvaluationException exception) {
            throw new PlatformException("Metadata formula expression is invalid: "
                    + rule.getAlias() + ", " + exception.getMessage(), exception);
        }
    }
}
