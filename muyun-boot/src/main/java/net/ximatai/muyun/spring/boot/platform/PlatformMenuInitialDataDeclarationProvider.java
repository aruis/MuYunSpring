package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclaration;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataDeclarationProvider;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlatformMenuInitialDataDeclarationProvider implements InitialDataDeclarationProvider {
    private final MenuService menuService;
    private final BeanManager beanManager;
    private final List<Class<?>> beanClasses;

    public PlatformMenuInitialDataDeclarationProvider(MenuService menuService,
                                                      BeanManager beanManager) {
        this.menuService = menuService;
        this.beanManager = beanManager;
        this.beanClasses = List.of();
    }

    public PlatformMenuInitialDataDeclarationProvider(MenuService menuService,
                                                      List<?> beanInstances) {
        this.menuService = menuService;
        this.beanManager = null;
        this.beanClasses = beanInstances == null
                ? List.of()
                : beanInstances.stream()
                .filter(Objects::nonNull)
                .map(Object::getClass)
                .toList();
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
        for (Class<?> beanClass : beanClassesWithAnnotation(PlatformMenu.class)) {
            PlatformMenu menu = findAnnotation(beanClass, PlatformMenu.class);
            if (menu == null) {
                continue;
            }
            PlatformStaticModule module = findAnnotation(beanClass, PlatformStaticModule.class);
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
        String route = module.route().trim();
        String externalUrl = module.externalUrl().trim();
        validateModuleEntry(module, route, externalUrl);
        desired.setOpenMode(menu.openMode());
        desired.setParentId(menu.parent());
        desired.setTitle(menu.title().isBlank() ? module.title() : menu.title().trim());
        desired.setModuleAlias(module.alias());
        desired.setRoute(route.isBlank() ? null : route);
        desired.setExternalUrl(externalUrl.isBlank() ? null : externalUrl);
        desired.setPageMode(route.isBlank() && externalUrl.isBlank() ? MenuPageMode.LIST : null);
        desired.setEnabled(menu.enabled());
        desired.setSortOrder(menu.order());
        return InitialDataDeclaration.reconcileManaged(menuService, desired);
    }

    private void validateModuleEntry(PlatformStaticModule module, String route, String externalUrl) {
        if (!route.isBlank() && !externalUrl.isBlank()) {
            throw new IllegalStateException("@PlatformStaticModule cannot declare both route and externalUrl: "
                    + module.alias());
        }
    }

    private String moduleMenuId(String moduleAlias) {
        return "platform.menu.module." + moduleAlias;
    }

    private List<Bean<?>> beansWithAnnotation(Class<? extends Annotation> annotationType) {
        return beanManager.getBeans(Object.class, Any.Literal.INSTANCE).stream()
                .filter(bean -> findAnnotation(bean.getBeanClass(), annotationType) != null)
                .toList();
    }

    private List<Class<?>> beanClassesWithAnnotation(Class<? extends Annotation> annotationType) {
        if (beanManager == null) {
            return beanClasses.stream()
                    .filter(beanClass -> findAnnotation(beanClass, annotationType) != null)
                    .toList();
        }
        return beansWithAnnotation(annotationType).stream()
                .map(Bean::getBeanClass)
                .toList();
    }

    private static <A extends Annotation> A findAnnotation(Class<?> type, Class<A> annotationType) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            A annotation = current.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
