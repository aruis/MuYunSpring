package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.BusinessMutation;
import net.ximatai.muyun.spring.boot.web.BusinessMutationResultSupport;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigPublishService;
import net.ximatai.muyun.spring.platform.ui.PlatformQueryTemplateService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigService;
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
    @BusinessMutation
    public int publishUiConfig(@PathVariable String id) {
        return webScope(() -> {
            service().publishUiConfig(id);
            BusinessMutationResultSupport.successUpdated("platform.ui-config.published",
                    "UI 配置已发布", PlatformUiConfigService.MODULE_ALIAS, id);
            return 1;
        });
    }

    @PostMapping("/ui-configs/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishUiConfig", title = "取消发布 UI 配置", level = PlatformActionLevel.RECORD)
    @BusinessMutation
    public int unpublishUiConfig(@PathVariable String id) {
        return webScope(() -> {
            service().unpublishUiConfig(id);
            BusinessMutationResultSupport.successUpdated("platform.ui-config.unpublished",
                    "UI 配置已取消发布", PlatformUiConfigService.MODULE_ALIAS, id);
            return 1;
        });
    }

    @PostMapping("/query-templates/{id}/publish")
    @CustomActionEndpoint(value = "publishQueryTemplate", title = "发布查询模板", level = PlatformActionLevel.RECORD)
    @BusinessMutation
    public int publishQueryTemplate(@PathVariable String id) {
        return webScope(() -> {
            service().publishQueryTemplate(id);
            BusinessMutationResultSupport.successUpdated("platform.query-template.published",
                    "查询模板已发布", PlatformQueryTemplateService.MODULE_ALIAS, id);
            return 1;
        });
    }

    @PostMapping("/query-templates/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishQueryTemplate", title = "取消发布查询模板",
            level = PlatformActionLevel.RECORD)
    @BusinessMutation
    public int unpublishQueryTemplate(@PathVariable String id) {
        return webScope(() -> {
            service().unpublishQueryTemplate(id);
            BusinessMutationResultSupport.successUpdated("platform.query-template.unpublished",
                    "查询模板已取消发布", PlatformQueryTemplateService.MODULE_ALIAS, id);
            return 1;
        });
    }
}
