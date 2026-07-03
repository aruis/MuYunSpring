package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = CodeRecycleEntryService.MODULE_ALIAS, title = "编码回收")
@Path("/platform.code_recycle_entry")
public class CodeRecycleEntryWebController extends WebSupport<CodeRecycleEntryService> implements
        ReadOnlyWeb<CodeRecycleEntry, CodeRecycleEntryService> {
}
