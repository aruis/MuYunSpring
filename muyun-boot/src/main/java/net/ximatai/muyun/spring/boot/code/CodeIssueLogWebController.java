package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.platform.code.CodeIssueLog;
import net.ximatai.muyun.spring.platform.code.CodeIssueLogService;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = CodeIssueLogService.MODULE_ALIAS, title = "编码日志")
@Path("/platform.code_issue_log")
public class CodeIssueLogWebController extends WebSupport<CodeIssueLogService> implements
        ReadOnlyWeb<CodeIssueLog, CodeIssueLogService> {
}
