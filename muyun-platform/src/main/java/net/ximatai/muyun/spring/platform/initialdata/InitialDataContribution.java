package net.ximatai.muyun.spring.platform.initialdata;

public interface InitialDataContribution {
    default String name() {
        return getClass().getName();
    }

    default int order() {
        return 100;
    }

    void contribute(InitialDataContext context);
}
