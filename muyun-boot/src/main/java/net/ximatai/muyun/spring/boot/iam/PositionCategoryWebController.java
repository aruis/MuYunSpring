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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.position_category", title = "岗位分类")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 35)
@RequestMapping("/iam.position_category")
public class PositionCategoryWebController extends WebSupport<PositionCategoryService> implements
        CrudWeb<PositionCategory, PositionCategoryService>,
        EnableWeb<PositionCategory, PositionCategoryService>,
        TreeWeb<PositionCategory, PositionCategoryService> {
}
