package net.ximatai.muyun.spring.iam.department;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.TenantActiveScopedService;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.util.Preconditions;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DepartmentService extends TenantActiveScopedService<Department> implements
        SoftDeleteAbility<Department>,
        EnableAbility<Department>,
        TreeAbility<Department>,
        ReferenceAbility<Department> {

    public static final String MODULE_ALIAS = "iam.department";

    private final OrganizationService organizationService;

    @Autowired
    public DepartmentService(DepartmentDao departmentDao,
                             ActiveTenantVerifier activeTenantVerifier,
                             OrganizationService organizationService) {
        super(MODULE_ALIAS, Department.class, departmentDao, activeTenantVerifier);
        this.organizationService = organizationService;
    }

    @Override
    public void normalizeBeforeMutation(Department department) {
        department.setOrganizationId(Preconditions.requireText(department.getOrganizationId(), "organizationId"));
        department.setCode(Preconditions.requireText(department.getCode(), "departmentCode"));
    }

    @Override
    public void beforeInsert(Department department) {
        requireActiveOrganization(department.getOrganizationId());
    }

    @Override
    public void beforeUpdate(Department department) {
        requireActiveTenantMutationContext();
        normalizeBeforeMutation(department);
        requireActiveOrganization(department.getOrganizationId());
    }

    @Override
    public Criteria sortScope(Department department) {
        return organizationTreeCriteria(department.getOrganizationId(), department.getParentId());
    }

    @Override
    public void validateSortScope(Department left, Department right) {
        TreeAbility.super.validateSortScope(left, right);
        if (!SortAbility.sameValue(left.getOrganizationId(), right.getOrganizationId())) {
            throw new PlatformException("Department sort can only move records within the same organization");
        }
    }

    @Override
    public void validateTreePlacement(Department department) {
        String id = department.getId();
        String parentId = department.getParentId();
        if (parentId == null || parentId.isBlank() || TreeAbility.ROOT_ID.equals(parentId)) {
            return;
        }
        if (parentId.equals(id)) {
            throw new PlatformException("Tree node cannot use itself as parent: " + id);
        }
        Department parent = select(parentId);
        if (parent == null) {
            throw new PlatformException("Tree node cannot use missing parent: " + parentId);
        }
        if (!SortAbility.sameValue(department.getOrganizationId(), parent.getOrganizationId())) {
            throw new PlatformException("Department parent must belong to the same organization");
        }
        if (ancestorIds(parentId).contains(id)) {
            throw new PlatformException("Tree node cannot move under its descendant: " + id);
        }
    }

    public List<Department> rootDepartments(String organizationId) {
        return departmentChildren(organizationId, TreeAbility.ROOT_ID);
    }

    public List<Department> departmentChildren(String organizationId, String parentId) {
        String validOrganizationId = Preconditions.requireText(organizationId, "organizationId");
        return children(organizationScope(validOrganizationId), parentId);
    }

    public void moveInDepartmentTree(String id, String previousId, String nextId, String parentId) {
        Department moving = select(id);
        if (moving == null) {
            throw new PlatformException("Cannot move missing department: " + id);
        }
        rejectSelfNeighbor(id, previousId);
        rejectSelfNeighbor(id, nextId);
        String targetParentId = resolveMoveParentId(moving, previousId, nextId, parentId);
        if (!SortAbility.sameValue(moving.getParentId(), targetParentId)) {
            moving.setParentId(targetParentId);
            validateTreePlacement(moving);
            update(moving);
        }
        List<Department> siblings = departmentChildren(moving.getOrganizationId(), targetParentId);
        List<String> orderedIds = new ArrayList<>();
        for (Department sibling : siblings) {
            if (!sibling.getId().equals(id)) {
                orderedIds.add(sibling.getId());
            }
        }
        Department previous = previousId == null || previousId.isBlank() ? lastSibling(orderedIds) : select(previousId);
        Department next = nextId == null || nextId.isBlank() ? null : select(nextId);
        if (moveBetween(moving, previous, next)) {
            return;
        }
        orderedIds.add(resolveInsertIndex(orderedIds, previousId, nextId), id);
        reorder(orderedIds);
    }

    private Criteria organizationScope(String organizationId) {
        return Criteria.of().eq("organizationId", Preconditions.requireText(organizationId, "organizationId"));
    }

    private Criteria organizationTreeCriteria(String organizationId, String parentId) {
        return Criteria.of()
                .eq("organizationId", Preconditions.requireText(organizationId, "organizationId"))
                .eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
    }

    private void requireActiveOrganization(String organizationId) {
        organizationService.requireEnabled(organizationId,
                "organization is not active: " + organizationId);
    }

    private void rejectSelfNeighbor(String id, String neighborId) {
        if (neighborId != null && !neighborId.isBlank() && neighborId.equals(id)) {
            throw new PlatformException("Department move neighbor cannot be moving record: " + id);
        }
    }

    private String resolveMoveParentId(Department moving, String previousId, String nextId, String parentId) {
        String targetParentId = normalizeTreeId(parentId);
        if (targetParentId == null) {
            targetParentId = neighborParentId(moving.getOrganizationId(), previousId);
        }
        if (targetParentId == null) {
            targetParentId = neighborParentId(moving.getOrganizationId(), nextId);
        }
        if (targetParentId == null) {
            targetParentId = normalizeTreeId(moving.getParentId());
        }
        if (targetParentId == null) {
            targetParentId = TreeAbility.ROOT_ID;
        }
        requireNeighborInOrganizationParent(moving.getOrganizationId(), previousId, targetParentId);
        requireNeighborInOrganizationParent(moving.getOrganizationId(), nextId, targetParentId);
        return targetParentId;
    }

    private String neighborParentId(String organizationId, String neighborId) {
        if (neighborId == null || neighborId.isBlank()) {
            return null;
        }
        Department neighbor = select(neighborId);
        if (neighbor == null || !SortAbility.sameValue(organizationId, neighbor.getOrganizationId())) {
            throw new PlatformException("Cannot move relative to missing department in organization: " + neighborId);
        }
        return normalizeTreeId(neighbor.getParentId());
    }

    private void requireNeighborInOrganizationParent(String organizationId, String neighborId, String parentId) {
        if (neighborId == null || neighborId.isBlank()) {
            return;
        }
        Department neighbor = select(neighborId);
        if (neighbor == null
                || !SortAbility.sameValue(organizationId, neighbor.getOrganizationId())
                || !SortAbility.sameValue(normalizeTreeId(neighbor.getParentId()), parentId)) {
            throw new PlatformException("Department move neighbor must belong to target parent: " + neighborId);
        }
    }

    private Department lastSibling(List<String> orderedIds) {
        return orderedIds.isEmpty() ? null : select(orderedIds.getLast());
    }

    private int resolveInsertIndex(List<String> orderedIds, String previousId, String nextId) {
        if (previousId != null && !previousId.isBlank()) {
            int previousIndex = orderedIds.indexOf(previousId);
            if (previousIndex < 0) {
                throw new PlatformException("Cannot move after missing previous department: " + previousId);
            }
            return previousIndex + 1;
        }
        if (nextId != null && !nextId.isBlank()) {
            int nextIndex = orderedIds.indexOf(nextId);
            if (nextIndex < 0) {
                throw new PlatformException("Cannot move before missing next department: " + nextId);
            }
            return nextIndex;
        }
        return orderedIds.size();
    }

    private String normalizeTreeId(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
