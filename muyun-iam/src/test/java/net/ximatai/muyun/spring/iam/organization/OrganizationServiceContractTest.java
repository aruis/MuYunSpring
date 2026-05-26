package net.ximatai.muyun.spring.iam.organization;

import net.ximatai.muyun.spring.ability.TreeAbility;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrganizationServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        OrganizationService service = new OrganizationService(null);

        assertThat(service.getModuleAlias()).isEqualTo("iam.organization");
    }

    @Test
    void shouldFillOrganizationDefaultsThroughCrudAbility() {
        OrganizationDao dao = mock(OrganizationDao.class);
        when(dao.insert(any())).thenReturn("org-1");
        OrganizationService service = new OrganizationService(dao);
        Organization organization = new Organization();

        service.insert(organization);

        assertThat(organization.getEnabled()).isTrue();
        assertThat(organization.getParentId()).isEqualTo(TreeAbility.ROOT_ID);
    }
}
