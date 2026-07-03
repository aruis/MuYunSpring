package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PlatformStaticModule(application = "iam", alias = "iam.tenant", title = "租户管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 10)
@Path("/iam.tenant")
public class TenantWebController extends WebSupport<TenantService> implements
        CrudWeb<Tenant, TenantService>,
        EnableWeb<Tenant, TenantService>,
        SortWeb<Tenant, TenantService>,
        SystemScope<TenantService> {
}
