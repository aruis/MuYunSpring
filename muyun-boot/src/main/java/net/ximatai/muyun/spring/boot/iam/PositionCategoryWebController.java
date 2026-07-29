package net.ximatai.muyun.spring.boot.iam;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeResolver;
import net.ximatai.muyun.spring.boot.web.TreeScope;
import net.ximatai.muyun.spring.boot.web.ScopedTreeWebProjectionPolicy;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.position.PositionCategory;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.position_category", title = "岗位管理",
        route = "/iam/positions")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "岗位管理", order = 40)
@RequestMapping("/iam.position_category")
public class PositionCategoryWebController extends WebSupport<PositionCategoryService> implements
        CrudWeb<PositionCategory, PositionCategoryService>,
        ScopedTreeWebProjectionPolicy<PositionCategory, PositionCategoryService>,
        MutationTenantScopeResolver<PositionCategory> {
    @Override
    public TreeScope treeScope(HttpServletRequest request) {
        String tenantId = request.getParameter("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            return TreeScope.none();
        }
        return TreeScope.tenant(Criteria.of().eq("tenantId", tenantId), tenantId);
    }

    @Override
    public Optional<String> tenantIdForCreate(PositionCategory record) {
        return Optional.ofNullable(record == null ? null : record.getTenantId()).filter(value -> !value.isBlank());
    }

    @Override
    public Optional<String> tenantIdForUpdate(String id, PositionCategory record) {
        PositionCategory existing = service().select(id);
        return Optional.ofNullable(existing == null ? null : existing.getTenantId()).filter(value -> !value.isBlank());
    }

    @Override
    public Optional<String> tenantIdForExistingRecord(String id) {
        PositionCategory existing = service().select(id);
        return Optional.ofNullable(existing == null ? null : existing.getTenantId()).filter(value -> !value.isBlank());
    }
}
