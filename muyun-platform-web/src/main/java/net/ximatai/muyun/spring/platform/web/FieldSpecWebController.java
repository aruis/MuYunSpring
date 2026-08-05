package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.FieldSpec;
import net.ximatai.muyun.spring.platform.metadata.FieldSpecService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = FieldSpecService.MODULE_ALIAS, title = "字段规格", route = "/config/field-specs")
@PlatformMenu(parent = PlatformMenuGroups.MODELING, title = "字段规格", order = 40)
@StaticModuleOpenApi
@RequestMapping("/platform.field_spec")
public class FieldSpecWebController extends WebSupport<FieldSpecService> implements
        CrudWeb<FieldSpec, FieldSpecService>,
        SystemScope<FieldSpecService> {
}
