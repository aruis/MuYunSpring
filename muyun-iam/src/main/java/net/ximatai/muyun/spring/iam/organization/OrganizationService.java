package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
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
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
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

    @Autowired
    public OrganizationService(OrganizationDao organizationDao,
                               ActiveTenantVerifier activeTenantVerifier,
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

    public List<Organization> organizationChildrenForAction(PlatformAction action,
                                                            String tenantId,
                                                            String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        String normalizedTenantId = normalizeTenantScope(tenantId);
        if (!TreeAbility.ROOT_ID.equals(parentId)
                && organizationForAction(action, normalizedTenantId, parentId) == null) {
            return List.of();
        }
        Criteria criteria = organizationTenantScope(normalizedTenantId)
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
        return listForAction(action, criteria, PageRequest.of(1, Integer.MAX_VALUE),
                Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public Organization organizationForAction(PlatformAction action, String tenantId, String organizationId) {
        if (organizationId == null || organizationId.isBlank()) {
            return null;
        }
        List<Organization> records = listForAction(action,
                organizationTenantScope(normalizeTenantScope(tenantId))
                        .eq(StandardEntitySchema.ID_FIELD, organizationId),
                PageRequest.of(1, 1));
        return records.isEmpty() ? null : records.getFirst();
    }

    public void moveInOrganizationTree(String tenantId, String id, String previousId, String nextId, String parentId) {
        moveInTree(organizationTenantScope(normalizeTenantScope(tenantId)), id, previousId, nextId, parentId);
    }

    @Override
    public List<String> organizationIdsFromSelfToRoot(String organizationId) {
        return ancestorIdsAndSelf(organizationId).reversed();
    }

    private Criteria organizationTenantScope(String tenantId) {
        Criteria criteria = Criteria.of();
        if (tenantId != null) {
            criteria.eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId);
        }
        return criteria;
    }

    private String normalizeTenantScope(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return tenantId.trim();
    }

}
