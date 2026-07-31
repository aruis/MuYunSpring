package net.ximatai.muyun.spring.web.endpoint;

import net.ximatai.muyun.spring.web.ScopedWeb;

import java.util.Objects;

/** Static service and request-scope adapter used by the source-neutral web dispatcher. */
public record StaticWebOperationTarget(String moduleAlias, ScopedWeb<?> anchor, Object service) {
    public StaticWebOperationTarget {
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new IllegalArgumentException("moduleAlias must not be blank");
        }
        anchor = Objects.requireNonNull(anchor, "anchor must not be null");
        service = Objects.requireNonNull(service, "service must not be null");
    }
}
