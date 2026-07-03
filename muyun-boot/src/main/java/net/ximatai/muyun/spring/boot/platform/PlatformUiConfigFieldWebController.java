package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigFieldService;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = PlatformUiConfigFieldService.MODULE_ALIAS,
        title = "平台 UI 字段配置")
@Path("/platform.ui-config/{uiConfigId}/fields")
public class PlatformUiConfigFieldWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiConfigField, PlatformUiConfigFieldService> {

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("uiConfigId", pathVariable(request, "uiConfigId"));
    }

    @Override
    protected void bindScope(PlatformUiConfigField record, WebRequestScope request) {
        record.setUiConfigId(pathVariable(request, "uiConfigId"));
    }

    @Override
    protected boolean inScope(PlatformUiConfigField record, WebRequestScope request) {
        return Objects.equals(record.getUiConfigId(), pathVariable(request, "uiConfigId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "UI config field does not belong to UI config: "
                + pathVariable(request, "uiConfigId") + "." + id;
    }
}
