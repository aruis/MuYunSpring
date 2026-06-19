package net.ximatai.muyun.spring.platform.initialdata;

import java.util.List;

public interface InitialDataContribution {
    default String name() {
        return getClass().getName();
    }

    default int order() {
        return 100;
    }

    List<InitialDataDeclaration<?>> declarations();
}
