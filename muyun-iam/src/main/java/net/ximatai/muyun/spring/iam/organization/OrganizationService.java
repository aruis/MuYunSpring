package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataOptions;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataPhase;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.platform.OrganizationHierarchyService;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationService extends TenantActiveScopedService<Organization> implements
        SoftDeleteAbility<Organization>,
        EnableAbility<Organization>,
        TreeAbility<Organization>,
        ReferenceAbility<Organization>,
        InitialDataAbility<Organization>,
        OrganizationHierarchyService {

    public static final String MODULE_ALIAS = "iam.organization";
    public static final String PLATFORM_ROOT_ORGANIZATION_ID = "platform.organization.root";
    public static final String PLATFORM_ROOT_ORGANIZATION_CODE = "PLATFORM";
    public static final String PLATFORM_ROOT_ORGANIZATION_TITLE = "平台组织";

    @Autowired
    public OrganizationService(OrganizationDao organizationDao, ActiveTenantVerifier activeTenantVerifier) {
        super(MODULE_ALIAS, Organization.class, organizationDao, activeTenantVerifier);
    }

    @Override
    public void normalizeBeforeMutation(Organization organization) {
        organization.setCode(Preconditions.requireText(organization.getCode(), "organizationCode"));
    }

    @Override
    public InitialDataOptions initialDataOptions() {
        return InitialDataOptions.defaults()
                .phase(InitialDataPhase.TENANT_INITIAL_DATA)
                .order(41)
                .tenant(TenantService.PLATFORM_TENANT_ID);
    }

    @Override
    public List<Organization> initialData() {
        Organization organization = new Organization();
        organization.setId(PLATFORM_ROOT_ORGANIZATION_ID);
        organization.setCode(PLATFORM_ROOT_ORGANIZATION_CODE);
        organization.setTitle(PLATFORM_ROOT_ORGANIZATION_TITLE);
        organization.setParentId(TreeAbility.ROOT_ID);
        organization.setEnabled(Boolean.TRUE);
        organization.setSortOrder(1);
        return List.of(organization);
    }

    @Override
    public List<String> organizationIdsFromSelfToRoot(String organizationId) {
        return ancestorIdsAndSelf(organizationId).reversed();
    }

}
