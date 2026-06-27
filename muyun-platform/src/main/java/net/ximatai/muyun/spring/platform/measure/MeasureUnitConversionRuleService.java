package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MeasureUnitConversionRuleService extends AbstractAbilityService<MeasureUnitConversionRule> implements
        SoftDeleteAbility<MeasureUnitConversionRule>,
        EnableAbility<MeasureUnitConversionRule>,
        SortAbility<MeasureUnitConversionRule>,
        ReferenceAbility<MeasureUnitConversionRule>,
        QueryAbility<MeasureUnitConversionRule>
{
    public static final String MODULE_ALIAS = "platform.measure_unit_conversion_rule";

    private final MeasureUnitService unitService;

    public MeasureUnitConversionRuleService(BaseDao<MeasureUnitConversionRule, String> ruleDao,
                                            MeasureUnitService unitService) {
        super(MODULE_ALIAS, MeasureUnitConversionRule.class, ruleDao);
        this.unitService = unitService;
    }



    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("tenantId", QueryOperator.EQ, QueryOperator.IN).withTitle("租户"))
                .field(QueryField.of("applicationAlias", QueryOperator.EQ, QueryOperator.IN).withTitle("所属应用"))
                .field(QueryField.of("scopeType", QueryOperator.EQ).withTitle("范围类型"))
                .field(QueryField.of("moduleAlias", QueryOperator.EQ, QueryOperator.IN).withTitle("模块标识"))
                .field(QueryField.of("contextObjectType", QueryOperator.EQ).withTitle("上下文对象类型"))
                .field(QueryField.of("contextObjectId", QueryOperator.EQ, QueryOperator.IN).withTitle("上下文对象ID"))
                .field(QueryField.of("fromCategoryAlias", QueryOperator.EQ).withTitle("来源分类"))
                .field(QueryField.of("fromUnitCode", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("来源单位").withQuickSearch().withSortable())
                .field(QueryField.of("toCategoryAlias", QueryOperator.EQ).withTitle("目标分类"))
                .field(QueryField.of("toUnitCode", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("目标单位").withQuickSearch().withSortable())
                .field(QueryField.of("factor", QueryOperator.EQ).withTitle("系数"))
                .field(QueryField.of("priority", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("优先级").withSortable())
                .field(QueryField.of("effectiveFrom", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("生效开始").withSortable())
                .field(QueryField.of("effectiveTo", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("生效结束").withSortable())
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("名称").withQuickSearch().withSortable())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                        QueryOperator.BETWEEN)
                .withTitle("更新时间").withSortable())
                .defaultSort(Sort.desc("priority"))
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("title"))
                .build();
    }
    @Override
    public void beforeInsert(MeasureUnitConversionRule rule) {
        normalizeAndValidate(rule);
    }

    @Override
    public void beforeUpdate(MeasureUnitConversionRule rule) {
        normalizeAndValidate(rule);
        validateImmutableIdentity(rule);
    }

    @Override
    public Criteria sortScope(MeasureUnitConversionRule rule) {
        return Criteria.of()
                .eqNullable(StandardEntitySchema.TENANT_ID_FIELD, rule.getTenantId())
                .eq("applicationAlias", rule.getApplicationAlias())
                .eq("scopeType", rule.getScopeType())
                .eq("moduleAlias", rule.getModuleAlias())
                .eq("contextObjectType", rule.getContextObjectType())
                .eq("contextObjectId", rule.getContextObjectId());
    }

    @Override
    public void validateSortScope(MeasureUnitConversionRule left, MeasureUnitConversionRule right) {
        validateSortScopeByFields(left, right,
                "Measure unit conversion rule sort can only move records within the same tenant and scope",
                "tenantId", "applicationAlias", "scopeType", "moduleAlias", "contextObjectType", "contextObjectId");
    }

    public List<MeasureUnitConversionRule> applicableRules(MeasureUnitConversionContext context) {
        MeasureUnitConversionContext validContext = normalizeContext(context);
        LocalDateTime operatedAt = validContext.operatedAt() == null ? LocalDateTime.now() : validContext.operatedAt();
        return listVisibleRules(validContext)
                .stream()
                .filter(rule -> isApplicable(rule, validContext, operatedAt))
                .toList();
    }

    private void normalizeAndValidate(MeasureUnitConversionRule rule) {
        rule.setApplicationAlias(PlatformNameRules.requireApplicationAlias(rule.getApplicationAlias()));
        if (rule.getScopeType() == null) {
            rule.setScopeType(MeasureUnitConversionScopeType.GLOBAL);
        }
        normalizeScope(rule);
        rule.setFromCategoryAlias(requireCode(rule.getFromCategoryAlias(), "fromCategoryAlias"));
        rule.setFromUnitCode(requireCode(rule.getFromUnitCode(), "fromUnitCode"));
        rule.setToCategoryAlias(requireCode(rule.getToCategoryAlias(), "toCategoryAlias"));
        rule.setToUnitCode(requireCode(rule.getToUnitCode(), "toUnitCode"));
        unitService.requireVisibleUnit(rule.getApplicationAlias(), rule.getFromCategoryAlias(), rule.getFromUnitCode());
        unitService.requireVisibleUnit(rule.getApplicationAlias(), rule.getToCategoryAlias(), rule.getToUnitCode());
        if (rule.getFromCategoryAlias().equals(rule.getToCategoryAlias())
                && rule.getFromUnitCode().equals(rule.getToUnitCode())) {
            throw new PlatformException("measure conversion rule source and target must be different");
        }
        if (rule.getFactor() == null) {
            rule.setFactor(BigDecimal.ONE);
        }
        if (rule.getFactor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PlatformException("measure conversion rule factor must be positive");
        }
        if (rule.getPriority() == null) {
            rule.setPriority(0);
        }
        if (rule.getEffectiveFrom() != null && rule.getEffectiveTo() != null
                && !rule.getEffectiveFrom().isBefore(rule.getEffectiveTo())) {
            throw new PlatformException("measure conversion rule effectiveFrom must be before effectiveTo");
        }
        validateNoOverlappingRule(rule);
    }

    private void normalizeScope(MeasureUnitConversionRule rule) {
        switch (rule.getScopeType()) {
            case GLOBAL -> {
                rule.setModuleAlias(null);
                rule.setContextObjectType(null);
                rule.setContextObjectId(null);
            }
            case MODULE -> {
                rule.setModuleAlias(PlatformNameRules.requireModuleAlias(rule.getModuleAlias()));
                rule.setContextObjectType(null);
                rule.setContextObjectId(null);
            }
            case RECORD_CONTEXT -> {
                rule.setModuleAlias(PlatformNameRules.requireModuleAlias(rule.getModuleAlias()));
                rule.setContextObjectType(requireCode(rule.getContextObjectType(), "contextObjectType"));
                if (rule.getContextObjectId() == null || rule.getContextObjectId().isBlank()) {
                    throw new PlatformException("measure conversion rule contextObjectId must not be blank");
                }
            }
        }
    }

    private void validateImmutableIdentity(MeasureUnitConversionRule rule) {
        MeasureUnitConversionRule existing = selectIncludingDeleted(rule.getId());
        rejectChanged(existing, rule, "Measure conversion rule application", MeasureUnitConversionRule::getApplicationAlias);
        rejectChanged(existing, rule, "Measure conversion rule scope type", MeasureUnitConversionRule::getScopeType);
        rejectChanged(existing, rule, "Measure conversion rule module", MeasureUnitConversionRule::getModuleAlias);
        rejectChanged(existing, rule, "Measure conversion rule context type", MeasureUnitConversionRule::getContextObjectType);
        rejectChanged(existing, rule, "Measure conversion rule context id", MeasureUnitConversionRule::getContextObjectId);
        rejectChanged(existing, rule, "Measure conversion rule source category", MeasureUnitConversionRule::getFromCategoryAlias);
        rejectChanged(existing, rule, "Measure conversion rule source unit", MeasureUnitConversionRule::getFromUnitCode);
        rejectChanged(existing, rule, "Measure conversion rule target category", MeasureUnitConversionRule::getToCategoryAlias);
        rejectChanged(existing, rule, "Measure conversion rule target unit", MeasureUnitConversionRule::getToUnitCode);
    }

    private boolean isApplicable(MeasureUnitConversionRule rule,
                                 MeasureUnitConversionContext context,
                                 LocalDateTime operatedAt) {
        if (rule.getEffectiveFrom() != null && operatedAt.isBefore(rule.getEffectiveFrom())) {
            return false;
        }
        if (rule.getEffectiveTo() != null && !operatedAt.isBefore(rule.getEffectiveTo())) {
            return false;
        }
        return switch (rule.getScopeType()) {
            case GLOBAL -> true;
            case MODULE -> rule.getModuleAlias().equals(context.moduleAlias());
            case RECORD_CONTEXT -> rule.getModuleAlias().equals(context.moduleAlias())
                    && rule.getContextObjectType().equals(context.contextObjectType())
                    && rule.getContextObjectId().equals(context.contextObjectId());
        };
    }

    private List<MeasureUnitConversionRule> listVisibleRules(MeasureUnitConversionContext context) {
        List<MeasureUnitConversionRule> rules = new java.util.ArrayList<>();
        for (String applicationAlias : applicationCandidates(context.applicationAlias())) {
            Criteria criteria = Criteria.of()
                    .eq("applicationAlias", applicationAlias)
                    .eq("enabled", Boolean.TRUE);
            rules.addAll(list(criteria, new PageRequest(0, Integer.MAX_VALUE),
                    Sort.desc("priority"), Sort.asc(PlatformAbilityFields.SORT_FIELD)));
        }
        if (TenantContext.currentTenantId().isPresent()) {
            for (String applicationAlias : applicationCandidates(context.applicationAlias())) {
                Criteria criteria = Criteria.of()
                        .eq("applicationAlias", applicationAlias)
                        .eq("enabled", Boolean.TRUE);
                rules.addAll(listGlobalRules(criteria));
            }
        }
        return List.copyOf(rules);
    }

    private List<MeasureUnitConversionRule> listGlobalRules(Criteria criteria) {
        try (TenantContext.Scope ignored = TenantContext.system("select global measure unit conversion rules")) {
            return list(criteria, new PageRequest(0, Integer.MAX_VALUE),
                    Sort.desc("priority"), Sort.asc(PlatformAbilityFields.SORT_FIELD))
                    .stream()
                    .filter(rule -> rule.getTenantId() == null || rule.getTenantId().isBlank())
                    .toList();
        }
    }

    private List<String> applicationCandidates(String applicationAlias) {
        if (MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS.equals(applicationAlias)) {
            return List.of(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS);
        }
        return List.of(MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS, applicationAlias);
    }

    private MeasureUnitConversionContext normalizeContext(MeasureUnitConversionContext context) {
        if (context == null) {
            throw new PlatformException("measure conversion context must not be null");
        }
        String applicationAlias = PlatformNameRules.requireApplicationAlias(context.applicationAlias());
        String moduleAlias = context.moduleAlias() == null || context.moduleAlias().isBlank()
                ? null
                : PlatformNameRules.requireModuleAliasInApplication(context.moduleAlias(), applicationAlias);
        String contextObjectType = context.contextObjectType() == null || context.contextObjectType().isBlank()
                ? null
                : requireCode(context.contextObjectType(), "contextObjectType");
        String contextObjectId = context.contextObjectId() == null || context.contextObjectId().isBlank()
                ? null
                : context.contextObjectId();
        return new MeasureUnitConversionContext(applicationAlias, moduleAlias,
                contextObjectType, contextObjectId, context.operatedAt());
    }

    private void validateNoOverlappingRule(MeasureUnitConversionRule rule) {
        for (MeasureUnitConversionRule existing : list(Criteria.of()
                        .eqNullable(StandardEntitySchema.TENANT_ID_FIELD, rule.getTenantId())
                        .eq("applicationAlias", rule.getApplicationAlias()),
                new PageRequest(0, Integer.MAX_VALUE))) {
            if (Objects.equals(existing.getId(), rule.getId())) {
                continue;
            }
            if (!sameScope(existing, rule) || !overlaps(existing, rule)) {
                continue;
            }
            if (sameDirection(existing, rule)) {
                throw new PlatformException("measure conversion rule already exists in overlapping effective window");
            }
            if (reverseDirection(existing, rule)) {
                throw new PlatformException("measure conversion reverse rule conflicts in overlapping effective window");
            }
        }
    }

    private boolean sameScope(MeasureUnitConversionRule left, MeasureUnitConversionRule right) {
        return left.getScopeType() == right.getScopeType()
                && Objects.equals(left.getModuleAlias(), right.getModuleAlias())
                && Objects.equals(left.getContextObjectType(), right.getContextObjectType())
                && Objects.equals(left.getContextObjectId(), right.getContextObjectId());
    }

    private boolean sameDirection(MeasureUnitConversionRule left, MeasureUnitConversionRule right) {
        return Objects.equals(left.getFromCategoryAlias(), right.getFromCategoryAlias())
                && Objects.equals(left.getFromUnitCode(), right.getFromUnitCode())
                && Objects.equals(left.getToCategoryAlias(), right.getToCategoryAlias())
                && Objects.equals(left.getToUnitCode(), right.getToUnitCode());
    }

    private boolean reverseDirection(MeasureUnitConversionRule left, MeasureUnitConversionRule right) {
        return Objects.equals(left.getFromCategoryAlias(), right.getToCategoryAlias())
                && Objects.equals(left.getFromUnitCode(), right.getToUnitCode())
                && Objects.equals(left.getToCategoryAlias(), right.getFromCategoryAlias())
                && Objects.equals(left.getToUnitCode(), right.getFromUnitCode());
    }

    private boolean overlaps(MeasureUnitConversionRule left, MeasureUnitConversionRule right) {
        return before(left.getEffectiveFrom(), right.getEffectiveTo())
                && before(right.getEffectiveFrom(), left.getEffectiveTo());
    }

    private boolean before(LocalDateTime start, LocalDateTime end) {
        return start == null || end == null || start.isBefore(end);
    }

    private String requireCode(String value, String name) {
        return PlatformNameRules.requireCode(value, name);
    }
}
