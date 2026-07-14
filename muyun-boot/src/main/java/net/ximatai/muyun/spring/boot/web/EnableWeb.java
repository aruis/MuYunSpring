package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface EnableWeb<T extends EntityContract & EnabledCapable, S extends EnableAbility<T>>
        extends ScopedWeb<S>, RecordLabelWeb<T> {
    @PostMapping("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    @StandardMutation(StandardMutationKind.ENABLE)
    default WebCountResponse enable(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            requireDataScopeRecord(PlatformAction.ENABLE, id);
            int count = service().enable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已启用"));
        }));
    }

    @PostMapping("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    @StandardMutation(StandardMutationKind.DISABLE)
    default WebCountResponse disable(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            requireDataScopeRecord(PlatformAction.DISABLE, id);
            int count = service().disable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已停用"));
        }));
    }

    private void requireDataScopeRecord(PlatformAction action, String id) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            dataScopeAbility.requireRecordScope(actionPolicy(action), java.util.List.of(id));
        }
    }

    private ActionExecutionPolicy actionPolicy(PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }
}
