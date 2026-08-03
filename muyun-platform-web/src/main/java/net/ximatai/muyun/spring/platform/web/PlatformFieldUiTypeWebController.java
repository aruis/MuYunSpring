package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldUiTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = PlatformFieldUiTypeService.MODULE_ALIAS, title = "平台字段 UI 类型")
@StaticModuleOpenApi
@RequestMapping("/platform.field_ui_type")
public class PlatformFieldUiTypeWebController extends WebSupport<PlatformFieldUiTypeService> implements
        CrudWeb<PlatformFieldUiType, PlatformFieldUiTypeService>,
        SystemScope<PlatformFieldUiTypeService> {
}
