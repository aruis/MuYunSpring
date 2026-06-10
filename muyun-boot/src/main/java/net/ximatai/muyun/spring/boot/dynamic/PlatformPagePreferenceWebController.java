package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreference;
import net.ximatai.muyun.spring.platform.ui.PlatformPagePreferenceService;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform.page-preference/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class PlatformPagePreferenceWebController {
    private final PlatformPagePreferenceService preferenceService;

    public PlatformPagePreferenceWebController(PlatformPagePreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public PlatformPagePreference preference(@PathVariable String moduleAlias,
                                             @RequestParam(defaultValue = "WEB") PlatformUiClientType clientType,
                                             @RequestParam(required = false) String pageKey) {
        return preferenceService.currentUserPreference(moduleAlias, clientType, pageKey);
    }

    @PostMapping
    public PlatformPagePreference savePreference(@PathVariable String moduleAlias,
                                                 @RequestBody(required = false) PlatformPagePreferenceRequest request) {
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
