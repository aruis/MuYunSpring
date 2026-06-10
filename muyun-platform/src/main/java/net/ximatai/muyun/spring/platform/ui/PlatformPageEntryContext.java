package net.ximatai.muyun.spring.platform.ui;

import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuPageMode;

public record PlatformPageEntryContext(
        String menuId,
        String moduleAlias,
        MenuPageMode pageMode,
        String defaultUiConfigId,
        String defaultQueryTemplateId,
        String entryParamsJson
) {
    public static PlatformPageEntryContext from(Menu menu,
                                                String defaultUiConfigId,
                                                String defaultQueryTemplateId) {
        return new PlatformPageEntryContext(
                menu == null ? null : menu.getId(),
                menu == null ? null : menu.getModuleAlias(),
                menu == null || menu.getPageMode() == null ? MenuPageMode.LIST : menu.getPageMode(),
                defaultUiConfigId,
                defaultQueryTemplateId,
                menu == null ? null : menu.getEntryParamsJson()
        );
    }

    public static PlatformPageEntryContext module(String moduleAlias,
                                                  String defaultUiConfigId,
                                                  String defaultQueryTemplateId) {
        return new PlatformPageEntryContext(null, moduleAlias, MenuPageMode.LIST, defaultUiConfigId,
                defaultQueryTemplateId, null);
    }
}
