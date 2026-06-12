package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.iam.position.Position;
import net.ximatai.muyun.spring.iam.position.PositionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.position", title = "岗位管理")
@RequestMapping("/iam.position")
public class PositionWebController extends WebSupport<PositionService> implements
        CrudWeb<Position, PositionService>,
        EnableWeb<Position, PositionService>,
        SortWeb<Position, PositionService> {
}
