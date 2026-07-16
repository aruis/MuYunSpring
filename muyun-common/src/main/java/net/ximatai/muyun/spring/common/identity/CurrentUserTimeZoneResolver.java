package net.ximatai.muyun.spring.common.identity;

import java.time.ZoneId;
import java.util.Optional;

public interface CurrentUserTimeZoneResolver {
    CurrentUserTimeZoneResolver NONE = currentUser -> Optional.empty();

    Optional<ZoneId> resolveZoneId(CurrentUser currentUser);
}
