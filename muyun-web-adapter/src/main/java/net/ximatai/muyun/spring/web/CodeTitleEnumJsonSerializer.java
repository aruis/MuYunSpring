package net.ximatai.muyun.spring.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

import java.io.IOException;

final class CodeTitleEnumJsonSerializer extends JsonSerializer<CodeTitleEnum> {
    @Override
    public void serialize(CodeTitleEnum value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getCode());
    }
}
