package net.ximatai.muyun.spring.boot.dynamic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreference;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreferenceService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;

@ApplicationScoped
@Path("/platform.page-preference/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class PlatformPagePreferenceWebController {
    private final PlatformPagePreferenceService preferenceService;

    public PlatformPagePreferenceWebController(PlatformPagePreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GET
    public PlatformPagePreference preference(@PathParam("moduleAlias") String moduleAlias,
                                             @DefaultValue("WEB") @QueryParam("clientType") PlatformUiClientType clientType,
                                             @QueryParam("pageKey") String pageKey) {
        return preferenceService.currentUserPreference(moduleAlias, clientType, pageKey);
    }

    @POST
    public PlatformPagePreference savePreference(@PathParam("moduleAlias") String moduleAlias,
                                                 PlatformPagePreferenceRequest request) {
        PlatformPagePreferenceRequest normalized = request == null
                ? PlatformPagePreferenceRequest.empty()
                : request;
        return preferenceService.saveCurrentUserPreference(
                moduleAlias,
                normalized.clientType(),
                normalized.pageKey(),
                requirePreferenceJson(normalized.preferenceJson())
        );
    }

    private String requirePreferenceJson(String preferenceJson) {
        if (preferenceJson == null || preferenceJson.isBlank()) {
            throw new PlatformException("page preference preferenceJson must not be blank");
        }
        return preferenceJson;
    }
}

record PlatformPagePreferenceRequest(PlatformUiClientType clientType,
                                     String pageKey,
                                     String preferenceJson) {
    static PlatformPagePreferenceRequest empty() {
        return new PlatformPagePreferenceRequest(null, null, null);
    }
}
