package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringJacksonConfiguration {
    @Bean
    public Module codeTitleEnumJacksonModule() {
        SimpleModule module = new SimpleModule("codeTitleEnum");
        module.addSerializer(CodeTitleEnum.class, new CodeTitleEnumJsonSerializer());
        module.setDeserializers(new CodeTitleEnumDeserializers());
        return module;
    }

    private static final class CodeTitleEnumDeserializers extends SimpleDeserializers {
        @Override
        public JsonDeserializer<?> findEnumDeserializer(Class<?> type,
                                                        DeserializationConfig config,
                                                        BeanDescription beanDesc) {
            if (type != null && CodeTitleEnum.class.isAssignableFrom(type)) {
                return new CodeTitleEnumJsonDeserializer(type);
            }
            return null;
        }
    }
}
