package net.ximatai.muyun.spring.boot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class MuYunSpringJacksonConfiguration {
    @Produces
    @ApplicationScoped
    public Module codeTitleEnumJacksonModule() {
        SimpleModule module = new SimpleModule("codeTitleEnum");
        module.addSerializer(CodeTitleEnum.class, new CodeTitleEnumJsonSerializer());
        module.setDeserializers(new CodeTitleEnumDeserializers());
        module.setMixInAnnotation(CurrentUser.class, CurrentUserJsonMixin.class);
        return module;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private abstract static class CurrentUserJsonMixin {
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
