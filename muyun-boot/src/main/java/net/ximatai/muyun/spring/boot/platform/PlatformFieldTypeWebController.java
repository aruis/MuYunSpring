package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformFieldTypeService.MODULE_ALIAS, title = "平台字段类型")
@RequestMapping("/platform.field_type")
public class PlatformFieldTypeWebController extends WebSupport<PlatformFieldTypeService> implements
        CrudWeb<PlatformFieldType, PlatformFieldTypeService>,
        EnableWeb<PlatformFieldType, PlatformFieldTypeService>,
        SortWeb<PlatformFieldType, PlatformFieldTypeService>,
        SystemScope<PlatformFieldTypeService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "alias", "title", "fieldType", "defaultLength", "defaultPrecision",
            "defaultScale", "defaultQueryOperator", "defaultUiTypeAlias",
            "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }
}
