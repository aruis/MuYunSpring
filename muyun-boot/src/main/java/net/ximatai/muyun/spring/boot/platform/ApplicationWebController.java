package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = ApplicationService.MODULE_ALIAS, title = "平台应用")
@RequestMapping("/platform.application")
public class ApplicationWebController extends WebSupport<ApplicationService> implements
        CrudWeb<Application, ApplicationService>,
        EnableWeb<Application, ApplicationService>,
        SortWeb<Application, ApplicationService>,
        SystemScope<ApplicationService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }
}
