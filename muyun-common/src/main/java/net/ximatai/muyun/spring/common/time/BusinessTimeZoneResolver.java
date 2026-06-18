package net.ximatai.muyun.spring.common.time;

import java.time.ZoneId;
import java.util.Optional;

@FunctionalInterface
public interface BusinessTimeZoneResolver {
    Optional<ZoneId> resolveZoneId(BusinessTimeContext context);
}
