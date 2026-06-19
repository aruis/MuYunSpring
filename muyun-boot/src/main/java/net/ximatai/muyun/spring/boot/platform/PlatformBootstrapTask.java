package net.ximatai.muyun.spring.boot.platform;

public interface PlatformBootstrapTask {
    default String name() {
        return getClass().getName();
    }

    default int order() {
        return 100;
    }

    void run();
}
