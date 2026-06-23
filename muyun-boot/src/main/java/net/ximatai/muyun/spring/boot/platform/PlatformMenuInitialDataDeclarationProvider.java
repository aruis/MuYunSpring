package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.menu.MenuType;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.List;

public class PlatformMenuInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private final MenuService menuService;
    private final ApplicationContext applicationContext;

    public PlatformMenuInitialDataDeclarationProvider(MenuService menuService,
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
    public List<InitialDataDeclaration<?>> declarations() {
        return contributedMenus(MenuSchemeService.ADMIN_SCHEME_ID);
    }

    private List<InitialDataDeclaration<?>> contributedMenus(String schemeId) {
        List<InitialDataDeclaration<?>> declarations = new ArrayList<>();
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
            declarations.add(moduleMenu(schemeId, module, menu));
        }
        return declarations;
    }

    private InitialDataDeclaration<Menu> moduleMenu(String schemeId, PlatformStaticModule module, PlatformMenu menu) {
        String menuId = menu.id().isBlank() ? moduleMenuId(module.alias()) : menu.id();
        Menu desired = new Menu();
        desired.setId(menuId);
        desired.setSchemeId(schemeId);
        desired.setMenuType(MenuType.MODULE);
        desired.setOpenMode(menu.openMode());
        desired.setParentId(menu.parent());
        desired.setTitle(menu.title().isBlank() ? module.title() : menu.title().trim());
        desired.setModuleAlias(module.alias());
        desired.setPageMode(MenuPageMode.LIST);
        desired.setEnabled(menu.enabled());
        desired.setSortOrder(menu.order());
        return InitialDataDeclaration.reconcileManaged(menuService, desired);
    }

    private String moduleMenuId(String moduleAlias) {
        return "platform.menu.module." + moduleAlias;
    }
}
