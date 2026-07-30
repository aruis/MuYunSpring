package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.SystemManagedAbility;
import net.ximatai.muyun.spring.ability.child.ChildRelation;
import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantCreationProvisioner;
import net.ximatai.muyun.spring.common.exception.PlatformErrorCodes;
import net.ximatai.muyun.spring.common.exception.PlatformErrors;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TenantService extends AbstractAbilityService<Tenant> implements
        SystemManagedAbility<Tenant>,
        GlobalScopedAbility<Tenant>,
        RecycleBinAbility<Tenant>,
        DeletionRecoveryAbility<Tenant>,
        EnableAbility<Tenant>,
        SortAbility<Tenant>,
        ReferenceAbility<Tenant>,
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
    public void beforeInsert(Tenant tenant) {
        Tenant existing = selectIgnoreSoftDelete(tenant.getId());
        if (existing != null && Boolean.TRUE.equals(existing.getDeleted())) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("resourceModuleAlias", MODULE_ALIAS);
            details.put("resourceRecordId", existing.getId());
            if (existing.getDeletedAt() != null) {
                details.put("deletedAt", existing.getDeletedAt());
            }
            details.put("recoveryAvailable", Boolean.TRUE);
            throw PlatformErrors.conflict(PlatformErrorCodes.RESOURCE_SOFT_DELETED_CONFLICT,
                    "Tenant alias is retained by a soft-deleted tenant; restore it from the recycle bin before creating it again",
                    details);
        }
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
    public void beforeRecycleBinQuery() {
        requireSystemMutationContext();
    }

    @Override
    public Criteria recycleBinCriteria(Criteria criteria) {
        return globalCriteria(criteria);
    }

    @Override
    public void beforeRecycleBinRestore() {
        requireSystemMutationContext();
    }

    @Override
    public void verifyActiveTenant(String tenantId) {
        requireActiveTenant(tenantId);
    }

    @Override
    public String getDeletionEntityAlias() {
        return "tenant";
    }

    @Override
    public List<ChildRelation<? extends EntityContract, Tenant>> childRelations() {
        return tenantApplicationService == null
                ? List.of()
                : List.of(childRelation(Tenant.class, tenantApplicationService));
    }

    /** Tenant applications are optional in lightweight IAM runtime assemblies. */
    @Override
    public boolean usesAutomaticChildRelations() {
        return false;
    }

    private String requireTenantAlias(String alias) {
        return PlatformNameRules.requireIdentifier(alias, "tenantAlias");
    }
}
