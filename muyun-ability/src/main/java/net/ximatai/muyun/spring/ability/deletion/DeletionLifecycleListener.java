package net.ximatai.muyun.spring.ability.deletion;

/**
 * Opens one lightweight observer for a direct deletion operation.
 *
 * <p>Ability remains independent of platform persistence through this SPI. The
 * returned session is propagated explicitly by {@link DeletionContext}; a
 * listener must not use thread-bound state to reconstruct a deletion tree.</p>
 */
public interface DeletionLifecycleListener {
    DeletionLifecycleListener NONE = new DeletionLifecycleListener() {
    };

    default DeletionLifecycleSession open(DeletionResource root) {
        return DeletionLifecycleSession.NONE;
    }
}
