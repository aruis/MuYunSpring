package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.tenant", title = "租户管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 10)
@RequestMapping("/iam.tenant")
public class TenantWebController extends WebSupport<TenantService> implements
        CrudWeb<Tenant, TenantService>,
        SystemScope<TenantService>,
        StaticModuleUiContributor {
    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(TenantService.MODULE_ALIAS)
                .typedTextConfirmation("delete", "alias")
                .build();
    }
}
