package net.ximatai.muyun.spring.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PlatformTimeService {
    private final Clock clock;
    private final ZoneId defaultZoneId;
    private final List<BusinessTimeZoneResolver> zoneResolvers;

    public PlatformTimeService() {
        this(Clock.systemDefaultZone(), List.of());
    }

    public PlatformTimeService(Clock clock) {
        this(clock, List.of());
    }

    public PlatformTimeService(Clock clock, List<BusinessTimeZoneResolver> zoneResolvers) {
        this(clock, null, zoneResolvers);
    }

    public PlatformTimeService(Clock clock, ZoneId defaultZoneId, List<BusinessTimeZoneResolver> zoneResolvers) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.defaultZoneId = defaultZoneId == null ? this.clock.getZone() : defaultZoneId;
        this.zoneResolvers = zoneResolvers == null ? List.of() : List.copyOf(zoneResolvers);
    }

    public Instant now() {
        return clock.instant();
    }

    public ZoneId resolveZoneId(BusinessTimeContext context) {
        BusinessTimeContext effectiveContext = context == null ? BusinessTimeContext.empty() : context;
        if (effectiveContext.explicitZoneId() != null) {
            return effectiveContext.explicitZoneId();
        }
        for (BusinessTimeZoneResolver resolver : zoneResolvers) {
            Optional<ZoneId> resolved = resolver.resolveZoneId(effectiveContext);
            ZoneId zoneId = (resolved == null ? Optional.<ZoneId>empty() : resolved)
                    .filter(Objects::nonNull)
                    .orElse(null);
            if (zoneId != null) {
                return zoneId;
            }
        }
        return defaultZoneId;
    }

    public LocalDateTime resolveBusinessLocalDateTime(BusinessTimeContext context, Instant instant) {
        Instant effectiveInstant = instant == null ? now() : instant;
        return LocalDateTime.ofInstant(effectiveInstant, resolveZoneId(context));
    }

    public Instant toInstant(LocalDateTime localDateTime, BusinessTimeContext context) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(resolveZoneId(context)).toInstant();
    }

    public Instant toInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        return toInstant(localDateTime, BusinessTimeContext.ofZone(Objects.requireNonNull(zoneId, "zoneId must not be null")));
    }

    public BusinessTimeRange localDateClosedRangeToInstantRange(LocalDate startInclusive,
                                                               LocalDate endInclusive,
                                                               BusinessTimeContext context) {
        if (startInclusive == null) {
            throw new IllegalArgumentException("startInclusive must not be null");
        }
        if (endInclusive == null) {
            throw new IllegalArgumentException("endInclusive must not be null");
        }
        if (endInclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("endInclusive must not be before startInclusive");
        }
        ZoneId zoneId = resolveZoneId(context);
        return new BusinessTimeRange(
                startInclusive.atStartOfDay(zoneId).toInstant(),
                endInclusive.plusDays(1).atStartOfDay(zoneId).toInstant()
        );
    }

    public static ZoneId requireIanaZoneId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("timeZone must be an IANA ZoneId");
        }
        try {
            ZoneId zoneId = ZoneId.of(value.trim());
            if (zoneId instanceof ZoneOffset) {
                throw new IllegalArgumentException("timeZone must be an IANA ZoneId");
            }
            return zoneId;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("timeZone must be an IANA ZoneId", e);
        }
    }
}
