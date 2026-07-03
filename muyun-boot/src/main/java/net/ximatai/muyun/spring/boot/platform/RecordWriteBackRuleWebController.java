package net.ximatai.muyun.spring.boot.platform;

import jakarta.enterprise.context.ApplicationScoped;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRule;
import net.ximatai.muyun.spring.platform.writeback.RecordWriteBackRuleService;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = RecordWriteBackRuleService.MODULE_ALIAS,
        title = "平台回写规则")
@Path("/platform.module/{moduleAlias}/write-back-rules")
public class RecordWriteBackRuleWebController
        extends ModuleScopedRuleTreeWebSupport<RecordWriteBackRule, RecordWriteBackRuleService> {

    public RecordWriteBackRuleWebController() {
        super("triggerModuleAlias");
    }

    @GET
    @Path("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看回写规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public RecordWriteBackRule viewTree(@Context UriInfo uriInfo, @PathParam("id") String id) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> {
            requireScopedRecord(request, id);
            return service().viewRuleTree(id);
        });
    }

    @POST
    @Path("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存回写规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public RecordWriteBackRule saveTree(@Context UriInfo uriInfo, RecordWriteBackRule rule) {
        WebRequestScope request = requestScope(uriInfo);
        return webScope(() -> {
            if (rule == null) {
                throw new IllegalArgumentException("write-back rule tree must not be null");
            }
            requireExistingRuleInScope(request, rule);
            rule.setTriggerModuleAlias(moduleAlias(request));
            return service().saveRuleTree(rule);
        });
    }

    @Override
    protected String scopeValue(RecordWriteBackRule record) {
        return record.getTriggerModuleAlias();
    }
}
