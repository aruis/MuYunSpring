package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.web.CrudWeb;
import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = PlatformFieldTypeService.MODULE_ALIAS, title = "平台字段类型")
@RequestMapping("/platform.field_type")
public class PlatformFieldTypeWebController extends WebSupport<PlatformFieldTypeService> implements
        CrudWeb<PlatformFieldType, PlatformFieldTypeService>,
        SystemScope<PlatformFieldTypeService> {
}
