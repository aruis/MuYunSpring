package net.ximatai.muyun.spring.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

import java.io.IOException;

final class CodeTitleEnumJsonDeserializer extends JsonDeserializer<Enum<?>> {
    private final Class<?> enumType;

    CodeTitleEnumJsonDeserializer(Class<?> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Enum<?> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String code = parser.getValueAsString();
        if (code != null) {
            for (Object constant : enumType.getEnumConstants()) {
                CodeTitleEnum codeTitleEnum = (CodeTitleEnum) constant;
                if (code.equals(codeTitleEnum.getCode())) {
                    return (Enum<?>) constant;
                }
            }
        }
        Object value = context.weirdStringException(code, enumType, "not a valid CodeTitleEnum code");
        return (Enum<?>) value;
    }
}
