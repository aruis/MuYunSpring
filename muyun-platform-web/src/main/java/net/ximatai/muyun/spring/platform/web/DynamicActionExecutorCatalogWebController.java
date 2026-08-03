package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformStaticModule;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exposes only explicitly bindable, deployed custom action executors to module configuration. */
@RestController
@PlatformStaticWebScope(PlatformStaticWebScope.Scope.CUSTOM)
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.application.PlatformApplication.class,
        alias = PlatformModuleActionService.MODULE_ALIAS, title = "平台模块动作")
@RequestMapping("/platform.module/action-executors")
public class DynamicActionExecutorCatalogWebController {
    private final DynamicActionExecutorRegistry executorRegistry;

    public DynamicActionExecutorCatalogWebController(DynamicActionExecutorRegistry executorRegistry) {
        this.executorRegistry = executorRegistry;
    }

    @GetMapping
    @ActionEndpoint(PlatformAction.QUERY)
    public List<DynamicActionExecutorDefinition> list() {
        return executorRegistry.definitions().stream().filter(DynamicActionExecutorDefinition::bindable).toList();
    }
}
