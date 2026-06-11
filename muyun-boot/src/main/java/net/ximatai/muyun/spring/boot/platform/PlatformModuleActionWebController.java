package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.NestedEnabledSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = PlatformModuleActionService.MODULE_ALIAS, title = "平台模块动作")
@RequestMapping("/platform.module/{moduleAlias}/actions")
public class PlatformModuleActionWebController
        extends NestedEnabledSortableCrudWebSupport<PlatformModuleAction, PlatformModuleActionService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "moduleAlias", "actionCode", "entityAlias", "permissionActionCode",
            "title", "category", "actionLevel", "accessMode", "actionAuth", "dataAuth",
            "defaultGrantPolicy", "executorType", "executorKey", "sourceType", "sourceId",
            "bindingType", "bindingId", "bindingAlias", "systemManaged",
            "enabled", "sortOrder", "createdAt", "updatedAt");

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return PlatformConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(PlatformModuleAction record, HttpServletRequest request) {
        record.setModuleAlias(moduleAlias(request));
    }

    @Override
    protected boolean inScope(PlatformModuleAction record, HttpServletRequest request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "module action does not belong to module: " + moduleAlias(request) + "." + id;
    }

    private String moduleAlias(HttpServletRequest request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }
}
