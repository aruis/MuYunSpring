package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeIssueLog;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@PlatformStaticModule(application = "platform", alias = CodeIssueLogService.MODULE_ALIAS, title = "编码日志")
@RequestMapping({"/platform.code_issue_log", "/platform/code/issue-log"})
public class CodeIssueLogWebController extends WebSupport<CodeIssueLogService> implements
        ReadOnlyWeb<CodeIssueLog, CodeIssueLogService> {
}
