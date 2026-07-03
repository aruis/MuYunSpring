package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntryService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = CodeLedgerEntryService.MODULE_ALIAS, title = "编码台账")
@Path("/platform.code_ledger_entry")
public class CodeLedgerEntryWebController extends WebSupport<CodeLedgerEntryService> implements
        ReadOnlyWeb<CodeLedgerEntry, CodeLedgerEntryService> {
}
