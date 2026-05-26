package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        OrganizationService service = new OrganizationService(null);

        assertThat(service.getModuleAlias()).isEqualTo("iam.organization");
    }

    @Test
    void shouldFillOrganizationDefaultsBeforeInsert() {
        OrganizationService service = new OrganizationService(null);
        Organization organization = new Organization();

        service.beforeInsert(organization);

        assertThat(organization.getEnabled()).isTrue();
        assertThat(organization.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
    }
}
