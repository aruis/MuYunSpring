package net.ximatai.muyun.spring.common.time;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformTimeServiceTest {
    @Test
    void shouldPreferExplicitZoneAndThenResolverBeforeClockZone() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T16:30:00Z"), ZoneOffset.UTC);
        PlatformTimeService service = new PlatformTimeService(clock, List.of(context ->
                "org-shanghai".equals(context.organizationId())
                        ? Optional.of(ZoneId.of("Asia/Shanghai"))
                        : Optional.empty()
        ));

        assertThat(service.resolveBusinessLocalDateTime(
                BusinessTimeContext.ofOrganization("org-shanghai"), clock.instant()))
                .isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 30));
        assertThat(service.resolveBusinessLocalDateTime(
                BusinessTimeContext.ofOrganization("org-unknown"), clock.instant()))
                .isEqualTo(LocalDateTime.of(2026, 12, 31, 16, 30));
        assertThat(service.resolveBusinessLocalDateTime(
                BusinessTimeContext.ofOrganization("org-shanghai").withZone(ZoneId.of("America/New_York")),
                clock.instant()))
                .isEqualTo(LocalDateTime.of(2026, 12, 31, 11, 30));
    }

    @Test
    void shouldConvertLocalDateClosedRangeToInstantRangeByBusinessZone() {
        PlatformTimeService service = new PlatformTimeService(Clock.systemUTC(), List.of(context ->
                Optional.of(ZoneId.of("Asia/Shanghai"))
        ));

        BusinessTimeRange range = service.localDateClosedRangeToInstantRange(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"),
                BusinessTimeContext.ofOrganization("org-shanghai"));

        assertThat(range.startInclusive()).isEqualTo(Instant.parse("2025-12-31T16:00:00Z"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2026-01-31T16:00:00Z"));
    }

    @Test
    void shouldUseExplicitDefaultZoneWhenResolversDoNotMatch() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T16:30:00Z"), ZoneOffset.UTC);
        PlatformTimeService service = new PlatformTimeService(
                clock,
                ZoneId.of("Asia/Shanghai"),
                List.of(context -> Optional.empty())
        );

        assertThat(service.resolveBusinessLocalDateTime(BusinessTimeContext.ofOrganization("org-unknown"), clock.instant()))
                .isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 30));
    }

    @Test
    void shouldRespectDstWhenConvertingLocalDateRange() {
        PlatformTimeService service = new PlatformTimeService(
                Clock.systemUTC(),
                ZoneId.of("America/New_York"),
                List.of()
        );

        BusinessTimeRange range = service.localDateClosedRangeToInstantRange(
                LocalDate.parse("2026-03-08"),
                LocalDate.parse("2026-03-08"),
                BusinessTimeContext.empty());

        assertThat(range.startInclusive()).isEqualTo(Instant.parse("2026-03-08T05:00:00Z"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2026-03-09T04:00:00Z"));
    }

    @Test
    void shouldRejectNullExplicitZone() {
        PlatformTimeService service = new PlatformTimeService(Clock.systemUTC());

        assertThatThrownBy(() -> service.toInstant(LocalDateTime.of(2026, 1, 1, 0, 0), (ZoneId) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("zoneId");
    }

    @Test
    void shouldRequireIanaZoneId() {
        assertThat(PlatformTimeService.requireIanaZoneId("Asia/Shanghai"))
                .isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThatThrownBy(() -> PlatformTimeService.requireIanaZoneId("+08:00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IANA");
        assertThatThrownBy(() -> PlatformTimeService.requireIanaZoneId("Z"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IANA");
    }
}
