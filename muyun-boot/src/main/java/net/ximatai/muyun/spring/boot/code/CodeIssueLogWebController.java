package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeIssueLog;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeIssueLogService.MODULE_ALIAS, title = "编码日志")
@RequestMapping({"/platform.code_issue_log", "/platform/code/issue-log"})
public class CodeIssueLogWebController extends WebSupport<CodeIssueLogService> implements
        ReadOnlyWeb<CodeIssueLog, CodeIssueLogService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "ruleId", "moduleAlias", "entityAlias", "fieldName", "basisKey", "periodKey",
            "generatedValue", "status", "retryCount");

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.desc("createdAt"));
    }
}
