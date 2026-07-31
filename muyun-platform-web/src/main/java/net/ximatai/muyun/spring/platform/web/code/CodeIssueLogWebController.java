package net.ximatai.muyun.spring.platform.web.code;

import net.ximatai.muyun.spring.platform.web.PlatformStaticModule;
import net.ximatai.muyun.spring.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeIssueLog;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = net.ximatai.muyun.spring.platform.web.PlatformApplication.class, alias = CodeIssueLogService.MODULE_ALIAS, title = "编码日志", webScope = PlatformStaticModule.WebScope.CUSTOM)
@RequestMapping({"/platform.code_issue_log", "/platform/code/issue-log"})
public class CodeIssueLogWebController extends WebSupport<CodeIssueLogService> implements
        ReadOnlyWeb<CodeIssueLog, CodeIssueLogService> {
}
