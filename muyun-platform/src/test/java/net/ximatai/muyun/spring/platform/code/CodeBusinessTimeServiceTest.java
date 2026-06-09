package net.ximatai.muyun.spring.platform.code;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CodeBusinessTimeServiceTest {

    @Test
    void shouldResolveBusinessTimeByOrganizationZoneWhenAvailable() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T16:30:00Z"), ZoneOffset.UTC);
        CodeBusinessTimeService service = new CodeBusinessTimeService(clock, List.of(organizationId ->
                "org-shanghai".equals(organizationId)
                        ? Optional.of(ZoneId.of("Asia/Shanghai"))
                        : Optional.empty()
        ));

        assertThat(service.resolveBusinessLocalDateTime("org-shanghai", clock.instant()))
                .isEqualTo(LocalDateTime.of(2027, 1, 1, 0, 30));
        assertThat(service.resolveBusinessLocalDateTime("org-unknown", clock.instant()))
                .isEqualTo(LocalDateTime.of(2026, 12, 31, 16, 30));
    }
}
