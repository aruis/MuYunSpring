package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

public class PlatformMenuRegistrar implements ApplicationRunner, Ordered {
    public static final String ADMIN_SCHEME_ID = "platform.menu_scheme.admin";
    public static final String ADMIN_SCHEME_ALIAS = "platform_admin";

    private final MenuSchemeService schemeService;
    private final MenuService menuService;
    private final ApplicationContext applicationContext;

    public PlatformMenuRegistrar(MenuSchemeService schemeService,
                                 MenuService menuService,
                                 ApplicationContext applicationContext) {
        this.schemeService = schemeService;
        this.menuService = menuService;
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void run(ApplicationArguments args) {
        registerAll();
    }

    public void registerAll() {
        try (TenantContext.Scope ignored = TenantContext.system("register platform menus")) {
            MenuScheme scheme = ensureAdminScheme();
            ensureGroup(scheme.getId(), PlatformMenuGroups.CONFIG, "平台配置与低代码运维", 10);
            ensureGroup(scheme.getId(), PlatformMenuGroups.IDENTITY, "组织与权限", 20);
            ensureGroup(scheme.getId(), PlatformMenuGroups.OPS, "平台运行运维", 30);
            registerContributedMenus(scheme.getId());
        }
    }

    private MenuScheme ensureAdminScheme() {
        MenuScheme scheme = schemeService.select(ADMIN_SCHEME_ID);
        if (scheme == null) {
            scheme = new MenuScheme();
            scheme.setId(ADMIN_SCHEME_ID);
            scheme.setAlias(ADMIN_SCHEME_ALIAS);
            scheme.setScopeType(MenuScopeType.SYSTEM);
            scheme.setTitle("平台超管");
            scheme.setEnabled(Boolean.TRUE);
            scheme.setSortOrder(1);
            schemeService.insert(scheme);
            return scheme;
        }

        scheme.setTitle("平台超管");
        scheme.setEnabled(Boolean.TRUE);
        scheme.setSortOrder(1);
        schemeService.update(scheme);
        return scheme;
    }

    private void ensureGroup(String schemeId, String groupId, String title, int sortOrder) {
        Menu group = menuService.select(groupId);
        if (group == null) {
            group = new Menu();
            group.setId(groupId);
            group.setSchemeId(schemeId);
        } else {
            requireManagedScheme(group, schemeId);
        }
        group.setParentId(TreeAbility.ROOT_ID);
        group.setMenuType(MenuType.GROUP);
        clearTarget(group);
        group.setTitle(title);
        group.setEnabled(Boolean.TRUE);
        group.setSortOrder(sortOrder);
        upsert(group);
    }

    private void registerContributedMenus(String schemeId) {
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
            ensureModuleMenu(schemeId, module, menu);
        }
    }

    private void ensureModuleMenu(String schemeId, PlatformStaticModule module, PlatformMenu menu) {
        String menuId = menu.id().isBlank() ? moduleMenuId(module.alias()) : menu.id();
        Menu item = menuService.select(menuId);
        if (item == null) {
            item = new Menu();
            item.setId(menuId);
            item.setSchemeId(schemeId);
        } else {
            requireManagedScheme(item, schemeId);
        }
        item.setMenuType(MenuType.MODULE);
        item.setParentId(menu.parent());
        item.setTitle(menu.title().isBlank() ? module.title() : menu.title().trim());
        item.setModuleAlias(module.alias());
        item.setRoute(null);
        item.setExternalUrl(null);
        item.setPageMode(null);
        item.setDefaultUiConfigId(null);
        item.setDefaultQueryTemplateId(null);
        item.setEntryParamsJson(null);
        item.setEnabled(menu.enabled());
        item.setSortOrder(menu.order());
        upsert(item);
    }

    private void clearTarget(Menu menu) {
        menu.setModuleAlias(null);
        menu.setRoute(null);
        menu.setExternalUrl(null);
        menu.setPageMode(null);
        menu.setDefaultUiConfigId(null);
        menu.setDefaultQueryTemplateId(null);
        menu.setEntryParamsJson(null);
    }

    private void upsert(Menu menu) {
        if (menuService.select(menu.getId()) == null) {
            menuService.insert(menu);
        } else {
            menuService.update(menu);
        }
    }

    private void requireManagedScheme(Menu menu, String schemeId) {
        if (!schemeId.equals(menu.getSchemeId())) {
            throw new IllegalStateException("Managed platform menu belongs to another scheme: " + menu.getId());
        }
    }

    private String moduleMenuId(String moduleAlias) {
        return "platform.menu.module." + moduleAlias;
    }
}
