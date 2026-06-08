package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeSequenceState;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeSequenceStateService.MODULE_ALIAS, title = "编码序列状态")
@RequestMapping({"/platform.code_sequence_state", "/platform/code/sequence-state"})
public class CodeSequenceStateWebController extends WebSupport<CodeSequenceStateService> implements
        ReadOnlyWeb<CodeSequenceState, CodeSequenceStateService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "ruleId", "basisKey", "periodKey", "currentValue");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("ruleId"), Sort.asc("basisKey"), Sort.asc("periodKey"));
    }
}
