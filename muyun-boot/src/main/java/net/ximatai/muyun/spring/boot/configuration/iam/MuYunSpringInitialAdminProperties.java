package net.ximatai.muyun.spring.boot.configuration.iam;

import net.ximatai.muyun.spring.iam.initialdata.PlatformInitialAdminSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 平台超级管理员的首次初始化配置，仅在初始数据链路读取。 */
@ConfigurationProperties("muyun.initial-admin")
public class MuYunSpringInitialAdminProperties implements PlatformInitialAdminSettings {
    private String initialPassword = "admin123";

    public String getInitialPassword() {
        return initialPassword;
    }

    @Override
    public String initialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        // 启动期尽早拒绝空密码，避免把不可登录的管理员状态写入数据库。
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalArgumentException("muyun.initial-admin.initial-password must not be blank");
        }
        this.initialPassword = initialPassword.trim();
    }
}
