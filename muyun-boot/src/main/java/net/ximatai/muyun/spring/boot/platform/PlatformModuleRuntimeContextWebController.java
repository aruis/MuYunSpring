package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@ApplicationScoped
@Path("/platform.module/{moduleAlias}/context")
public class PlatformModuleRuntimeContextWebController {
    private final PlatformModuleRuntimeContextService contextService;

    public PlatformModuleRuntimeContextWebController(PlatformModuleRuntimeContextService contextService) {
        this.contextService = contextService;
    }

    @GET
    @ActionEndpoint(PlatformAction.MENU)
    public PlatformModuleRuntimeContext context(@PathParam("moduleAlias") String moduleAlias) {
        return contextService.context(moduleAlias);
    }
}
