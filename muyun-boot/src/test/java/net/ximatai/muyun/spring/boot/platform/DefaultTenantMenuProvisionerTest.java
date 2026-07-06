package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuDao;
import net.ximatai.muyun.spring.platform.menu.MenuOpenMode;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeDao;
import net.ximatai.muyun.spring.platform.menu.MenuSchemeService;
import net.ximatai.muyun.spring.platform.menu.MenuScopeType;
import net.ximatai.muyun.spring.platform.menu.MenuService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTenantMenuProvisionerTest {
    private static final int STANDARD_ID_MAX_LENGTH = 32;

    private final MenuSchemeMemoryDao schemeDao = new MenuSchemeMemoryDao();
    private final MenuMemoryDao menuDao = new MenuMemoryDao();
    private final PlatformModuleService moduleService = mock(PlatformModuleService.class);
    private final MenuSchemeService schemeService = new MenuSchemeService(schemeDao);
    private final MenuService menuService = new MenuService(menuDao, schemeService, moduleService, Optional.empty());
    private final DefaultTenantMenuProvisioner provisioner = new DefaultTenantMenuProvisioner(schemeService, menuService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateTenantAdminSchemeAndCopySystemAdminMenus() {
        when(moduleService.resolveVisibleModule("iam.user")).thenReturn(module("iam.user"));
        createSystemAdminMenuTree();

        provisioner.afterTenantCreated("demo");
        provisioner.afterTenantCreated("demo");

        String schemeId = DefaultTenantMenuProvisioner.tenantAdminSchemeId("demo");
        try (TenantContext.Scope ignored = TenantContext.use("demo")) {
            MenuScheme scheme = schemeService.select(schemeId);
            assertThat(scheme).isNotNull();
            assertThat(scheme.getAlias()).isEqualTo(DefaultTenantMenuProvisioner.TENANT_ADMIN_SCHEME_ALIAS);
            assertThat(scheme.getScopeType()).isEqualTo(MenuScopeType.TENANT);
            assertThat(scheme.getScopeId()).isEqualTo("demo");
            assertThat(scheme.getTenantId()).isEqualTo("demo");

            assertThat(menuService.rootMenus(schemeId))
                    .singleElement()
                    .satisfies(menu -> assertThat(menu.getTitle()).isEqualTo("组织与权限"));
            assertThat(menuDao.list(Criteria.of().eq("schemeId", schemeId)))
                    .hasSize(2)
                    .allSatisfy(menu -> assertThat(menu.getId()).hasSizeLessThanOrEqualTo(STANDARD_ID_MAX_LENGTH));
        }
    }

    @Test
    void shouldGenerateTenantAdminSchemeIdWithinStandardIdLength() {
        String schemeId = DefaultTenantMenuProvisioner.tenantAdminSchemeId("demo");

        assertThat(schemeId).startsWith("tenant_menu_");
        assertThat(schemeId).hasSizeLessThanOrEqualTo(STANDARD_ID_MAX_LENGTH);
    }

    private void createSystemAdminMenuTree() {
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            MenuScheme scheme = new MenuScheme();
            scheme.setId(MenuSchemeService.ADMIN_SCHEME_ID);
            scheme.setAlias(MenuSchemeService.ADMIN_SCHEME_ALIAS);
            scheme.setScopeType(MenuScopeType.SYSTEM);
            scheme.setScopeId(MenuSchemeService.SYSTEM_SCOPE_ID);
            scheme.setTitle("平台超管");
            scheme.setEnabled(Boolean.TRUE);
            schemeService.insert(scheme);

            Menu group = new Menu();
            group.setId("platform.menu.group.identity");
            group.setSchemeId(MenuSchemeService.ADMIN_SCHEME_ID);
            group.setParentId(TreeAbility.ROOT_ID);
            group.setTitle("组织与权限");
            group.setEnabled(Boolean.TRUE);
            group.setSortOrder(1);
            menuService.insert(group);

            Menu user = new Menu();
            user.setId("platform.menu.module.iam.user");
            user.setSchemeId(MenuSchemeService.ADMIN_SCHEME_ID);
            user.setParentId(group.getId());
            user.setTitle("用户");
            user.setOpenMode(MenuOpenMode.TAB);
            user.setModuleAlias("iam.user");
            user.setEnabled(Boolean.TRUE);
            user.setSortOrder(1);
            menuService.insert(user);
        }
    }

    private PlatformModule module(String moduleAlias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(moduleAlias);
        module.setApplicationAlias(moduleAlias.substring(0, moduleAlias.indexOf('.')));
        module.setModuleKind(ModuleKind.STATIC);
        module.setEnabled(Boolean.TRUE);
        return module;
    }

    private static class MenuSchemeMemoryDao extends TestMemoryDao<MenuScheme> implements MenuSchemeDao {
    }

    private static class MenuMemoryDao extends TestMemoryDao<Menu> implements MenuDao {
    }
}
