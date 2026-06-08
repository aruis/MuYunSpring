package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntryService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeRecycleEntryService.MODULE_ALIAS, title = "编码回收")
@RequestMapping({"/platform.code_recycle_entry", "/platform/code/recycle-entry"})
public class CodeRecycleEntryWebController extends WebSupport<CodeRecycleEntryService> implements
        ReadOnlyWeb<CodeRecycleEntry, CodeRecycleEntryService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "ruleId", "basisKey", "periodKey", "recycledValue", "sourceRecordId", "status");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.desc("createdAt"));
    }
}
