package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.PlatformManagedProtectionAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExchangeRateTypeService extends AbstractAbilityService<ExchangeRateType> implements
        SoftDeleteAbility<ExchangeRateType>,
        EnableAbility<ExchangeRateType>,
        SortAbility<ExchangeRateType>,
        ReferenceAbility<ExchangeRateType>,
        CacheAbility<ExchangeRateType>,
        PlatformManagedProtectionAbility<ExchangeRateType>, QueryAbility<ExchangeRateType>
{
    public static final String MODULE_ALIAS = "platform.exchange_rate_type";

    public ExchangeRateTypeService(BaseDao<ExchangeRateType, String> rateTypeDao) {
        super(MODULE_ALIAS, ExchangeRateType.class, rateTypeDao);
    }


    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("code", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                .withTitle("编码").withQuickSearch().withSortable())
                .field(QueryField.of("systemManaged", QueryValueType.BOOLEAN, QueryOperator.EQ).withTitle("系统管理"))
                .field(QueryField.of("tenantId", QueryOperator.EQ, QueryOperator.IN).withTitle("租户"))
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
                .defaultSort(Sort.asc("sortOrder"))
                .defaultSort(Sort.asc("code"))
                .build();
    }
    @Override
    public void beforeInsert(ExchangeRateType rateType) {
        normalizeAndValidate(rateType);
    }

    @Override
    public void beforeUpdate(ExchangeRateType rateType) {
        normalizeAndValidate(rateType);
        validateImmutableIdentity(rateType);
    }

    @Override
    public Criteria sortScope(ExchangeRateType rateType) {
        return Criteria.of().eqNullable(StandardEntitySchema.TENANT_ID_FIELD, rateType.getTenantId());
    }

    @Override
    public void validateSortScope(ExchangeRateType left, ExchangeRateType right) {
        validateSortScopeByFields(left, right,
                "Exchange rate type sort can only move records within the same tenant scope", "tenantId");
    }

    public ExchangeRateType resolveRateType(String rateTypeCode) {
        String code = requireRateTypeCode(rateTypeCode);
        for (ExchangeRateType rateType : visibleRateTypeCandidates(code, false)) {
            return rateType;
        }
        return null;
    }

    public ExchangeRateType requireRateType(String rateTypeCode) {
        ExchangeRateType rateType = resolveRateType(rateTypeCode);
        if (rateType == null) {
            throw new PlatformException("Exchange rate type requires existing type: " + rateTypeCode);
        }
        return rateType;
    }

    public ExchangeRateType requireEnabledRateType(String rateTypeCode) {
        ExchangeRateType rateType = requireRateType(rateTypeCode);
        if (!Boolean.TRUE.equals(rateType.getEnabled())) {
            throw new PlatformException("Exchange rate type is disabled: " + rateTypeCode);
        }
        return rateType;
    }

    public List<ExchangeRateType> listRateTypes(boolean enabledOnly) {
        return listVisibleRateTypes(enabledOnly);
    }

    public List<ExchangeRateType> listVisibleRateTypes(boolean enabledOnly) {
        Map<String, ExchangeRateType> rateTypes = new LinkedHashMap<>();
        if (TenantContext.currentTenantId().isPresent()) {
            listTenantLayer(false).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
            listGlobalLayer(false).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
        } else {
            listGlobalLayer(false).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
        }
        return rateTypes.values().stream()
                .filter(rateType -> !enabledOnly || Boolean.TRUE.equals(rateType.getEnabled()))
                .toList();
    }

    private List<ExchangeRateType> visibleRateTypeCandidates(String rateTypeCode, boolean enabledOnly) {
        return listVisibleRateTypes(enabledOnly).stream()
                .filter(rateType -> Objects.equals(rateType.getCode(), rateTypeCode))
                .toList();
    }

    private List<ExchangeRateType> listTenantLayer(boolean enabledOnly) {
        Criteria criteria = Criteria.of();
        if (enabledOnly) {
            criteria.eq("enabled", Boolean.TRUE);
        }
        return list(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    private List<ExchangeRateType> listGlobalLayer(boolean enabledOnly) {
        try (TenantContext.Scope ignored = TenantContext.system("select global exchange rate types")) {
            Criteria criteria = Criteria.of();
            if (enabledOnly) {
                criteria.eq("enabled", Boolean.TRUE);
            }
            return list(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD))
                    .stream()
                    .filter(rateType -> rateType.getTenantId() == null || rateType.getTenantId().isBlank())
                    .toList();
        }
    }

    private void normalizeAndValidate(ExchangeRateType rateType) {
        rateType.setCode(requireRateTypeCode(rateType.getCode()));
        if (rateType.getSystemManaged() == null) {
            rateType.setSystemManaged(Boolean.FALSE);
        }
        rejectDuplicate(rateType, Criteria.of()
                        .eqNullable(StandardEntitySchema.TENANT_ID_FIELD, rateType.getTenantId())
                        .eq("code", rateType.getCode()),
                "exchange rate type code must be unique within tenant scope: " + rateType.getCode());
    }

    private void validateImmutableIdentity(ExchangeRateType rateType) {
        ExchangeRateType existing = selectIncludingDeleted(rateType.getId());
        rejectChanged(existing, rateType, "Exchange rate type code", ExchangeRateType::getCode);
    }

    private String requireRateTypeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("exchangeRateTypeCode must not be blank");
        }
        String code = CurrencyCodeRules.normalizeRateTypeCode(value);
        if (!CurrencyCodeRules.isRateTypeCode(code)) {
            throw new PlatformException("exchangeRateTypeCode must use upper snake code: " + value);
        }
        return code;
    }
}
