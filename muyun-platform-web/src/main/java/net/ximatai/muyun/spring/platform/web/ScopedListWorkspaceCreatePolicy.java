package net.ximatai.muyun.spring.platform.web;

/** Defines whether a list workspace may create records before its scope is selected. */
public enum ScopedListWorkspaceCreatePolicy {
    /** The module may create an unscoped record; the scope is only a list filter. */
    ALLOW_UNSCOPED,
    /** A selected scope is required so a new record always receives its owning reference. */
    REQUIRE_SCOPE
}
