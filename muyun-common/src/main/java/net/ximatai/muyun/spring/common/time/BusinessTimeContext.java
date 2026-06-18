package net.ximatai.muyun.spring.common.time;

import java.time.ZoneId;

public record BusinessTimeContext(
        ZoneId explicitZoneId,
        String organizationId,
        String tenantId
) {
    public static BusinessTimeContext empty() {
        return new BusinessTimeContext(null, null, null);
    }

    public static BusinessTimeContext ofOrganization(String organizationId) {
        return new BusinessTimeContext(null, organizationId, null);
    }

    public static BusinessTimeContext ofZone(ZoneId zoneId) {
        return new BusinessTimeContext(zoneId, null, null);
    }

    public BusinessTimeContext withZone(ZoneId zoneId) {
        return new BusinessTimeContext(zoneId, organizationId, tenantId);
    }

    public BusinessTimeContext withOrganization(String value) {
        return new BusinessTimeContext(explicitZoneId, value, tenantId);
    }

    public BusinessTimeContext withTenant(String value) {
        return new BusinessTimeContext(explicitZoneId, organizationId, value);
    }
}
