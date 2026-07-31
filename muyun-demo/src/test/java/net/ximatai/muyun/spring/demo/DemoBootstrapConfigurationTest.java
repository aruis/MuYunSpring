package net.ximatai.muyun.spring.demo;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class DemoBootstrapConfigurationTest {
    @Test
    void shouldActivateOnlyAsPartOfSchoolDemoEnvironment() {
        Profile profile = AnnotationUtils.findAnnotation(DemoBootstrapConfiguration.class, Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("school-demo");
    }
}
