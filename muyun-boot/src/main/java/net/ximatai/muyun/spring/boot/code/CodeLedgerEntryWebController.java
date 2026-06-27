package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = CodeLedgerEntryService.MODULE_ALIAS, title = "编码台账")
@RequestMapping({"/platform.code_ledger_entry", "/platform/code/ledger-entry"})
public class CodeLedgerEntryWebController extends WebSupport<CodeLedgerEntryService> implements
        ReadOnlyWeb<CodeLedgerEntry, CodeLedgerEntryService> {
}
