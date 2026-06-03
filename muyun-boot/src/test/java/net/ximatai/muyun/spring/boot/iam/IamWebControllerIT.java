package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.boot.web.TenantContextWebFilter;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        TenantWebController.class,
        OrganizationWebController.class
})
@Import({
        TenantContextWebFilter.class,
        IamWebExceptionHandler.class
})
class IamWebControllerIT {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private OrganizationService organizationService;

    @Test
    void shouldUseInjectedServiceAndTenantHeaderInRealMvcContext() throws Exception {
        Organization organization = new Organization();
        organization.setId("org-1");
        organization.setCode("HQ");
        organization.setTitle("Headquarters");
        organization.setParentId(TreeAbility.ROOT_ID);
        when(organizationService.children(TreeAbility.ROOT_ID)).thenReturn(List.of(organization));

        mvc.perform(post("/iam.organization/tree")
                        .header(TenantContextWebFilter.TENANT_HEADER, "tenant_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("org-1"));
    }

    @Test
    void shouldApplyAdviceWhenTenantHeaderIsMissingInRealMvcContext() throws Exception {
        mvc.perform(post("/iam.organization/tree"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IAM_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("iam.organization requires tenant context"));
    }
}
