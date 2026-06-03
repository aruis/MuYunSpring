package net.ximatai.muyun.spring.boot.dynamic;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

final class DynamicRecordJsonDeserializer extends JsonDeserializer<DynamicRecord> {
    private final DynamicRecordService recordService;

    DynamicRecordJsonDeserializer(DynamicRecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    public DynamicRecord deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        DynamicRecord record = recordService.mainEntity(moduleAlias).newRecord();
        JsonNode root = parser.getCodec().readTree(parser);
        JsonNode id = root.get("id");
        if (id != null && !id.isNull()) {
            record.setId(id.asText());
        }
        JsonNode version = root.get("version");
        if (version != null && !version.isNull()) {
            record.setVersion(version.asInt());
        }
        JsonNode values = root.has("values") ? root.get("values") : root;
        if (values == null || values.isNull()) {
            return record;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = values.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (isEnvelopeField(field.getKey())) {
                continue;
            }
            try (JsonParser valueParser = field.getValue().traverse(parser.getCodec())) {
                valueParser.nextToken();
                record.setValue(field.getKey(), context.readValue(valueParser, Object.class));
            }
        }
        return record;
    }

    private boolean isEnvelopeField(String fieldName) {
        return "id".equals(fieldName)
                || "version".equals(fieldName)
                || "values".equals(fieldName)
                || "children".equals(fieldName);
    }
}
