package net.ximatai.muyun.spring.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 框架仓库的本地宿主；平台装配由 muyun-spring-boot-starter 自动提供。 */
@SpringBootApplication
public class MuYunSpringApplication {
    /** 启动不含演示业务依赖的标准平台宿主。 */
    public static void main(String[] args) {
        SpringApplication.run(MuYunSpringApplication.class, args);
    }
}
