package net.ximatai.muyun.spring.platform.code;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import net.ximatai.muyun.spring.common.time.BusinessTimeContext;
import net.ximatai.muyun.spring.common.time.BusinessTimeZoneResolver;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Dependent
public class CodeBusinessTimeService {
    private final PlatformTimeService platformTimeService;
    private final List<BusinessTimeZoneResolver> organizationTimeZoneResolvers;

    public CodeBusinessTimeService() {
        this(Clock.systemDefaultZone(), List.of());
    }

    public CodeBusinessTimeService(Clock clock) {
        this(clock, List.of());
    }

    public CodeBusinessTimeService(Clock clock, List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        this(new PlatformTimeService(clock), organizationTimeZoneResolvers);
    }

    public CodeBusinessTimeService(PlatformTimeService platformTimeService) {
        this(platformTimeService, List.of());
    }

    @Inject
    public CodeBusinessTimeService(PlatformTimeService platformTimeService,
                                   List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        this.platformTimeService = platformTimeService == null ? new PlatformTimeService() : platformTimeService;
        this.organizationTimeZoneResolvers = adapters(organizationTimeZoneResolvers);
    }

    public LocalDateTime resolveBusinessLocalDateTime(String organizationId, LocalDateTime explicitAt) {
        if (explicitAt != null) {
            return explicitAt;
        }
        return resolveBusinessLocalDateTime(organizationId, platformTimeService.now());
    }

    public LocalDateTime resolveBusinessLocalDateTime(String organizationId, Instant instant) {
        BusinessTimeContext context = BusinessTimeContext.ofOrganization(organizationId);
        for (BusinessTimeZoneResolver resolver : organizationTimeZoneResolvers) {
            java.util.Optional<ZoneId> resolved = resolver.resolveZoneId(context);
            ZoneId zoneId = (resolved == null ? java.util.Optional.<ZoneId>empty() : resolved)
                    .filter(Objects::nonNull)
                    .orElse(null);
            if (zoneId != null) {
                context = context.withZone(zoneId);
                break;
            }
        }
        return platformTimeService.resolveBusinessLocalDateTime(
                context,
                instant
        );
    }

    private static List<BusinessTimeZoneResolver> adapters(
            List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        if (organizationTimeZoneResolvers == null || organizationTimeZoneResolvers.isEmpty()) {
            return List.of();
        }
        return List.copyOf(organizationTimeZoneResolvers).stream()
                .map(CodeBusinessTimeService::adapter)
                .toList();
    }

    private static BusinessTimeZoneResolver adapter(CodeOrganizationTimeZoneResolver resolver) {
        return context -> {
            if (context == null || context.organizationId() == null || context.organizationId().isBlank()) {
                return java.util.Optional.empty();
            }
            java.util.Optional<ZoneId> candidate = resolver.resolveZoneId(context.organizationId());
            return (candidate == null ? java.util.Optional.<ZoneId>empty() : candidate)
                    .filter(Objects::nonNull);
        };
    }
}
