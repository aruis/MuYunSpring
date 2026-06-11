package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicActionDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.ui.PlatformActionBlock;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrap;
import net.ximatai.muyun.spring.platform.ui.PlatformPageBootstrapService;
import net.ximatai.muyun.spring.platform.ui.PlatformResolvedPageConfig;
import net.ximatai.muyun.spring.platform.ui.PlatformUiClientType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/platform.menu")
public class DynamicPageBootstrapWebController {
    private final PlatformPageBootstrapService bootstrapService;
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;

    public DynamicPageBootstrapWebController(PlatformPageBootstrapService bootstrapService,
                                             DynamicRecordService recordService,
                                             ActiveTenantVerifier activeTenantVerifier) {
        this.bootstrapService = bootstrapService;
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
    }

    @GetMapping("/{menuId}/entry")
    public DynamicPageBootstrapResponse entry(@PathVariable String menuId,
                                              @RequestParam(defaultValue = "WEB") PlatformUiClientType clientType) {
        requireTenantContext();
        PlatformPageBootstrap bootstrap = bootstrapService.bootstrapByMenu(menuId, clientType);
        return response(bootstrap);
    }

    private DynamicPageBootstrapResponse response(PlatformPageBootstrap bootstrap) {
        String moduleAlias = bootstrap.entry().moduleAlias();
        DynamicModuleDescriptor descriptor = permissionScopedDescriptor(moduleAlias);
        PlatformResolvedPageConfig resolvedConfig = permissionScopedResolvedConfig(bootstrap.resolvedConfig(), descriptor);
        return new DynamicPageBootstrapResponse(
                bootstrap.entry(),
                bootstrap.clientType(),
                descriptor,
                descriptor.mainEntityAlias(),
                resolvedConfig,
                "/" + moduleAlias + "/openapi"
        );
    }

    private PlatformResolvedPageConfig permissionScopedResolvedConfig(PlatformResolvedPageConfig config,
                                                                     DynamicModuleDescriptor descriptor) {
        Set<String> visibleActionCodes = descriptor.entities().stream()
                .flatMap(entity -> entity.actions().stream())
                .map(DynamicActionDescriptor::code)
                .collect(java.util.stream.Collectors.toSet());
        visibleActionCodes.addAll(descriptor.actions().stream()
                .map(DynamicActionDescriptor::code)
                .collect(java.util.stream.Collectors.toSet()));
        List<PlatformActionBlock> actionBlocks = config.actionBlocks().stream()
                .filter(block -> visibleActionCodes.contains(block.actionCode()))
                .toList();
        return new PlatformResolvedPageConfig(
                config.uiFields(),
                config.queryItems(),
                config.fieldUiTypes(),
                config.associationBlocks(),
                actionBlocks
        );
    }

    private DynamicModuleDescriptor permissionScopedDescriptor(String moduleAlias) {
        DynamicModuleDescriptor descriptor = recordService.describe(moduleAlias);
        return new DynamicModuleDescriptor(
                descriptor.moduleAlias(),
                descriptor.title(),
                descriptor.mainEntityAlias(),
                visibleModuleActions(moduleAlias, descriptor.actions()),
                descriptor.entities().stream()
                        .map(entity -> new DynamicEntityDescriptor(
                                entity.entityAlias(),
                                entity.title(),
                                entity.capabilities(),
                                entity.fields(),
                                entity.formulaRules(),
                                visibleEntityActions(moduleAlias, entity.entityAlias(), entity.actions()),
                                entity.views(),
                                entity.associationViews()
                        ))
                        .toList(),
                descriptor.relations(),
                descriptor.references(),
                descriptor.associationViews()
        );
    }

    private List<DynamicActionDescriptor> visibleModuleActions(String moduleAlias,
                                                               List<DynamicActionDescriptor> actions) {
        return actions.stream()
                .filter(action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, action.code(), Set.of()).available())
                .toList();
    }

    private List<DynamicActionDescriptor> visibleEntityActions(String moduleAlias,
                                                               String entityAlias,
                                                               List<DynamicActionDescriptor> actions) {
        return actions.stream()
                .filter(action -> recordService.actionAuthorizationAvailability(
                        moduleAlias, entityAlias, action.code(), Set.of()).available())
                .toList();
    }

    private void requireTenantContext() {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException("page bootstrap requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
    }
}
