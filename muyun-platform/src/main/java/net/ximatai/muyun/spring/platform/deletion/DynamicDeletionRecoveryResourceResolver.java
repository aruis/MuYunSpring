package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/** Resolves dynamic resources through the current dynamic runtime, never as singleton Ability beans. */
@Component
public class DynamicDeletionRecoveryResourceResolver implements DeletionRecoveryResourceResolver {
    private final Optional<DynamicRecordService> dynamicRecords;

    public DynamicDeletionRecoveryResourceResolver(Optional<DynamicRecordService> dynamicRecords) {
        this.dynamicRecords = dynamicRecords == null ? Optional.empty() : dynamicRecords;
    }

    @Override
    public boolean supports(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        if (entry.getResourceEntityAlias() == null || entry.getResourceEntityAlias().isBlank()) {
            return false;
        }
        return dynamicRecords.map(records -> {
            try {
                records.entityDescriptor(entry.getResourceModuleAlias(), entry.getResourceEntityAlias());
                return true;
            } catch (RuntimeException ignored) {
                return false;
            }
        }).orElse(false);
    }

    @Override
    public Optional<SoftDeleteAbility<?>> resolve(DeletionEntry entry) {
        Objects.requireNonNull(entry, "deletionEntry must not be null");
        if (!supports(entry)) {
            return Optional.empty();
        }
        return dynamicRecords.map(records -> records.entity(
                entry.getResourceModuleAlias(), entry.getResourceEntityAlias()));
    }
}
