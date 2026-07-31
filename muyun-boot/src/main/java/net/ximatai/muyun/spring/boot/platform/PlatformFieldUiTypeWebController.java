package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = PlatformFieldUiTypeService.MODULE_ALIAS, title = "平台字段 UI 类型")
@RequestMapping("/platform.field_ui_type")
public class PlatformFieldUiTypeWebController extends WebSupport<PlatformFieldUiTypeService> implements
        CrudWeb<PlatformFieldUiType, PlatformFieldUiTypeService>,
        SystemScope<PlatformFieldUiTypeService> {
}
