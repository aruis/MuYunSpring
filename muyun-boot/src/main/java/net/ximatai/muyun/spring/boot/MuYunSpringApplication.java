package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 标准 MuYunSpring 宿主入口：扫描平台交付模块并启用基础 Repository、自动配置和调度。 */
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "net.ximatai.muyun.spring",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
        }
)
@EnableMuYunRepositories(basePackages = {
        "net.ximatai.muyun.spring.dynamic",
        "net.ximatai.muyun.spring.platform",
        "net.ximatai.muyun.spring.iam"
})
@EnableScheduling
public class MuYunSpringApplication {
    /** 启动不含演示业务依赖的标准平台宿主。 */
    public static void main(String[] args) {
        SpringApplication.run(MuYunSpringApplication.class, args);
    }
}
