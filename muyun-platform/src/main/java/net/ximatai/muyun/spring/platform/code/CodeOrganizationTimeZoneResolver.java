package net.ximatai.muyun.spring.platform.code;

import java.time.ZoneId;
import java.util.Optional;

@FunctionalInterface
public interface CodeOrganizationTimeZoneResolver {
    Optional<ZoneId> resolveZoneId(String organizationId);
}
