package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/iam.tenant")
public class TenantWebController extends WebSupport<TenantService> implements
        CrudWeb<Tenant, TenantService>,
        EnableWeb<Tenant, TenantService> {
    @Override
    public <T> T webScope(java.util.function.Supplier<T> action) {
        try (TenantContext.Scope ignored = TenantContext.system()) {
            return action.get();
        }
    }
}
