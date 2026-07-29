package net.ximatai.muyun.spring.ability.reference;

/**
 * Declares how a reference participates when its target enters soft deletion.
 * Lifecycle ownership and cascading remain the concern of child relations.
 */
public enum ReferenceTargetDeletionPolicy {
    PRESERVE_HISTORY,
    RESTRICT
}
