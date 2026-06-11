package net.ximatai.muyun.spring.platform.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.ArrayList;
import java.util.List;

final class PlatformTaskBlockLayoutResolver {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PlatformTaskBlockLayoutResolver() {
    }

    static List<PlatformTaskBlock> resolve(PlatformUiConfig config) {
        String layoutJson = config.getLayoutJson();
        if (layoutJson == null || layoutJson.isBlank()) {
            return List.of();
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(layoutJson);
        } catch (JsonProcessingException exception) {
            throw new PlatformException("UI config layout JSON cannot be decoded: " + config.getId());
        }
        JsonNode blocks = root.get("blocks");
        if (blocks == null || !blocks.isArray()) {
            return List.of();
        }
        ArrayList<PlatformTaskBlock> resolved = new ArrayList<>();
        for (JsonNode block : blocks) {
            if (block == null || !block.isObject() || !"taskPanel".equals(text(block, "type"))) {
                continue;
            }
            String key = text(block, "key");
            if (key == null) {
                continue;
            }
            resolved.add(new PlatformTaskBlock(
                    config.getId(),
                    key,
                    text(block, "title"),
                    taskCheckType(text(block, "checkType")),
                    text(block, "associationViewCode"),
                    text(block, "queryTemplateId"),
                    text(block, "externalRecordIdKey"),
                    text(block, "diagnosticPath")
            ));
        }
        return resolved;
    }

    private static PlatformTaskCheckType taskCheckType(String value) {
        if (value == null) {
            return PlatformTaskCheckType.MANUAL;
        }
        try {
            return PlatformTaskCheckType.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new PlatformException("Unsupported task check type: " + value);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }
}
