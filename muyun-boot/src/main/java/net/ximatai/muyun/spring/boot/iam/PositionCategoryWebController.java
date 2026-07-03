package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.TreeWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.position.PositionCategory;
import net.ximatai.muyun.spring.iam.position.PositionCategoryService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@PlatformStaticModule(application = "iam", alias = "iam.position_category", title = "岗位管理",
        route = "/iam/positions")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "岗位管理", order = 40)
@Path("/iam.position_category")
public class PositionCategoryWebController extends WebSupport<PositionCategoryService> implements
        CrudWeb<PositionCategory, PositionCategoryService>,
        EnableWeb<PositionCategory, PositionCategoryService>,
        TreeWeb<PositionCategory, PositionCategoryService> {
}
