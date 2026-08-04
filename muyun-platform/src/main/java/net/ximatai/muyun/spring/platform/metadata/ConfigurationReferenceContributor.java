package net.ximatai.muyun.spring.platform.metadata;

import java.util.Optional;

/** Declares one configuration reference fact consumed by deletion governance. */
public interface ConfigurationReferenceContributor {
    ConfigurationReferenceTarget target();

    ConfigurationReference reference();

    Optional<String> findReferenceId(String targetId);
}
