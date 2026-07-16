package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailability;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;

import java.util.Objects;

public class BusinessRealtimeRecipientPolicyFactory {
    private final PlatformRecordActionAvailabilityService actionAvailabilityService;

    public BusinessRealtimeRecipientPolicyFactory(
            PlatformRecordActionAvailabilityService actionAvailabilityService) {
        this.actionAvailabilityService = Objects.requireNonNull(
                actionAvailabilityService, "actionAvailabilityService must not be null");
    }

    public BusinessRealtimeFanOutPublisher.RecipientPolicy recordAction(
            String moduleAlias,
            String recordId,
            String actionCode) {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new IllegalArgumentException("business realtime recipient moduleAlias must not be blank");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("business realtime recipient recordId must not be blank");
        }
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("business realtime recipient actionCode must not be blank");
        }
        String normalizedModuleAlias = moduleAlias.trim();
        String normalizedRecordId = recordId.trim();
        String normalizedActionCode = actionCode.trim();
        return currentUser -> {
            if (currentUser == null || currentUser.passwordChangeRequired()) {
                return false;
            }
            try {
                PlatformRecordActionAvailability availability =
                        actionAvailabilityService.recordActions(normalizedModuleAlias, normalizedRecordId);
                return availability.actions().stream()
                        .anyMatch(action -> normalizedActionCode.equals(action.actionCode()) && action.available());
            } catch (RuntimeException ignored) {
                return false;
            }
        };
    }
}
