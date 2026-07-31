package net.ximatai.muyun.spring.platform.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class PlatformModuleRuntimeActionWebController {
    private final PlatformRecordActionAvailabilityService recordActionAvailabilityService;

    public PlatformModuleRuntimeActionWebController(
            PlatformRecordActionAvailabilityService recordActionAvailabilityService) {
        this.recordActionAvailabilityService = recordActionAvailabilityService;
    }

    @GetMapping("/actions/{recordId}")
    public PlatformRecordActionAvailability recordActions(@PathVariable String moduleAlias,
                                                          @PathVariable String recordId) {
        return recordActionAvailabilityService.recordActions(moduleAlias, recordId);
    }
}
