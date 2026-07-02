package net.ximatai.muyun.spring.iam.department;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.DataScopeFieldMappingAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.TenantStandardBusinessService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeFieldMapping;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class DepartmentService extends TenantStandardBusinessService<Department> implements
        SoftDeleteAbility<Department>,
        EnableAbility<Department>,
        TreeAbility<Department>,
        DataScopeAbility<Department>,
        DataScopeFieldMappingAbility,
        ReferenceAbility<Department> {

    public static final String MODULE_ALIAS = "iam.department";
    private static final DataScopeFieldMapping DATA_SCOPE_FIELD_MAPPING =
            DataScopeFieldMapping.of(null, "organizationId", "id");

    private final OrganizationService organizationService;
    private final Supplier<DataScopeCriteriaService> dataScopeCriteriaService;

    public DepartmentService(DepartmentDao departmentDao,
                             ActiveTenantVerifier activeTenantVerifier,
                             OrganizationService organizationService) {
        this(departmentDao, activeTenantVerifier, organizationService, Optional.empty());
    }

    @Autowired
    public DepartmentService(DepartmentDao departmentDao,
                             ActiveTenantVerifier activeTenantVerifier,
                             OrganizationService organizationService,
                             ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Department.class, departmentDao, activeTenantVerifier);
        this.organizationService = organizationService;
        this.dataScopeCriteriaService = () -> dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new);
    }

    public DepartmentService(DepartmentDao departmentDao,
                             ActiveTenantVerifier activeTenantVerifier,
                             OrganizationService organizationService,
                             Optional<DataScopeCriteriaService> dataScopeCriteriaService) {
        super(MODULE_ALIAS, Department.class, departmentDao, activeTenantVerifier);
        this.organizationService = organizationService;
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
    public void normalizeBeforeMutation(Department department) {
        department.setOrganizationId(Preconditions.requireText(department.getOrganizationId(), "organizationId"));
        department.setCode(Preconditions.requireText(department.getCode(), "departmentCode"));
    }

    @Override
    protected void validateBeforeSave(Department department) {
        requireActiveOrganization(department.getOrganizationId());
    }

    @Override
    public Criteria sortScope(Department department) {
        return scopedTreeCriteria(department, "organizationId");
    }

    @Override
    public void validateSortScope(Department left, Department right) {
        validateTreeSortScopeByFields(left, right,
                "Department sort can only move records within the same organization", "organizationId");
    }

    @Override
    public void validateTreePlacement(Department department) {
        validateTreePlacementInScope(department, organizationScope(department.getOrganizationId()),
                "Department parent must belong to the same organization");
    }

    public List<Department> rootDepartments(String organizationId) {
        return departmentChildren(organizationId, TreeAbility.ROOT_ID);
    }

    public List<Department> departmentChildren(String organizationId, String parentId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        return children(organizationScope(validOrganizationId), parentId);
    }

    public List<Department> departmentChildrenForAction(PlatformAction action, String organizationId, String parentId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        String validParentId = Preconditions.requireText(parentId, "parentId");
        Criteria criteria = scopedTreeCriteria(organizationScope(validOrganizationId), validParentId);
        return listForAction(action, criteria, Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    public List<String> selfAndDescendantIds(String organizationId, String departmentId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        String validDepartmentId = Preconditions.requireText(departmentId, "departmentId");
        if (selectInScope(organizationScope(validOrganizationId), validDepartmentId) == null) {
            return List.of();
        }
        ArrayList<String> ids = new ArrayList<>();
        ids.add(validDepartmentId);
        collectDepartmentDescendantIds(validOrganizationId, validDepartmentId, ids, new LinkedHashSet<>());
        return List.copyOf(ids);
    }

    public void moveInDepartmentTree(String id, String previousId, String nextId, String parentId) {
        Department moving = select(id);
        if (moving == null) {
            throw new PlatformException("Cannot move missing department: " + id);
        }
        moveInTree(organizationScope(moving.getOrganizationId()), id, previousId, nextId, parentId);
    }

    private void collectDepartmentDescendantIds(String organizationId,
                                                String parentId,
                                                List<String> result,
                                                Set<String> visited) {
        if (!visited.add(parentId)) {
            throw new PlatformException("Department tree cycle detected while resolving descendants: " + parentId);
        }
        for (Department child : departmentChildren(organizationId, parentId)) {
            if (visited.contains(child.getId())) {
                throw new PlatformException("Department tree cycle detected while resolving descendants: " + parentId);
            }
            result.add(child.getId());
            collectDepartmentDescendantIds(organizationId, child.getId(), result, visited);
        }
        visited.remove(parentId);
    }

    private Criteria organizationScope(String organizationId) {
        return Criteria.of().eq("organizationId", Preconditions.requireText(organizationId, "organizationId"));
    }

    private void requireActiveOrganization(String organizationId) {
        organizationService.requireEnabled(organizationId,
                "organization is not active: " + organizationId);
    }
}
