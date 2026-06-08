package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeLedgerEntryService.MODULE_ALIAS, title = "编码台账")
@RequestMapping({"/platform.code_ledger_entry", "/platform/code/ledger-entry"})
public class CodeLedgerEntryWebController extends WebSupport<CodeLedgerEntryService> implements
        ReadOnlyWeb<CodeLedgerEntry, CodeLedgerEntryService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "ruleId", "moduleAlias", "entityAlias", "fieldName", "codeValue",
            "basisKey", "periodKey", "sourceRecordId", "status", "lastAction");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.desc("createdAt"));
    }
}
