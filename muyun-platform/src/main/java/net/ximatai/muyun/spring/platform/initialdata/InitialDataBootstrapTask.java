package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.platform.runtime.PlatformBootstrapTask;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;

public class InitialDataBootstrapTask implements PlatformBootstrapTask {
    private final InitialDataExecutor initialDataExecutor;

    public InitialDataBootstrapTask(InitialDataExecutor initialDataExecutor) {
        this.initialDataExecutor = initialDataExecutor;
    }

    @Override
    public String name() {
        return "platform.initial-data";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public void run() {
        initialDataExecutor.initializeAll();
    }
}
