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

public interface EnableWeb<T extends EntityContract & EnabledCapable, S extends EnableAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    default WebCountResponse enable(@PathVariable String id) {
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.ENABLE, id);
            return new WebCountResponse(service().enable(id));
        });
    }

    @PostMapping("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    default WebCountResponse disable(@PathVariable String id) {
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.DISABLE, id);
            return new WebCountResponse(service().disable(id));
        });
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
