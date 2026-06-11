package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.code.CodeOpsActionService;
import net.ximatai.muyun.spring.platform.code.CodeSequenceBaselineResult;
import net.ximatai.muyun.spring.platform.code.CodeSequenceState;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = CodeSequenceStateService.MODULE_ALIAS, title = "编码序列状态")
@RequestMapping({"/platform.code_sequence_state", "/platform/code/sequence-state"})
public class CodeSequenceStateWebController extends WebSupport<CodeSequenceStateService> implements
        ReadOnlyWeb<CodeSequenceState, CodeSequenceStateService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "ruleId", "basisKey", "periodKey", "currentValue");

    private CodeOpsActionService opsActionService;

    public CodeSequenceStateWebController() {
    }

    @Autowired
    public CodeSequenceStateWebController(CodeOpsActionService opsActionService) {
        this.opsActionService = opsActionService;
    }

    @Override
    public Criteria queryCriteria(WebQueryRequest request) {
        return CodeWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    public Sort[] querySorts(WebQueryRequest request) {
        return CodeWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("ruleId"), Sort.asc("basisKey"), Sort.asc("periodKey"));
    }

    @PostMapping("/adjust/{id}")
    @CustomActionEndpoint(value = "adjustBaseline", title = "调整序列基线",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeSequenceBaselineResult adjustBaseline(@PathVariable String id,
                                                     @RequestBody AdjustBaselineRequest request) {
        return webScope(() -> requireOpsActionService().adjustSequenceState(
                id,
                request.currentValue(),
                request.reason()
        ));
    }

    private CodeOpsActionService requireOpsActionService() {
        if (opsActionService == null) {
            throw new IllegalStateException("Code ops action service is not configured");
        }
        return opsActionService;
    }

    public record AdjustBaselineRequest(Long currentValue, String reason) {
    }
}
