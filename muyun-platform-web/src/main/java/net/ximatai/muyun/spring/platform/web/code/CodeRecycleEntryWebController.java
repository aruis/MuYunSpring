package net.ximatai.muyun.spring.platform.web.code;

import net.ximatai.muyun.spring.platform.web.PlatformStaticModule;
import net.ximatai.muyun.spring.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = CodeRecycleEntryService.MODULE_ALIAS, title = "编码回收", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping({"/platform.code_recycle_entry", "/platform/code/recycle-entry"})
public class CodeRecycleEntryWebController extends WebSupport<CodeRecycleEntryService> implements
        ReadOnlyWeb<CodeRecycleEntry, CodeRecycleEntryService> {
}
