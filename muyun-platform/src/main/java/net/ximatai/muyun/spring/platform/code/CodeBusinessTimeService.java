package net.ximatai.muyun.spring.platform.code;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import net.ximatai.muyun.spring.common.time.BusinessTimeContext;
import net.ximatai.muyun.spring.common.time.BusinessTimeZoneResolver;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class CodeBusinessTimeService {
    private final PlatformTimeService platformTimeService;

    public CodeBusinessTimeService() {
        this(Clock.systemDefaultZone(), List.of());
    }

    public CodeBusinessTimeService(Clock clock) {
        this(clock, List.of());
    }

    public CodeBusinessTimeService(Clock clock, List<CodeOrganizationTimeZoneResolver> organizationTimeZoneResolvers) {
        this.platformTimeService = new PlatformTimeService(clock, adapters(organizationTimeZoneResolvers));
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
        return resolveBusinessLocalDateTime(organizationId, platformTimeService.now());
    }

    public LocalDateTime resolveBusinessLocalDateTime(String organizationId, Instant instant) {
        return platformTimeService.resolveBusinessLocalDateTime(
                BusinessTimeContext.ofOrganization(organizationId),
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
