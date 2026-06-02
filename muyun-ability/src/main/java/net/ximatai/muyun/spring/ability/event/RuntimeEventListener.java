package net.ximatai.muyun.spring.ability.event;

@FunctionalInterface
public interface RuntimeEventListener {
    void onRuntimeEvent(RuntimeEvent event);
}
