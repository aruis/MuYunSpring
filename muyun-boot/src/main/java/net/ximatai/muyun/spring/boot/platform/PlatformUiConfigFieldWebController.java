package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigField;
import net.ximatai.muyun.spring.platform.ui.PlatformUiConfigFieldService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformUiConfigFieldService.MODULE_ALIAS,
        title = "平台 UI 字段配置")
@RequestMapping("/platform.ui-config/{uiConfigId}/fields")
public class PlatformUiConfigFieldWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformUiConfigField, PlatformUiConfigFieldService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "title", "moduleMetadataFieldId", "fieldUiTypeAlias", "visible", "readOnly",
            "requiredOverride", "enabled");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, "platform UI config field query");
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("title"));
    }

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
