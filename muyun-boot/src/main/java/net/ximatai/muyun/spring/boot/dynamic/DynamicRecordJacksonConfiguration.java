package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DynamicRecordJacksonConfiguration {
    @Bean
    Module dynamicRecordJacksonModule(DynamicRecordService recordService) {
        SimpleModule module = new SimpleModule("dynamicRecord");
        module.addDeserializer(DynamicRecord.class, new DynamicRecordJsonDeserializer(recordService));
        module.addSerializer(DynamicRecord.class, new DynamicRecordJsonSerializer());
        return module;
    }
}
