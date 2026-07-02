package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.DataScopeFieldMappingAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeFieldMapping;
import net.ximatai.muyun.spring.common.platform.OrganizationHierarchyService;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.common.util.Preconditions;
import jakarta.inject.Inject;
import net.ximatai.muyun.spring.common.di.ObjectProvider;
import jakarta.enterprise.context.Dependent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Dependent
public class OrganizationService extends TenantActiveScopedService<Organization> implements
        SoftDeleteAbility<Organization>,
        EnableAbility<Organization>,
        TreeAbility<Organization>,
        ReferenceAbility<Organization>,
        DataScopeAbility<Organization>,
        DataScopeFieldMappingAbility,
        OrganizationHierarchyService {

    public static final String MODULE_ALIAS = "iam.organization";
    private static final DataScopeFieldMapping DATA_SCOPE_FIELD_MAPPING = DataScopeFieldMapping.of(null, "id", null);
    private final Supplier<DataScopeCriteriaService> dataScopeCriteriaService;

    public OrganizationService(OrganizationDao organizationDao, ActiveTenantVerifier activeTenantVerifier) {
        this(organizationDao, activeTenantVerifier, Optional.empty());
    }

    @Inject
    public OrganizationService(OrganizationDao organizationDao,
                               TenantService activeTenantVerifier,
                               ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Organization.class, organizationDao, activeTenantVerifier);
        this.dataScopeCriteriaService = () -> dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new);
    }

    public OrganizationService(OrganizationDao organizationDao,
                               ActiveTenantVerifier activeTenantVerifier,
                               Optional<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Organization.class, organizationDao, activeTenantVerifier);
        Optional<DataScopeCriteriaService> criteriaService = dataScopeCriteriaService == null
                ? Optional.empty()
                : dataScopeCriteriaService;
        this.dataScopeCriteriaService = () -> criteriaService
                .<DataScopeCriteriaService>map(service -> service)
                .orElseGet(AllowAllDataScopeCriteriaService::new);
    }

    @Override
    public DataScopeCriteriaService getDataScopeCriteriaService() {
        return dataScopeCriteriaService.get();
    }

    @Override
    public DataScopeFieldMapping dataScopeFieldMapping() {
        return DATA_SCOPE_FIELD_MAPPING;
    }

    @Override
    public void normalizeBeforeMutation(Organization organization) {
        organization.setCode(Preconditions.requireText(organization.getCode(), "organizationCode"));
    }

    @Override
    public List<String> organizationIdsFromSelfToRoot(String organizationId) {
        return ancestorIdsAndSelf(organizationId).reversed();
    }

}
