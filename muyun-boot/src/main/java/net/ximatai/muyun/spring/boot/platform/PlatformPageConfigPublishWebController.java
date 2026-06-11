package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigPublishService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "platform", alias = "platform.page_config_publish", title = "平台页面配置发布")
@RequestMapping("/platform.page_config_publish")
public class PlatformPageConfigPublishWebController extends WebSupport<PlatformPageConfigPublishService>
        implements SystemScope<PlatformPageConfigPublishService> {
    @PostMapping("/ui-configs/{id}/publish")
    @CustomActionEndpoint(value = "publishUiConfig", title = "发布 UI 配置", level = PlatformActionLevel.RECORD)
    public WebCountResponse publishUiConfig(@PathVariable String id) {
        return webScope(() -> {
            service().publishUiConfig(id);
            return new WebCountResponse(1);
        });
    }

    @PostMapping("/ui-configs/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishUiConfig", title = "取消发布 UI 配置", level = PlatformActionLevel.RECORD)
    public WebCountResponse unpublishUiConfig(@PathVariable String id) {
        return webScope(() -> {
            service().unpublishUiConfig(id);
            return new WebCountResponse(1);
        });
    }

    @PostMapping("/query-templates/{id}/publish")
    @CustomActionEndpoint(value = "publishQueryTemplate", title = "发布查询模板", level = PlatformActionLevel.RECORD)
    public WebCountResponse publishQueryTemplate(@PathVariable String id) {
        return webScope(() -> {
            service().publishQueryTemplate(id);
            return new WebCountResponse(1);
        });
    }

    @PostMapping("/query-templates/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishQueryTemplate", title = "取消发布查询模板",
            level = PlatformActionLevel.RECORD)
    public WebCountResponse unpublishQueryTemplate(@PathVariable String id) {
        return webScope(() -> {
            service().unpublishQueryTemplate(id);
            return new WebCountResponse(1);
        });
    }
}
