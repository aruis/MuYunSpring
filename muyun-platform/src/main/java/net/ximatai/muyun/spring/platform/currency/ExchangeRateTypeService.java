package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CacheAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
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
        CacheAbility<ExchangeRateType> {
    public static final String MODULE_ALIAS = "platform.exchange_rate_type";

    public ExchangeRateTypeService(BaseDao<ExchangeRateType, String> rateTypeDao) {
        super(MODULE_ALIAS, ExchangeRateType.class, rateTypeDao);
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
        return Criteria.of().eq(StandardEntitySchema.TENANT_ID_FIELD, rateType.getTenantId());
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
            listTenantLayer(enabledOnly).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
            listGlobalLayer(enabledOnly).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
        } else {
            listTenantLayer(enabledOnly).forEach(rateType -> rateTypes.putIfAbsent(rateType.getCode(), rateType));
        }
        return List.copyOf(rateTypes.values());
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
                        .eq(StandardEntitySchema.TENANT_ID_FIELD, rateType.getTenantId())
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
        String code = value.trim().toUpperCase();
        if (!code.matches("[A-Z][A-Z0-9_]{0,63}")) {
            throw new PlatformException("exchangeRateTypeCode must use upper snake code: " + value);
        }
        return code;
    }
}
