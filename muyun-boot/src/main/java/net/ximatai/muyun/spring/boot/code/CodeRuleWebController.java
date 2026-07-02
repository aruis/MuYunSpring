package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.ReadOnlyWeb;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.code.CodePreviewResult;
import net.ximatai.muyun.spring.platform.code.CodePreviewService;
import net.ximatai.muyun.spring.platform.code.CodeRecycleEntry;
import net.ximatai.muyun.spring.platform.code.CodeRecycleStatus;
import net.ximatai.muyun.spring.platform.code.CodeLedgerEntry;
import net.ximatai.muyun.spring.platform.code.CodeLedgerInspection;
import net.ximatai.muyun.spring.platform.code.CodeOpsActionService;
import net.ximatai.muyun.spring.platform.code.CodeOpsQueryService;
import net.ximatai.muyun.spring.platform.code.CodeRule;
import net.ximatai.muyun.spring.platform.code.CodeRuleOpsSnapshot;
import net.ximatai.muyun.spring.platform.code.CodeRuleService;
import net.ximatai.muyun.spring.platform.code.CodeSequenceBaselineResult;
import net.ximatai.muyun.spring.platform.code.CodeSequenceStateLocation;
import net.ximatai.muyun.spring.platform.code.PreviewCodeRuleCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = CodeRuleService.MODULE_ALIAS, title = "编码规则")
@PlatformMenu(parent = PlatformMenuGroups.OPS, order = 10)
@Path("/platform.code_rule")
public class CodeRuleWebController extends WebSupport<CodeRuleService> implements
        ReadOnlyWeb<CodeRule, CodeRuleService>,
        EnableWeb<CodeRule, CodeRuleService>,
        SortWeb<CodeRule, CodeRuleService> {

    private final CodePreviewService previewService;
    private final CodeOpsQueryService opsQueryService;
    private final CodeOpsActionService opsActionService;

    public CodeRuleWebController(CodePreviewService previewService) {
        this(previewService, null, null);
    }

    @Inject
    public CodeRuleWebController(CodePreviewService previewService,
                                 CodeOpsQueryService opsQueryService,
                                 CodeOpsActionService opsActionService) {
        this.previewService = previewService;
        this.opsQueryService = opsQueryService;
        this.opsActionService = opsActionService;
    }

    @GET
    @Path("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeRule viewTree(@PathParam("id") String id) {
        return webScope(() -> service().viewRuleTree(id));
    }

    @POST
    @Path("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public CodeRule saveTree(CodeRule rule) {
        return webScope(() -> service().saveRuleTree(rule));
    }

    @POST
    @Path("/preview")
    @CustomActionEndpoint(value = "preview", title = "预览编码",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public CodePreviewResult preview(PreviewRequest request) {
        return webScope(() -> {
            PreviewRequest normalized = request == null ? PreviewRequest.empty() : request;
            CodeRule rule = normalized.rule();
            if (normalized.ruleId() != null && !normalized.ruleId().isBlank()) {
                rule = service().viewRuleTree(normalized.ruleId());
            }
            return previewService.previewDraft(new PreviewCodeRuleCommand(
                    rule,
                    normalized.context(),
                    normalized.organizationId(),
                    normalized.at(),
                    normalized.sequenceValue()
            ));
        });
    }

    @POST
    @Path("/ops/view/{id}")
    @CustomActionEndpoint(value = "opsQuery", title = "编码运维查询",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeRuleOpsSnapshot viewOpsSnapshot(@PathParam("id") String id,
                                               OpsSnapshotRequest request) {
        return webScope(() -> requireOpsQueryService().viewRuleSnapshot(
                id,
                request == null ? null : request.limitPerCategory()
        ));
    }

    @POST
    @Path("/ops/queryByBizObject")
    @CustomActionEndpoint(value = "opsQuery", title = "编码运维查询",
            level = PlatformActionLevel.LIST, dataAuth = false)
    public List<CodeRuleOpsSnapshot> queryOpsSnapshots(OpsSnapshotRequest request) {
        return webScope(() -> {
            OpsSnapshotRequest normalized = request == null ? OpsSnapshotRequest.empty() : request;
            return requireOpsQueryService().queryBusinessObjectSnapshots(
                    normalized.moduleAlias(),
                    normalized.entityAlias(),
                    normalized.limitPerCategory()
            );
        });
    }

    @POST
    @Path("/ops/sequenceState/locate")
    @CustomActionEndpoint(value = "opsQuery", title = "编码运维查询",
            level = PlatformActionLevel.LIST, dataAuth = false)
    public CodeSequenceStateLocation locateSequenceState(SequenceBucketRequest request) {
        return webScope(() -> requireOpsQueryService().locateSequenceState(
                request.ruleId(),
                request.basisKey(),
                request.periodKey()
        ));
    }

    @POST
    @Path("/ops/sequenceState/baseline")
    @CustomActionEndpoint(value = "opsManage", title = "编码运维管理",
            level = PlatformActionLevel.LIST, dataAuth = false)
    public CodeSequenceBaselineResult setSequenceBaseline(SequenceBaselineRequest request) {
        return webScope(() -> requireOpsActionService().setSequenceBaseline(
                request.ruleId(),
                request.basisKey(),
                request.periodKey(),
                request.currentValue(),
                request.reason()
        ));
    }

    @POST
    @Path("/ops/recycleEntry/{id}/adjust")
    @CustomActionEndpoint(value = "opsManage", title = "编码运维管理",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeRecycleEntry adjustRecycleEntry(@PathParam("id") String id,
                                               RecycleAdjustRequest request) {
        return webScope(() -> requireOpsActionService().adjustRecycleEntry(
                id,
                request.status(),
                request.reason()
        ));
    }

    @POST
    @Path("/ops/ledgerEntry/{id}/inspect")
    @CustomActionEndpoint(value = "opsQuery", title = "编码运维查询",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeLedgerInspection inspectLedgerEntry(@PathParam("id") String id) {
        return webScope(() -> requireOpsActionService().inspectLedgerEntry(id));
    }

    @POST
    @Path("/ops/ledgerEntry/{id}/release")
    @CustomActionEndpoint(value = "opsManage", title = "编码运维管理",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public CodeLedgerEntry releaseLedgerEntry(@PathParam("id") String id,
                                              ReleaseLedgerRequest request) {
        return webScope(() -> requireOpsActionService().releaseStaleLedgerEntry(
                id,
                request == null ? null : request.reason()
        ));
    }

    private CodeOpsQueryService requireOpsQueryService() {
        if (opsQueryService == null) {
            throw new IllegalStateException("Code ops query service is not configured");
        }
        return opsQueryService;
    }

    private CodeOpsActionService requireOpsActionService() {
        if (opsActionService == null) {
            throw new IllegalStateException("Code ops action service is not configured");
        }
        return opsActionService;
    }

    public record PreviewRequest(
            String ruleId,
            CodeRule rule,
            Map<String, Object> context,
            String organizationId,
            LocalDateTime at,
            Long sequenceValue
    ) {
        static PreviewRequest empty() {
            return new PreviewRequest(null, null, Map.of(), null, null, null);
        }
    }

    public record OpsSnapshotRequest(String moduleAlias, String entityAlias, Integer limitPerCategory) {
        static OpsSnapshotRequest empty() {
            return new OpsSnapshotRequest(null, null, null);
        }
    }

    public record SequenceBucketRequest(String ruleId, String basisKey, String periodKey) {
    }

    public record SequenceBaselineRequest(String ruleId,
                                          String basisKey,
                                          String periodKey,
                                          Long currentValue,
                                          String reason) {
    }

    public record RecycleAdjustRequest(CodeRecycleStatus status, String reason) {
    }

    public record ReleaseLedgerRequest(String reason) {
    }
}
