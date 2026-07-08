package net.ximatai.muyun.spring.boot.platform;

import java.util.List;

public record PlatformRecordActionAvailability(
        String recordId,
        List<Action> actions
) {
    public PlatformRecordActionAvailability {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public record Action(
            String actionCode,
            boolean available,
            String reason
    ) {
    }
}
