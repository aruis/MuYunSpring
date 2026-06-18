package net.ximatai.muyun.spring.common.time;

import java.time.Duration;
import java.time.Instant;

public interface BusinessCalendarService {
    BusinessCalendar resolveCalendar(BusinessTimeContext context);

    Instant addElapsedTime(Instant startInclusive, Duration duration, BusinessTimeContext context);

    Duration elapsedTime(Instant startInclusive, Instant endExclusive, BusinessTimeContext context);
}
