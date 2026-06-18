package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.platform.initialdata.InitialDataAbility;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

public class InitialDataApplicationRunner implements ApplicationRunner, Ordered {
    private final InitialDataAbility initialDataAbility;

    public InitialDataApplicationRunner(InitialDataAbility initialDataAbility) {
        this.initialDataAbility = initialDataAbility;
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialDataAbility.initializeAll();
    }
}
