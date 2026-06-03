package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserProvider;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TenantWebController.class,
        OrganizationWebController.class
})
@Import({
        CurrentUserWebFilter.class,
        IamWebExceptionHandler.class
})
class IamWebControllerIT {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void shouldUseInjectedServiceAndCurrentUserTenantInRealMvcContext() throws Exception {
        Organization organization = new Organization();
        organization.setId("org-1");
        organization.setCode("HQ");
        organization.setTitle("Headquarters");
        organization.setParentId(TreeAbility.ROOT_ID);
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));
        when(organizationService.children(TreeAbility.ROOT_ID)).thenReturn(List.of(organization));
        when(organizationService.children("org-1")).thenReturn(List.of());

        mvc.perform(post("/iam.organization/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].record.id").value("org-1"))
                .andExpect(jsonPath("$.records[0].children").isArray());
    }

    @Test
    void shouldBindTreeSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant_a")));

        mvc.perform(post("/iam.organization/sort/org-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"org-0","parentId":"root"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(organizationService).moveInTree("org-1", "org-0", null, TreeAbility.ROOT_ID);
    }

    @Test
    void shouldBindPlainSortEndpointInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser())
                .thenReturn(Optional.of(CurrentUser.systemUser("admin", "Admin")));

        mvc.perform(post("/iam.tenant/sort/tenant-1")
                        .contentType("application/json")
                        .content("""
                                {"previousId":"tenant-0"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        verify(tenantService).moveAfter("tenant-1", "tenant-0");
    }

    @Test
    void shouldApplyAdviceWhenCurrentUserTenantIsMissingInRealMvcContext() throws Exception {
        when(currentUserProvider.currentUser()).thenReturn(Optional.empty());

        mvc.perform(post("/iam.organization/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("iam.organization requires tenant context"));
    }
}
