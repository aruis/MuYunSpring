package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbilityContractTest {
    @Test
    void crudAbilityShouldFillStandardFieldsAndSoftDelete() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization organization = new DemoOrganization("Headquarters", TreeAbility.ROOT_ID);

        String id = service.insert(organization);

        assertThat(id).hasSize(32);
        assertThat(organization.getVersion()).isZero();
        assertThat(organization.getDeleted()).isFalse();
        assertThat(organization.getCreatedAt()).isNotNull();

        assertThat(service.select(id)).isSameAs(organization);

        assertThat(service.delete(id)).isEqualTo(1);
        assertThat(organization.getVersion()).isEqualTo(1);
        assertThat(service.select(id)).isNull();
    }

    @Test
    void crudAbilityShouldIncreaseVersionOnUpdate() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization organization = new DemoOrganization("Versioned", TreeAbility.ROOT_ID);
        service.insert(organization);

        service.update(organization);

        assertThat(organization.getVersion()).isEqualTo(1);
        assertThat(organization.getUpdatedAt()).isAfterOrEqualTo(organization.getCreatedAt());
    }

    @Test
    void treeAbilityShouldResolveChildrenAndAncestors() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization rootChild = new DemoOrganization("Region", TreeAbility.ROOT_ID);
        DemoOrganization leaf = new DemoOrganization("Branch", null);
        DemoOrganization subLeaf = new DemoOrganization("Desk", null);

        String regionId = service.insert(rootChild);
        leaf.setParentId(regionId);
        String leafId = service.insert(leaf);
        subLeaf.setParentId(leafId);
        String subLeafId = service.insert(subLeaf);

        assertThat(service.children(regionId)).containsExactly(leaf);
        assertThat(service.ancestorIds(leafId)).containsExactly(regionId);
        assertThat(service.ancestorIdsAndSelf(leafId)).containsExactly(regionId, leafId);
        assertThat(service.ancestorIds(subLeafId)).containsExactly(regionId, leafId);
        assertThat(service.descendantIds(regionId)).containsExactly(leafId, subLeafId);
        assertThat(service.ancestorIdsAndSelf("missing")).isEmpty();
    }

    @Test
    void treeAbilityShouldRejectCycles() {
        DemoOrganizationService service = new DemoOrganizationService();
        String parentId = service.insert(new DemoOrganization("Parent", TreeAbility.ROOT_ID));
        DemoOrganization child = new DemoOrganization("Child", parentId);
        String childId = service.insert(child);

        DemoOrganization parent = service.select(parentId);
        parent.setParentId(childId);

        assertThatThrownBy(() -> service.update(parent))
                .isInstanceOf(AbilityException.class);
    }

    @Test
    void treeAbilityShouldRejectCorruptDescendantCycles() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization first = new DemoOrganization("First", null);
        DemoOrganization second = new DemoOrganization("Second", null);

        String firstId = service.insert(first);
        second.setParentId(firstId);
        String secondId = service.insert(second);
        first.setParentId(secondId);

        assertThatThrownBy(() -> service.descendantIds(firstId))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("Tree cycle");
    }

    @Test
    void sortAbilityShouldReorderAndMoveRecords() {
        DemoOrganizationService service = new DemoOrganizationService();
        String first = service.insert(new DemoOrganization("First", TreeAbility.ROOT_ID));
        String second = service.insert(new DemoOrganization("Second", TreeAbility.ROOT_ID));
        String third = service.insert(new DemoOrganization("Third", TreeAbility.ROOT_ID));

        service.reorder(List.of(first, second, third));
        service.moveBefore(third, first);

        assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                .containsExactly(third, first, second);

        service.moveAfter(first, second);

        assertThat(service.sortedList(Criteria.of()).stream().map(DemoOrganization::getId))
                .containsExactly(third, second, first);
    }

    @Test
    void sortAbilityShouldRejectCrossParentTreeMove() {
        DemoOrganizationService service = new DemoOrganizationService();
        String firstParent = service.insert(new DemoOrganization("First Parent", TreeAbility.ROOT_ID));
        String secondParent = service.insert(new DemoOrganization("Second Parent", TreeAbility.ROOT_ID));
        String firstChild = service.insert(new DemoOrganization("First Child", firstParent));
        String secondChild = service.insert(new DemoOrganization("Second Child", secondParent));

        assertThatThrownBy(() -> service.moveBefore(firstChild, secondChild))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");
    }

    @Test
    void sortAbilityShouldRejectDuplicateReorderIds() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Duplicate", TreeAbility.ROOT_ID));

        assertThatThrownBy(() -> service.reorder(List.of(id, id)))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void sortAbilityShouldRejectEmptyAndCrossParentReorder() {
        DemoOrganizationService service = new DemoOrganizationService();
        String firstParent = service.insert(new DemoOrganization("First Parent", TreeAbility.ROOT_ID));
        String secondParent = service.insert(new DemoOrganization("Second Parent", TreeAbility.ROOT_ID));
        String firstChild = service.insert(new DemoOrganization("First Child", firstParent));
        String secondChild = service.insert(new DemoOrganization("Second Child", secondParent));

        assertThatThrownBy(() -> service.reorder(List.of()))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("empty");

        assertThatThrownBy(() -> service.reorder(List.of(firstChild, secondChild)))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("same parent");
    }

    @Test
    void referenceAbilityShouldResolveTitles() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Reference Title", TreeAbility.ROOT_ID));
        String secondId = service.insert(new DemoOrganization("Second Title", TreeAbility.ROOT_ID));

        assertThat(service.title(id)).isEqualTo("Reference Title");
        assertThat(service.titles(java.util.List.of(secondId, id)))
                .containsExactly(
                        Map.entry(secondId, "Second Title"),
                        Map.entry(id, "Reference Title")
                );
        assertThat(service.referenceOptions(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .containsExactly(
                        new ReferenceOption(id, "Reference Title"),
                        new ReferenceOption(secondId, "Second Title")
                );
    }

    @Test
    void referenceAbilityShouldHideDeletedTitlesInBatch() {
        DemoOrganizationService service = new DemoOrganizationService();
        String activeId = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));
        String deletedId = service.insert(new DemoOrganization("Deleted", TreeAbility.ROOT_ID));
        service.delete(deletedId);

        assertThat(service.titles(List.of(deletedId, activeId)))
                .containsExactly(Map.entry(activeId, "Active"));
    }

    @Test
    void pageQueryShouldHideSoftDeletedRows() {
        DemoOrganizationService service = new DemoOrganizationService();
        String activeId = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));
        DemoOrganization nullDeleted = new DemoOrganization("Null Deleted", TreeAbility.ROOT_ID);
        String nullDeletedId = service.insert(nullDeleted);
        nullDeleted.setDeleted(null);
        String deletedId = service.insert(new DemoOrganization("Deleted", TreeAbility.ROOT_ID));
        service.delete(deletedId);

        assertThat(service.pageQuery(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .extracting(DemoOrganization::getId)
                .containsExactly(activeId, nullDeletedId);
    }

    @Test
    void abilityQueriesShouldNotMutateCallerCriteria() {
        DemoOrganizationService service = new DemoOrganizationService();
        Criteria criteria = Criteria.of().eq("parentId", TreeAbility.ROOT_ID);

        service.pageQuery(criteria, PageRequest.of(1, 10));
        service.count(criteria);
        service.sortedList(criteria);

        assertThat(criteria.getClauses())
                .extracting(clause -> clause.getField() + ":" + clause.getOperator())
                .containsExactly("parentId:EQ");
    }
}
