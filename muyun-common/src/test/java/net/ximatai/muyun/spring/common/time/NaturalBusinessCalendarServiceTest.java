package net.ximatai.muyun.spring.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalBusinessCalendarServiceTest {
    @Test
    void shouldExposeNaturalCalendarWithBusinessZone() {
        PlatformTimeService timeService = new PlatformTimeService(
                Clock.systemUTC(),
                ZoneId.of("Asia/Shanghai"),
                List.of()
        );
        NaturalBusinessCalendarService service = new NaturalBusinessCalendarService(timeService);

        BusinessCalendar calendar = service.resolveCalendar(BusinessTimeContext.empty());

        assertThat(calendar.calendarId()).isEqualTo(BusinessCalendar.NATURAL_CALENDAR_ID);
        assertThat(calendar.zoneId()).isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(calendar.workingTimeAware()).isFalse();
    }

    @Test
    void shouldCalculateNaturalElapsedTimeWithoutSkippingNonWorkingTime() {
        NaturalBusinessCalendarService service = new NaturalBusinessCalendarService();
        Instant fridayEvening = Instant.parse("2026-06-05T18:00:00Z");

        Instant dueAt = service.addElapsedTime(fridayEvening, Duration.ofHours(48), BusinessTimeContext.empty());
        Duration elapsed = service.elapsedTime(fridayEvening, dueAt, BusinessTimeContext.empty());

        assertThat(dueAt).isEqualTo(Instant.parse("2026-06-07T18:00:00Z"));
        assertThat(elapsed).isEqualTo(Duration.ofHours(48));
    }
}
