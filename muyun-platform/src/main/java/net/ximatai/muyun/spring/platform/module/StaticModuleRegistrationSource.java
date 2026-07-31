package net.ximatai.muyun.spring.platform.module;

import java.util.List;

/** Supplies static module registrations without exposing any delivery-specific descriptor. */
public interface StaticModuleRegistrationSource {
    List<? extends StaticModuleRegistration> definitions();

    static StaticModuleRegistrationSource empty() {
        return List::of;
    }
}
