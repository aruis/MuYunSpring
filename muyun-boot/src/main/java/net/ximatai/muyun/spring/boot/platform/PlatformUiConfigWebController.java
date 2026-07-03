package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformUiConfigService.MODULE_ALIAS, title = "平台 UI 配置")
@Path("/platform.ui-set/{uiSetId}/configs")
public class PlatformUiConfigWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiConfig, PlatformUiConfigService> {

    @Override
    protected void appendScope(Criteria criteria, @Context HttpServletRequest request) {
        criteria.eq("uiSetId", pathVariable(request, "uiSetId"));
    }

    @Override
    protected void bindScope(PlatformUiConfig record, @Context HttpServletRequest request) {
        record.setUiSetId(pathVariable(request, "uiSetId"));
    }

    @Override
    protected boolean inScope(PlatformUiConfig record, @Context HttpServletRequest request) {
        return Objects.equals(record.getUiSetId(), pathVariable(request, "uiSetId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "UI config does not belong to UI set: " + pathVariable(request, "uiSetId") + "." + id;
    }
}
