package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataAbility;

public class InitialDataBootstrapTask implements PlatformBootstrapTask {
    private final InitialDataAbility initialDataAbility;

    public InitialDataBootstrapTask(InitialDataAbility initialDataAbility) {
        this.initialDataAbility = initialDataAbility;
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
        initialDataAbility.initializeAll();
    }
}
