package net.ximatai.muyun.spring.common.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class NaturalBusinessCalendarService implements BusinessCalendarService {
    private final PlatformTimeService timeService;

    public NaturalBusinessCalendarService() {
        this(new PlatformTimeService());
    }

    public NaturalBusinessCalendarService(PlatformTimeService timeService) {
        this.timeService = timeService == null ? new PlatformTimeService() : timeService;
    }

    @Override
    public BusinessCalendar resolveCalendar(BusinessTimeContext context) {
        return new BusinessCalendar(
                BusinessCalendar.NATURAL_CALENDAR_ID,
                timeService.resolveZoneId(context),
                false
        );
    }

    @Override
    public Instant addElapsedTime(Instant startInclusive, Duration duration, BusinessTimeContext context) {
        Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        Objects.requireNonNull(duration, "duration must not be null");
        return startInclusive.plus(duration);
    }

    @Override
    public Duration elapsedTime(Instant startInclusive, Instant endExclusive, BusinessTimeContext context) {
        Objects.requireNonNull(startInclusive, "startInclusive must not be null");
        Objects.requireNonNull(endExclusive, "endExclusive must not be null");
        return Duration.between(startInclusive, endExclusive);
    }
}
