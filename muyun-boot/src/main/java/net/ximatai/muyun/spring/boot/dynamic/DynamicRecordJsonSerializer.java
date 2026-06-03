package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;

import java.io.IOException;

final class DynamicRecordJsonSerializer extends JsonSerializer<DynamicRecord> {
    @Override
    public void serialize(DynamicRecord record, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        generator.writeObject(DynamicRecordResponse.from(record));
    }
}
