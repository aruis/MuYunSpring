package net.ximatai.muyun.spring.platform.application;

/**
 * Contributes a domain-owned reference check before an application is deleted.
 *
 * <p>The application domain owns the deletion decision, while each owning domain
 * remains responsible for declaring its own {@code applicationAlias} facts.</p>
 */
public interface ApplicationReferenceContributor {
    String resourceKey();

    String resourceName();

    boolean hasReferenceTo(String applicationAlias);
}
