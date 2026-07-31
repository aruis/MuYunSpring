package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigFieldService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.boot.platform.PlatformApplication.class, alias = PlatformUiConfigFieldService.MODULE_ALIAS,
        title = "平台 UI 字段配置", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping("/platform.ui-config/{uiConfigId}/fields")
public class PlatformUiConfigFieldWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiConfigField, PlatformUiConfigFieldService> {

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("uiConfigId", pathVariable(request, "uiConfigId"));
    }

    @Override
    protected void bindScope(PlatformUiConfigField record, HttpServletRequest request) {
        record.setUiConfigId(pathVariable(request, "uiConfigId"));
    }

    @Override
    protected boolean inScope(PlatformUiConfigField record, HttpServletRequest request) {
        return Objects.equals(record.getUiConfigId(), pathVariable(request, "uiConfigId"));
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "UI config field does not belong to UI config: "
                + pathVariable(request, "uiConfigId") + "." + id;
    }
}
