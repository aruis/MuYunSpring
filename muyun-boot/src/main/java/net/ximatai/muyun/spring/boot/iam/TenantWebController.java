package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.RecycleBinWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.platform.deletion.RecycleBinFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.tenant", title = "租户管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 10)
@RequestMapping("/iam.tenant")
public class TenantWebController extends WebSupport<TenantService> implements
        CrudWeb<Tenant, TenantService>,
        EnableWeb<Tenant, TenantService>,
        RecycleBinWeb<Tenant, TenantService>,
        SortWeb<Tenant, TenantService>,
        SystemScope<TenantService> {
    private RecycleBinFacade recycleBinFacade;

    @Autowired
    void setRecycleBinFacade(RecycleBinFacade recycleBinFacade) {
        this.recycleBinFacade = recycleBinFacade;
    }

    @Override
    public RecycleBinFacade recycleBinFacade() {
        if (recycleBinFacade == null) {
            throw new IllegalStateException("RecycleBinFacade must be configured for recycle-bin endpoints");
        }
        return recycleBinFacade;
    }
}
