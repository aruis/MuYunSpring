package net.ximatai.muyun.spring.boot.configuration.runtime;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code muyun.runtime} 的平台治理默认值，不替代各能力的显式配置。 */
@ConfigurationProperties("muyun.runtime")
public class MuYunSpringRuntimeProperties {
    private PlatformRuntimeMode mode = PlatformRuntimeMode.PRODUCTION;

    public PlatformRuntimeMode getMode() {
        return mode;
    }

    public void setMode(PlatformRuntimeMode mode) {
        // 空值统一回落到生产治理口径，避免配置绑定缺失时放宽约束。
        this.mode = mode == null ? PlatformRuntimeMode.PRODUCTION : mode;
    }
}
