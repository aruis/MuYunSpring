package net.ximatai.muyun.spring.platform.runtime;

public interface PlatformBootstrapTask {
    default String name() {
        return getClass().getName();
    }

    default int order() {
        return 100;
    }

    void run();
}
