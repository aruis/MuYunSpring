package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = MetadataService.MODULE_ALIAS, title = "平台元数据")
@RequestMapping("/platform.metadata")
public class MetadataWebController extends WebSupport<MetadataService> implements
        CrudWeb<Metadata, MetadataService>,
        EnableWeb<Metadata, MetadataService>,
        SortWeb<Metadata, MetadataService>,
        SystemScope<MetadataService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "applicationAlias", "alias", "schemaName", "tableName",
            "dataScopeEnabled", "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }
}
