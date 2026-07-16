package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailability;
import net.ximatai.muyun.spring.boot.platform.PlatformRecordActionAvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Supplier;

public class BusinessRealtimeRecipientPolicyFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessRealtimeRecipientPolicyFactory.class);

    private final Supplier<PlatformRecordActionAvailabilityService> actionAvailabilityService;

    public BusinessRealtimeRecipientPolicyFactory(
            PlatformRecordActionAvailabilityService actionAvailabilityService) {
        this(() -> actionAvailabilityService);
    }

    public BusinessRealtimeRecipientPolicyFactory(
            Supplier<PlatformRecordActionAvailabilityService> actionAvailabilityService) {
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
                PlatformRecordActionAvailabilityService availabilityService = actionAvailabilityService.get();
                if (availabilityService == null) {
                    LOGGER.debug("Skip business realtime recipient because action availability service is missing");
                    return false;
                }
                PlatformRecordActionAvailability availability =
                        availabilityService.recordActions(normalizedModuleAlias, normalizedRecordId);
                return availability.actions().stream()
                        .anyMatch(action -> normalizedActionCode.equals(action.actionCode()) && action.available());
            } catch (RuntimeException exception) {
                LOGGER.debug("Failed to evaluate business realtime recipient policy: moduleAlias={}, recordId={}, "
                                + "actionCode={}",
                        normalizedModuleAlias, normalizedRecordId, normalizedActionCode, exception);
                return false;
            }
        };
    }
}
