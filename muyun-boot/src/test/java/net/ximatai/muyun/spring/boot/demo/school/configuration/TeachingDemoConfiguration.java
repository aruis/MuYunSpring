package net.ximatai.muyun.spring.boot.demo.school.configuration;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;

/** 仅测试使用的装配入口，模拟静态业务模块接入最终 Boot 运行时的方式。 */
@TestConfiguration(proxyBeanMethods = false)
@PlatformStaticApplication(alias = "education", title = "教学管理", sortOrder = 100)
@Profile("school-demo")
public class TeachingDemoConfiguration {
    @Bean
    TeachingDictionaryInitialDataProvider teachingDictionaryInitialDataProvider(
            DictionaryInitialDataDeclarations dictionaries) {
        return new TeachingDictionaryInitialDataProvider(dictionaries);
    }
}
