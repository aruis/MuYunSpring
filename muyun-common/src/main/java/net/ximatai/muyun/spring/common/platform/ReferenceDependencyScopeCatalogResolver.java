package net.ximatai.muyun.spring.common.platform;

import java.util.List;

/**
 * Exposes metadata-confirmed reference dependency choices to authorization administration.
 *
 * <p>This is intentionally separate from {@link ReferenceDependencyScopeResolver}: one resolves a
 * runtime SQL plan, the other exposes safe configuration candidates. Both are implemented by the same
 * module runtime where applicable, without making IAM depend on dynamic metadata.</p>
 */
public interface ReferenceDependencyScopeCatalogResolver {
    List<ReferenceDependencyScopeCandidate> resolveCandidates(String moduleAlias);
}
