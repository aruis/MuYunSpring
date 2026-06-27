package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = CodeRecycleEntryService.MODULE_ALIAS, title = "编码回收")
@RequestMapping({"/platform.code_recycle_entry", "/platform/code/recycle-entry"})
public class CodeRecycleEntryWebController extends WebSupport<CodeRecycleEntryService> implements
        ReadOnlyWeb<CodeRecycleEntry, CodeRecycleEntryService> {
}
