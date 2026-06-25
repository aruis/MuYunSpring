package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.SystemManagedAbility;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService extends AbstractAbilityService<Tenant> implements
        SystemManagedAbility<Tenant>,
        GlobalScopedAbility<Tenant>,
        EnableAbility<Tenant>,
        SortAbility<Tenant>,
        ActiveTenantVerifier,
        InitialDataAbility<Tenant> {

    public static final String MODULE_ALIAS = "iam.tenant";
    public static final String PLATFORM_TENANT_ID = "platform";
    public static final String PLATFORM_TENANT_TITLE = "平台租户";

    public TenantService(TenantDao tenantDao) {
        super(MODULE_ALIAS, Tenant.class, tenantDao);
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.defaults().order(30);
    }

    @Override
    public List<Tenant> initialData() {
        Tenant tenant = new Tenant();
        tenant.setId(PLATFORM_TENANT_ID);
        tenant.setTitle(PLATFORM_TENANT_TITLE);
        tenant.setEnabled(Boolean.TRUE);
        tenant.setSortOrder(1);
        return List.of(tenant);
    }

    @Override
    public void normalizeBeforeMutation(Tenant tenant) {
        tenant.setAlias(requireTenantAlias(tenant.getAlias()));
        tenant.setTenantId(null);
    }

    @Override
    public void beforeUpdate(Tenant tenant) {
        requireSystemMutationContext();
        normalizeBeforeMutation(tenant);
        if (PLATFORM_TENANT_ID.equals(tenant.getAlias()) && !Boolean.TRUE.equals(tenant.getEnabled())) {
            throw new PlatformException("platform tenant cannot be disabled");
        }
    }

    @Override
    public void beforeDelete(String id) {
        requireSystemMutationContext();
        if (PLATFORM_TENANT_ID.equals(id)) {
            throw new PlatformException("platform tenant cannot be deleted");
        }
    }

    public Tenant requireActiveTenant(String tenantAlias) {
        String alias = requireTenantAlias(tenantAlias);
        return requireEnabled(alias, "Tenant is not active: " + alias);
    }

    @Override
    public void verifyActiveTenant(String tenantId) {
        requireActiveTenant(tenantId);
    }

    private String requireTenantAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "tenantAlias");
    }
}
