package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrganizationService extends AbstractAbilityService<Organization> implements
        SoftDeleteAbility<Organization>,
        EnableAbility<Organization>,
        TreeAbility<Organization>,
        ReferenceAbility<Organization> {

    public static final String MODULE_ALIAS = "iam.organization";

    private final TenantService tenantService;

    @Autowired
    public OrganizationService(OrganizationDao organizationDao, TenantService tenantService) {
        super(MODULE_ALIAS, Organization.class, organizationDao);
        this.tenantService = tenantService;
    }

    OrganizationService(OrganizationDao organizationDao) {
        this(organizationDao, null);
    }

    @Override
    public void beforePrepareInsert(Organization organization) {
        requireActiveTenantContext();
        normalizeOrganization(organization);
    }

    @Override
    public void beforeUpdate(Organization organization) {
        requireActiveTenantContext();
        normalizeOrganization(organization);
    }

    @Override
    public void beforeDelete(String id) {
        requireActiveTenantContext();
    }

    private void normalizeOrganization(Organization organization) {
        organization.setCode(Preconditions.requireText(organization.getCode(), "organizationCode"));
    }

    private String requireActiveTenantContext() {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("Organization management requires tenant context"));
        if (tenantService != null) {
            tenantService.requireActiveTenant(tenantId);
        }
        return tenantId;
    }
}
