package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class TenantService extends AbstractAbilityService<Tenant> implements
        SoftDeleteAbility<Tenant>,
        EnableAbility<Tenant>,
        SortAbility<Tenant> {

    public static final String MODULE_ALIAS = "iam.tenant";

    public TenantService(TenantDao tenantDao) {
        super(MODULE_ALIAS, Tenant.class, tenantDao);
    }

    @Override
    public Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.andGroup(group -> group
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE)
                .orIsNull(StandardEntitySchema.DELETED_FIELD));
        return scoped;
    }

    @Override
    public void beforePrepareInsert(Tenant tenant) {
        requireSystemContext();
        normalizeTenant(tenant);
    }

    @Override
    public void beforeInsert(Tenant tenant) {
        requireSystemContext();
        normalizeTenant(tenant);
    }

    @Override
    public void beforeUpdate(Tenant tenant) {
        requireSystemContext();
        normalizeTenant(tenant);
    }

    @Override
    public void beforeDelete(String id) {
        requireSystemContext();
    }

    public Tenant requireActiveTenant(String tenantAlias) {
        String alias = requireTenantAlias(tenantAlias);
        Tenant tenant = select(alias);
        if (tenant == null || !Boolean.TRUE.equals(tenant.getEnabled())) {
            throw new PlatformException("Tenant is not active: " + alias);
        }
        return tenant;
    }

    private void normalizeTenant(Tenant tenant) {
        tenant.setAlias(requireTenantAlias(tenant.getAlias()));
        tenant.setTenantId(null);
    }

    private String requireTenantAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "tenantAlias");
    }

    private void requireSystemContext() {
        if (!TenantContext.isSystem()) {
            throw new PlatformException("Tenant management requires system context");
        }
    }
}
