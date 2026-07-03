package net.ximatai.muyun.spring.boot.platform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRule;
import net.ximatai.muyun.spring.platform.generation.RecordGenerationRuleService;


@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = RecordGenerationRuleService.MODULE_ALIAS,
        title = "平台生单规则")
@Path("/platform.module/{moduleAlias}/generation-rules")
public class RecordGenerationRuleWebController
        extends ModuleScopedRuleTreeWebSupport<RecordGenerationRule, RecordGenerationRuleService> {

    public RecordGenerationRuleWebController() {
        super("sourceModuleAlias");
    }

    @GET
    @Path("/viewTree/{id}")
    @CustomActionEndpoint(value = "viewTree", title = "查看生单规则树",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "id")
    public RecordGenerationRule viewTree(@Context HttpServletRequest request, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(request, id);
            return service().viewRuleTree(id);
        });
    }

    @POST
    @Path("/saveTree")
    @CustomActionEndpoint(value = "saveTree", title = "保存生单规则树",
            level = PlatformActionLevel.ANY, dataAuth = false)
    public RecordGenerationRule saveTree(@Context HttpServletRequest request, RecordGenerationRule rule) {
        return webScope(() -> {
            if (rule == null) {
                throw new IllegalArgumentException("generation rule tree must not be null");
            }
            requireExistingRuleInScope(request, rule);
            rule.setSourceModuleAlias(moduleAlias(request));
            return service().saveRuleTree(rule);
        });
    }

    @Override
    protected String scopeValue(RecordGenerationRule record) {
        return record.getSourceModuleAlias();
    }
}
