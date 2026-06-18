package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataContext;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataContribution;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataRecord;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

public class PlatformMenuRegistrar implements InitialDataContribution {
    private final MenuService menuService;
    private final ApplicationContext applicationContext;

    public PlatformMenuRegistrar(MenuService menuService,
                                 ApplicationContext applicationContext) {
        this.menuService = menuService;
        this.applicationContext = applicationContext;
    }

    @Override
    public String name() {
        return "platform.menu-contributions";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void contribute(InitialDataContext context) {
        registerContributedMenus(context, PlatformAdminMenuInitialDataContribution.ADMIN_SCHEME_ID);
    }

    private void registerContributedMenus(InitialDataContext context, String schemeId) {
        for (String beanName : applicationContext.getBeanNamesForAnnotation(PlatformMenu.class)) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            PlatformMenu menu = AnnotationUtils.findAnnotation(beanClass, PlatformMenu.class);
            if (menu == null) {
                continue;
            }
            PlatformStaticModule module = AnnotationUtils.findAnnotation(beanClass, PlatformStaticModule.class);
            if (module == null) {
                throw new IllegalStateException("@PlatformMenu requires @PlatformStaticModule: " + beanClass.getName());
            }
            ensureModuleMenu(context, schemeId, module, menu);
        }
    }

    private void ensureModuleMenu(InitialDataContext context, String schemeId, PlatformStaticModule module, PlatformMenu menu) {
        String menuId = menu.id().isBlank() ? moduleMenuId(module.alias()) : menu.id();
        Menu desired = new Menu();
        desired.setId(menuId);
        desired.setSchemeId(schemeId);
        desired.setMenuType(MenuType.MODULE);
        desired.setParentId(menu.parent());
        desired.setTitle(menu.title().isBlank() ? module.title() : menu.title().trim());
        desired.setModuleAlias(module.alias());
        desired.setPageMode(MenuPageMode.LIST);
        desired.setEnabled(menu.enabled());
        desired.setSortOrder(menu.order());

        context.apply(InitialDataRecord
                        .of(menuId, InitialDataPolicy.RECONCILE_MANAGED,
                                menuService.selectIgnoreSoftDelete(menuId), desired)
                        .identity(PlatformAdminMenuInitialDataContribution.menuIdField(),
                                PlatformAdminMenuInitialDataContribution.menuSchemeIdField())
                        .managed(PlatformAdminMenuInitialDataContribution.menuParentIdField(),
                                PlatformAdminMenuInitialDataContribution.menuTypeField(),
                                PlatformAdminMenuInitialDataContribution.menuModuleAliasField(),
                                PlatformAdminMenuInitialDataContribution.menuRouteField(),
                                PlatformAdminMenuInitialDataContribution.menuExternalUrlField(),
                                PlatformAdminMenuInitialDataContribution.menuPageModeField(),
                                PlatformAdminMenuInitialDataContribution.menuDefaultUiConfigIdField(),
                                PlatformAdminMenuInitialDataContribution.menuDefaultQueryTemplateIdField(),
                                PlatformAdminMenuInitialDataContribution.menuEntryParamsJsonField())
                        .operator(PlatformAdminMenuInitialDataContribution.menuTitleField(),
                                PlatformAdminMenuInitialDataContribution.menuEnabledField(),
                                PlatformAdminMenuInitialDataContribution.menuSortOrderField()),
                item -> menuService.insert(item),
                item -> menuService.update(item));
    }

    private String moduleMenuId(String moduleAlias) {
        return "platform.menu.module." + moduleAlias;
    }
}
