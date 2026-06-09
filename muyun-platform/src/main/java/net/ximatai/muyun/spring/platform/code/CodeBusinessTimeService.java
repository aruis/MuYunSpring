package net.ximatai.muyun.spring.platform.code;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class CodeBusinessTimeService {
    private final Clock clock;
    private final List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers;

    public CodeBusinessTimeService() {
        this(Clock.systemDefaultZone(), List.of());
    }

    public CodeBusinessTimeService(Clock clock) {
        this(clock, List.of());
    }

    public CodeBusinessTimeService(Clock clock, List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.organizationTimeZoneResolvers = organizationTimeZoneResolvers == null
                ? List.of()
                : List.copyOf(organizationTimeZoneResolvers);
    }

    @Autowired
    public CodeBusinessTimeService(ObjectProvider<Clock> clockProvider,
                                   List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        this(clockProvider == null ? null : clockProvider.getIfAvailable(), organizationTimeZoneResolvers);
    }

    public LocalDateTime resolveBusinessLocalDateTime(String organizationId, LocalDateTime explicitAt) {
        if (explicitAt != null) {
            return explicitAt;
        }
        return resolveBusinessLocalDateTime(organizationId, clock.instant());
    }

    public LocalDateTime resolveBusinessLocalDateTime(String organizationId, Instant instant) {
        Instant effectiveInstant = instant == null ? clock.instant() : instant;
        return LocalDateTime.ofInstant(effectiveInstant, resolveZoneId(organizationId));
    }

    private ZoneId resolveZoneId(String organizationId) {
        if (organizationId != null && !organizationId.isBlank()) {
            for (CodeOrganizationTimeZoneResolver resolver : organizationTimeZoneResolvers) {
                java.util.Optional<ZoneId> candidate = resolver.resolveZoneId(organizationId);
                ZoneId resolved = (candidate == null ? java.util.Optional.<ZoneId>empty() : candidate)
                        .filter(Objects::nonNull)
                        .orElse(null);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return clock.getZone();
    }
}
