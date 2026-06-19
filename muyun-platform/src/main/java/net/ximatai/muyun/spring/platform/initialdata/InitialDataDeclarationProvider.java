package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.initialdata.InitialDataPhase;

import java.util.List;

/**
 * Internal declaration source for platform scanned data or cross-domain relationship data.
 * Regular business services should implement InitialDataAbility on the service instead.
 */
public interface InitialDataDeclarationProvider {
    default String name() {
        return getClass().getName();
    }

    default int order() {
        return 100;
    }

    default InitialDataPhase phase() {
        return InitialDataPhase.SYSTEM_INITIAL_DATA;
    }

    List<InitialDataDeclaration<?>> declarations();
}
