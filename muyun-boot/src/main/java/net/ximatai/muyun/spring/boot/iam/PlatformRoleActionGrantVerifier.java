package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.role.RoleActionGrantVerifier;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataAction;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataActionService;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelationService;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformRoleActionGrantVerifier implements RoleActionGrantVerifier {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final PlatformModuleService moduleService;
    private final ModuleMetadataRelationService relationService;
    private final ModuleMetadataActionService actionService;
    private final StaticModuleActionRegistry staticModuleActionRegistry;

    public PlatformRoleActionGrantVerifier(PlatformModuleService moduleService,
                                           ModuleMetadataRelationService relationService,
                                           ModuleMetadataActionService actionService) {
        this(moduleService, relationService, actionService, new StaticModuleActionRegistry());
    }

    @Autowired
    public PlatformRoleActionGrantVerifier(PlatformModuleService moduleService,
                                           ModuleMetadataRelationService relationService,
                                           ModuleMetadataActionService actionService,
                                           StaticModuleActionRegistry staticModuleActionRegistry) {
        this.moduleService = moduleService;
        this.relationService = relationService;
        this.actionService = actionService;
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
        if ((module.getModuleKind() == null || module.getModuleKind() == ModuleKind.STATIC)
                && PlatformAction.fromCode(actionCode)
                .filter(action -> staticModuleActionRegistry.isGrantable(moduleAlias, action))
                .isPresent()) {
            return;
        }
        List<String> relationIds = relationService.list(Criteria.of().eq("moduleAlias", moduleAlias), ALL)
                .stream()
                .map(ModuleMetadataRelation::getId)
                .toList();
        if (relationIds.isEmpty()) {
            throw new PlatformException("role action requires configured module action: "
                    + moduleAlias + "." + actionCode);
        }
        boolean exists = actionService.listByRelationIds(relationIds)
                .stream()
                .filter(action -> actionCode.equals(action.getActionCode()))
                .filter(action -> Boolean.TRUE.equals(action.getEnabled()))
                .anyMatch(this::isActionAuth);
        if (!exists) {
            throw new PlatformException("role action requires configured module action: "
                    + moduleAlias + "." + actionCode);
        }
    }

    private boolean isActionAuth(ModuleMetadataAction action) {
        return action.getActionAuth() == null || Boolean.TRUE.equals(action.getActionAuth());
    }
}
