package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam.organization")
public class OrganizationWebController extends WebSupport<OrganizationService> implements
        CrudWeb<Organization, OrganizationService>,
        EnableWeb<Organization, OrganizationService>,
        TreeWeb<Organization, OrganizationService> {
    @Override
    public <T> T webScope(java.util.function.Supplier<T> action) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("iam.organization requires tenant context"));
        service().verifyActiveTenant(tenantId);
        return action.get();
    }
}
