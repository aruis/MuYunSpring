package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformFieldTypeService.MODULE_ALIAS, title = "平台字段类型")
@Path("/platform.field_type")
public class PlatformFieldTypeWebController extends WebSupport<PlatformFieldTypeService> implements
        CrudWeb<PlatformFieldType, PlatformFieldTypeService>,
        EnableWeb<PlatformFieldType, PlatformFieldTypeService>,
        SortWeb<PlatformFieldType, PlatformFieldTypeService>,
        SystemScope<PlatformFieldTypeService> {
}
