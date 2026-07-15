package net.ximatai.muyun.spring.iam.user;

public interface UserSecurityEventPublisher {
    UserSecurityEventPublisher NOOP = event -> {
    };

    void publish(UserSecurityEvent event);
}
