package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContribution;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import net.ximatai.muyun.spring.iam.position.PositionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticActionContribution(
        targetModule = PositionCategoryService.MODULE_ALIAS,
        resource = "position",
        resourceTitle = "岗位"
)
@RequestMapping("/iam.position")
public class PositionWebController extends WebSupport<PositionService> implements
        CrudWeb<Position, PositionService>,
        EnableWeb<Position, PositionService>,
        SortWeb<Position, PositionService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "categoryId", "code", "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return IamWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return IamWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"), Sort.asc("title"));
    }

    @Override
    public boolean supportsUnpagedQuery() {
        return true;
    }
}
