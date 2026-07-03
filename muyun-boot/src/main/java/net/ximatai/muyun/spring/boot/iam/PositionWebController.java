package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.ModuleUiViewCodes;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticActionContribution;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import net.ximatai.muyun.spring.iam.position.PositionService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticActionContribution(
        targetModule = PositionCategoryService.MODULE_ALIAS,
        resource = "position",
        resourceTitle = "岗位"
)
@Path("/iam.position")
public class PositionWebController extends WebSupport<PositionService> implements
        CrudWeb<Position, PositionService>,
        EnableWeb<Position, PositionService>,
        SortWeb<Position, PositionService>,
        StaticModuleUiContributor {

    private static final String RESOURCE = "position";

    @Override
    public boolean supportsUnpagedQuery() {
        return true;
    }

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(PositionCategoryService.MODULE_ALIAS)
                .formView(ModuleUiViewCodes.childResourceDefaultForm(RESOURCE), form -> form
                        .title("岗位")
                        .field(RESOURCE, "categoryId", field -> field.label("所属分类").required())
                        .field(RESOURCE, "code", field -> field.label("岗位编码").required())
                        .field(RESOURCE, "title", field -> field.label("岗位名称").required())
                        .field(RESOURCE, "description", field -> field.label("说明"))
                        .field(RESOURCE, "enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }
}
