package net.ximatai.muyun.spring.iam.user;

public interface UserSessionLifecycleEventPublisher {
    UserSessionLifecycleEventPublisher NOOP = event -> {
    };

    void publish(UserSessionLifecycleEvent event);
}
