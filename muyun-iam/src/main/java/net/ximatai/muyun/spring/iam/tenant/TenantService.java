package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.SystemManagedAbility;
import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantCreationProvisioner;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantService extends AbstractAbilityService<Tenant> implements
        SystemManagedAbility<Tenant>,
        GlobalScopedAbility<Tenant>,
        EnableAbility<Tenant>,
        SortAbility<Tenant>,
        ChildrenAbility<Tenant>,
        ActiveTenantVerifier {

    public static final String MODULE_ALIAS = "iam.tenant";
    private final ObjectProvider<TenantCreationProvisioner> creationProvisioners;
    private final TenantApplicationService tenantApplicationService;

    public TenantService(TenantDao tenantDao) {
        super(MODULE_ALIAS, Tenant.class, tenantDao);
        this.creationProvisioners = null;
        this.tenantApplicationService = null;
    }

    public TenantService(TenantDao tenantDao, ObjectProvider<TenantCreationProvisioner> creationProvisioners) {
        super(MODULE_ALIAS, Tenant.class, tenantDao);
        this.creationProvisioners = creationProvisioners;
        this.tenantApplicationService = null;
    }

    @Autowired
    public TenantService(TenantDao tenantDao,
                         ObjectProvider<TenantCreationProvisioner> creationProvisioners,
                         TenantApplicationService tenantApplicationService) {
        super(MODULE_ALIAS, Tenant.class, tenantDao);
        this.creationProvisioners = creationProvisioners;
        this.tenantApplicationService = tenantApplicationService;
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
    }

    @Override
    public void beforeDelete(String id) {
        requireSystemMutationContext();
    }

    @Override
    public void afterInsert(String id, Tenant tenant) {
        provisionTenant(id);
    }

    public void provisionTenant(String tenantId) {
        if (creationProvisioners != null) {
            creationProvisioners.orderedStream().forEach(provisioner -> provisioner.afterTenantCreated(tenantId));
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

    @Override
    public List<ChildRelation<? extends EntityContract, Tenant>> childRelations() {
        if (tenantApplicationService == null) {
            return List.of();
        }
        return List.of(tenantApplicationService.<Tenant>toChildRelation(TenantApplication::setTenantId,
                "tenantId", tenant -> null).autoDeleteWithParent());
    }

    private String requireTenantAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "tenantAlias");
    }
}
