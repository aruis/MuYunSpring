package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.ability.CrudAbility;

/** Declares a static module backed by a service but intentionally without an HTTP controller. */
public interface StaticModuleServiceDeclaration {
    CrudAbility<?> staticModuleService();
}
