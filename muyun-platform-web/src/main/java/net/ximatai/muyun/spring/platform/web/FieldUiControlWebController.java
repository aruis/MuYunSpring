package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;

import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.FieldUiControl;
import net.ximatai.muyun.spring.platform.metadata.FieldUiControlService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class, alias = FieldUiControlService.MODULE_ALIAS, title = "字段 UI 控件", route = "/config/field-ui-controls")
@PlatformMenu(parent = PlatformMenuGroups.MODELING, title = "字段 UI 控件", order = 50)
@StaticModuleOpenApi
@RequestMapping("/platform.field_ui_control")
public class FieldUiControlWebController extends WebSupport<FieldUiControlService> implements
        CrudWeb<FieldUiControl, FieldUiControlService>,
        SystemScope<FieldUiControlService> {
}
