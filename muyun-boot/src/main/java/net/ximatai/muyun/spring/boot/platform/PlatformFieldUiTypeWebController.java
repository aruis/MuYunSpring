package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformFieldUiTypeService.MODULE_ALIAS, title = "平台字段 UI 类型")
@Path("/platform.field_ui_type")
public class PlatformFieldUiTypeWebController extends WebSupport<PlatformFieldUiTypeService> implements
        CrudWeb<PlatformFieldUiType, PlatformFieldUiTypeService>,
        EnableWeb<PlatformFieldUiType, PlatformFieldUiTypeService>,
        SortWeb<PlatformFieldUiType, PlatformFieldUiTypeService>,
        SystemScope<PlatformFieldUiTypeService> {
}
