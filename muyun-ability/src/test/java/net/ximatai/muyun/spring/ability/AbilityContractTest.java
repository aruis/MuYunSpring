package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(service.select(id)).isNull();
    }

    @Test
    void treeAbilityShouldResolveChildrenAndAncestors() {
        DemoOrganizationService service = new DemoOrganizationService();
        DemoOrganization rootChild = new DemoOrganization("Region", TreeAbility.ROOT_ID);
        DemoOrganization leaf = new DemoOrganization("Branch", null);

        String regionId = service.insert(rootChild);
        leaf.setParentId(regionId);
        String leafId = service.insert(leaf);

        assertThat(service.children(regionId)).containsExactly(leaf);
        assertThat(service.ancestorIds(leafId)).containsExactly(regionId);
        assertThat(service.ancestorIdsAndSelf(leafId)).containsExactly(regionId, leafId);
    }

    @Test
    void referenceAbilityShouldResolveTitles() {
        DemoOrganizationService service = new DemoOrganizationService();
        String id = service.insert(new DemoOrganization("Reference Title", TreeAbility.ROOT_ID));

        assertThat(service.title(id)).isEqualTo("Reference Title");
        assertThat(service.titles(java.util.List.of(id))).isEqualTo(Map.of(id, "Reference Title"));
    }

    @Test
    void pageQueryShouldHideSoftDeletedRows() {
        DemoOrganizationService service = new DemoOrganizationService();
        String activeId = service.insert(new DemoOrganization("Active", TreeAbility.ROOT_ID));
        String deletedId = service.insert(new DemoOrganization("Deleted", TreeAbility.ROOT_ID));
        service.delete(deletedId);

        assertThat(service.pageQuery(Criteria.of(), PageRequest.of(1, 10)).getRecords())
                .extracting(DemoOrganization::getId)
                .containsExactly(activeId);
    }
}
