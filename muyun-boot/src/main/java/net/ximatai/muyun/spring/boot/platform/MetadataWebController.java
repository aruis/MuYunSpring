package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = MetadataService.MODULE_ALIAS, title = "平台元数据")
@PlatformMenu(parent = PlatformMenuGroups.CONFIG, title = "元数据管理", order = 30)
@RequestMapping("/platform.metadata")
public class MetadataWebController extends WebSupport<MetadataService> implements
        CrudWeb<Metadata, MetadataService>,
        EnableWeb<Metadata, MetadataService>,
        SortWeb<Metadata, MetadataService>,
        SystemScope<MetadataService> {
}
