package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface EnableWeb<T extends EntityContract & EnabledCapable, S extends EnableAbility<T>>
        extends ScopedWeb<S>, RecordLabelWeb<T> {
    @PostMapping("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    @StandardMutation(StandardMutationKind.ENABLE)
    default int enable(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            StaticStandardMutationSupport.requireDataScopeRecord(this, PlatformAction.ENABLE, id);
            return StaticStandardMutationSupport.enabled(this, id, () -> service().enable(id));
        }));
    }

    @PostMapping("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    @StandardMutation(StandardMutationKind.DISABLE)
    default int disable(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            StaticStandardMutationSupport.requireDataScopeRecord(this, PlatformAction.DISABLE, id);
            return StaticStandardMutationSupport.disabled(this, id, () -> service().disable(id));
        }));
    }
}
