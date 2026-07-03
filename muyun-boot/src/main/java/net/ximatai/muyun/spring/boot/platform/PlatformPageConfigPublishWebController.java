package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.ui.PlatformPageConfigPublishService;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = "platform.page_config_publish", title = "平台页面配置发布")
@Path("/platform.page_config_publish")
public class PlatformPageConfigPublishWebController extends WebSupport<PlatformPageConfigPublishService>
        implements SystemScope<PlatformPageConfigPublishService> {
    @POST
    @Path("/ui-configs/{id}/publish")
    @CustomActionEndpoint(value = "publishUiConfig", title = "发布 UI 配置", level = PlatformActionLevel.RECORD)
    public WebCountResponse publishUiConfig(@PathParam("id") String id) {
        return webScope(() -> {
            service().publishUiConfig(id);
            return new WebCountResponse(1);
        });
    }

    @POST
    @Path("/ui-configs/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishUiConfig", title = "取消发布 UI 配置", level = PlatformActionLevel.RECORD)
    public WebCountResponse unpublishUiConfig(@PathParam("id") String id) {
        return webScope(() -> {
            service().unpublishUiConfig(id);
            return new WebCountResponse(1);
        });
    }

    @POST
    @Path("/query-templates/{id}/publish")
    @CustomActionEndpoint(value = "publishQueryTemplate", title = "发布查询模板", level = PlatformActionLevel.RECORD)
    public WebCountResponse publishQueryTemplate(@PathParam("id") String id) {
        return webScope(() -> {
            service().publishQueryTemplate(id);
            return new WebCountResponse(1);
        });
    }

    @POST
    @Path("/query-templates/{id}/unpublish")
    @CustomActionEndpoint(value = "unpublishQueryTemplate", title = "取消发布查询模板",
            level = PlatformActionLevel.RECORD)
    public WebCountResponse unpublishQueryTemplate(@PathParam("id") String id) {
        return webScope(() -> {
            service().unpublishQueryTemplate(id);
            return new WebCountResponse(1);
        });
    }
}
