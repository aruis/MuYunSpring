package net.ximatai.muyun.spring.ability.deletion;

/** Whether a resource deletion is directly requested or inherited from a parent resource. */
public enum DeletionTrigger {
    DIRECT,
    CASCADE
}
