package net.ximatai.muyun.spring.common.time;

import java.time.ZoneId;

public record BusinessCalendar(
        String calendarId,
        ZoneId zoneId,
        boolean workingTimeAware
) {
    public static final String NATURAL_CALENDAR_ID = "natural";

    public BusinessCalendar {
        if (calendarId == null || calendarId.isBlank()) {
            throw new IllegalArgumentException("calendarId must not be blank");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("zoneId must not be null");
        }
    }
}
