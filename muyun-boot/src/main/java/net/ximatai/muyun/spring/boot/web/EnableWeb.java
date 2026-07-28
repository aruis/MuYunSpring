package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface EnableWeb<T extends EntityContract & EnabledCapable, S extends EnableAbility<T>>
        extends ScopedWeb<S>, RecordLabelWeb<T> {
    @PostMapping("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    @StandardMutation(StandardMutationKind.ENABLE)
    default int enable(@PathVariable String id, @RequestBody RecordActionWebRequest request) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            StaticStandardMutationSupport.requireDataScopeRecord(this, PlatformAction.ENABLE, id);
            T existing = service().select(id);
            return StandardMutationResultSupport.enabled(this, id, recordLabel(existing),
                    () -> service().enable(id, request.version()));
        }));
    }

    @PostMapping("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    @StandardMutation(StandardMutationKind.DISABLE)
    default int disable(@PathVariable String id, @RequestBody RecordActionWebRequest request) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            StaticStandardMutationSupport.requireDataScopeRecord(this, PlatformAction.DISABLE, id);
            T existing = service().select(id);
            return StandardMutationResultSupport.disabled(this, id, recordLabel(existing),
                    () -> service().disable(id, request.version()));
        }));
    }
}
