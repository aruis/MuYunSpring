package net.ximatai.muyun.spring.demo.school.configuration;

import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Import;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryInitialDataDeclarations;

/** 学校演示模块的 profile-gated 装配入口。 */
@AutoConfiguration
@Import(EducationApplication.class)
@Profile("school-demo")
@ComponentScan(basePackages = "net.ximatai.muyun.spring.demo.school")
@EnableMuYunRepositories(basePackages = "net.ximatai.muyun.spring.demo.school")
public class TeachingDemoConfiguration {
    @Bean
    TeachingDictionaryInitialDataProvider teachingDictionaryInitialDataProvider(
            DictionaryInitialDataDeclarations dictionaries) {
        return new TeachingDictionaryInitialDataProvider(dictionaries);
    }
}
