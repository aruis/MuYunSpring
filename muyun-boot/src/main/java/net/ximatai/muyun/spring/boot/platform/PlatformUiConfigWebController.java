package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformUiConfigService.MODULE_ALIAS, title = "平台 UI 配置")
@RequestMapping("/platform.ui-set/{uiSetId}/configs")
public class PlatformUiConfigWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiConfig, PlatformUiConfigService> {
    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("uiSetId", pathVariable(request, "uiSetId"));
    }

    @Override
    protected void bindScope(PlatformUiConfig record, HttpServletRequest request) {
        record.setUiSetId(pathVariable(request, "uiSetId"));
    }

    @Override
    protected boolean inScope(PlatformUiConfig record, HttpServletRequest request) {
        return Objects.equals(record.getUiSetId(), pathVariable(request, "uiSetId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "UI config does not belong to UI set: " + pathVariable(request, "uiSetId") + "." + id;
    }
}
