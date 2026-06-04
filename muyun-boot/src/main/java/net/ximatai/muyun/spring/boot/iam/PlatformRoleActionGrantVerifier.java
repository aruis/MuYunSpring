package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.RoleActionGrantVerifier;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformRoleActionGrantVerifier implements RoleActionGrantVerifier {
    private final PlatformModuleService moduleService;
    private final PlatformModuleActionService moduleActionService;
    private final StaticModuleActionRegistry staticModuleActionRegistry;

    public PlatformRoleActionGrantVerifier(PlatformModuleService moduleService,
                                           PlatformModuleActionService moduleActionService) {
        this(moduleService, moduleActionService, new StaticModuleActionRegistry());
    }

    @Autowired
    public PlatformRoleActionGrantVerifier(PlatformModuleService moduleService,
                                           PlatformModuleActionService moduleActionService,
                                           StaticModuleActionRegistry staticModuleActionRegistry) {
        this.moduleService = moduleService;
        this.moduleActionService = moduleActionService;
        this.staticModuleActionRegistry = staticModuleActionRegistry == null
                ? new StaticModuleActionRegistry()
                : staticModuleActionRegistry;
    }

    @Override
    public void requireGrantable(String moduleAlias, String actionCode) {
        PlatformModule module = moduleService.resolveVisibleModule(moduleAlias);
        if (module == null) {
            throw new PlatformException("role action requires existing module: " + moduleAlias);
        }
        List<PlatformModuleAction> registeredActions = registeredModuleActions(moduleAlias);
        if (!registeredActions.isEmpty()) {
            if (registeredActions.stream().anyMatch(action -> actionCode.equals(action.getActionCode()))) {
                return;
            }
            throw new PlatformException("role action requires configured module action: "
                    + moduleAlias + "." + actionCode);
        }
        if ((module.getModuleKind() == null || module.getModuleKind() == ModuleKind.STATIC)
                && PlatformAction.fromCode(actionCode)
                .filter(action -> staticModuleActionRegistry.isGrantable(moduleAlias, action))
                .isPresent()) {
            return;
        }
        throw new PlatformException("role action requires configured module action: "
                + moduleAlias + "." + actionCode);
    }

    private List<PlatformModuleAction> registeredModuleActions(String moduleAlias) {
        return moduleActionService.listByModuleAliases(List.of(moduleAlias)).stream()
                .filter(action -> Boolean.TRUE.equals(action.getEnabled()))
                .filter(action -> action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth()))
                .toList();
    }
}
