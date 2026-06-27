package net.ximatai.muyun.spring.platform.currency;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryDescriptors;

@Service
public class TenantCurrencySettingService extends AbstractAbilityService<TenantCurrencySetting> implements
        SoftDeleteAbility<TenantCurrencySetting>,
        QueryAbility<TenantCurrencySetting> {
    public static final String MODULE_ALIAS = "platform.tenant_currency_setting";

    private final CurrencyService currencyService;

    public TenantCurrencySettingService(BaseDao<TenantCurrencySetting, String> settingDao,
                                        CurrencyService currencyService) {
        super(MODULE_ALIAS, TenantCurrencySetting.class, settingDao);
        this.currencyService = currencyService;
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptors.simple(MODULE_ALIAS, java.util.List.of("id", "tenantId", "baseCurrencyCode", "title", "createdAt", "updatedAt"),
                net.ximatai.muyun.database.core.orm.Sort.asc("tenantId"));
    }

    @Override
    public void beforeInsert(TenantCurrencySetting setting) {
        normalizeAndValidate(setting);
    }

    @Override
    public void beforeUpdate(TenantCurrencySetting setting) {
        normalizeAndValidate(setting);
        validateImmutableIdentity(setting);
    }

    public TenantCurrencySetting currentTenantSetting() {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("tenant currency setting requires tenant context"));
        return findOne(Criteria.of().eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId));
    }

    public String requireCurrentBaseCurrencyCode() {
        TenantCurrencySetting setting = currentTenantSetting();
        if (setting == null) {
            throw new PlatformException("tenant base currency is not configured");
        }
        return setting.getBaseCurrencyCode();
    }

    private void normalizeAndValidate(TenantCurrencySetting setting) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("tenant currency setting requires tenant context"));
        setting.setTenantId(tenantId);
        Currency currency = currencyService.requireEnabledCurrency(setting.getBaseCurrencyCode());
        setting.setBaseCurrencyCode(currency.getCode());
        rejectDuplicate(setting, Criteria.of().eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId),
                "tenant base currency setting must be unique within tenant");
    }

    private void validateImmutableIdentity(TenantCurrencySetting setting) {
        TenantCurrencySetting existing = selectIncludingDeleted(setting.getId());
        rejectChanged(existing, setting, "Tenant currency setting tenant", TenantCurrencySetting::getTenantId);
    }
}
