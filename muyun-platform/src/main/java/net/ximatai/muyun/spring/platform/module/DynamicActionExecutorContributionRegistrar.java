package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Registers code-owned dynamic actions after all executor beans have been assembled. */
@Service
public class DynamicActionExecutorContributionRegistrar implements PlatformBootstrapTask {
    private final ApplicationContext applicationContext;
    private final ModuleActionContributionRegistrar contributionRegistrar;

    public DynamicActionExecutorContributionRegistrar(ApplicationContext applicationContext,
                                                      ModuleActionContributionRegistrar contributionRegistrar) {
        this.applicationContext = applicationContext;
        this.contributionRegistrar = contributionRegistrar;
    }

    @Override
    public void run() {
        applicationContext.getBeansOfType(DynamicActionExecutor.class).values().forEach(this::register);
    }

    @Override
    public int order() {
        return 20;
    }

    private void register(DynamicActionExecutor executor) {
        Set<PlatformDynamicActionContribution> declarations = AnnotationUtils.getRepeatableAnnotations(
                AopUtils.getTargetClass(executor), PlatformDynamicActionContribution.class);
        if (declarations.isEmpty()) {
            return;
        }
        String executorKey = executor.executorKey();
        List<ModuleActionContribution> contributions = new ArrayList<>();
        for (PlatformDynamicActionContribution declaration : declarations) {
            contributions.add(new ModuleActionContribution(
                    declaration.moduleAlias(), blankToNull(declaration.entityAlias()), declaration.actionCode(),
                    blankToNull(declaration.permissionActionCode()), declaration.title(), declaration.category(),
                    declaration.actionLevel(), declaration.accessMode(), declaration.actionAuth(), declaration.dataAuth(),
                    declaration.defaultGrantPolicy(), blankToNull(declaration.availableExpression()),
                    blankToNull(declaration.unavailableMessage()), EntityActionExecutorType.SERVICE, executorKey,
                    ModuleActionSourceType.CODE_EXTENSION, sourceId(executorKey, declaration.moduleAlias()), null,
                    ModuleActionBindingType.DYNAMIC_ACTION_EXECUTOR, executorKey, executorKey, true
            ));
        }
        contributionRegistrar.registerAll(contributions);
    }

    private String sourceId(String executorKey, String moduleAlias) {
        return executorKey + ":" + moduleAlias;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
