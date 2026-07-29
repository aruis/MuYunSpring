package net.ximatai.muyun.spring.boot.platform;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.List;

/** Scans Spring-managed application declarations without coupling them to a business service. */
public class StaticApplicationDefinitionScanner {
    private final ApplicationContext applicationContext;

    public StaticApplicationDefinitionScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<StaticApplicationDefinition> scan() {
        return java.util.Arrays.stream(applicationContext.getBeanNamesForAnnotation(PlatformStaticApplication.class))
                .map(applicationContext::getBean)
                .map(AopUtils::getTargetClass)
                .map(beanClass -> AnnotationUtils.findAnnotation(beanClass, PlatformStaticApplication.class))
                .filter(java.util.Objects::nonNull)
                .map(application -> StaticApplicationDefinition.of(
                        application.alias(), application.title(), application.sortOrder()))
                .toList();
    }
}
