package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

/** Runs every registered reference-target guard at the shared soft-delete boundary. */
public final class CompositeReferenceDeletionGuard implements ReferenceDeletionGuard {
    private final List<ReferenceDeletionGuard> guards;

    public CompositeReferenceDeletionGuard(List<ReferenceDeletionGuard> guards) {
        this.guards = guards == null ? List.of() : guards.stream()
                .filter(guard -> guard != null && guard != ReferenceDeletionGuard.NONE)
                .toList();
    }

    @Override
    public void beforeSoftDelete(CrudAbility<?> ability, EntityContract entity) {
        for (ReferenceDeletionGuard guard : guards) {
            guard.beforeSoftDelete(ability, entity);
        }
    }
}
