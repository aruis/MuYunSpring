package net.ximatai.muyun.spring.platform.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class WorkflowParticipantPolicyCodec {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WorkflowParticipantPolicyCodec() {
    }

    static ParticipantPolicy parse(String policyText, String nodeKey) {
        if (policyText == null || policyText.isBlank()) {
            return new ParticipantPolicy(List.of());
        }
        String text = policyText.trim();
        if (!text.startsWith("{") && !text.startsWith("[")) {
            return parseLegacy(text, nodeKey);
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(text);
            LinkedHashSet<String> userIds = new LinkedHashSet<>();
            if (root.isArray()) {
                root.forEach(node -> appendRule(userIds, node, nodeKey));
            } else if (root.path("rules").isArray()) {
                root.path("rules").forEach(node -> appendRule(userIds, node, nodeKey));
            } else {
                appendCanonicalObject(userIds, root, nodeKey);
            }
            return new ParticipantPolicy(List.copyOf(userIds));
        } catch (PlatformException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PlatformException("workflow participant policy parse failed: " + nodeKey, ex);
        }
    }

    private static ParticipantPolicy parseLegacy(String text, String nodeKey) {
        String normalized = text;
        if (text.regionMatches(true, 0, "user:", 0, "user:".length())) {
            normalized = text.substring("user:".length()).trim();
        } else if (text.contains(":")) {
            throw unsupported(nodeKey);
        }
        LinkedHashSet<String> userIds = new LinkedHashSet<>();
        for (String part : normalized.split("[,;]")) {
            String userId = part.trim();
            if (!userId.isBlank()) {
                userIds.add(userId);
            }
        }
        return new ParticipantPolicy(List.copyOf(userIds));
    }

    private static void appendCanonicalObject(LinkedHashSet<String> userIds, JsonNode node, String nodeKey) {
        String type = normalizeType(textValue(node, "type", "participantType"));
        if (type != null && !"USER".equals(type)) {
            throw unsupported(nodeKey);
        }
        appendUserIds(userIds, node);
    }

    private static void appendRule(LinkedHashSet<String> userIds, JsonNode node, String nodeKey) {
        if (node.isTextual()) {
            addUserId(userIds, node);
            return;
        }
        String type = normalizeType(textValue(node, "type", "participantType"));
        if (type != null && !"USER".equals(type)) {
            throw unsupported(nodeKey);
        }
        appendUserIds(userIds, node);
    }

    private static void appendUserIds(LinkedHashSet<String> userIds, JsonNode node) {
        JsonNode userIdArray = node.path("userIds");
        if (userIdArray.isArray()) {
            for (JsonNode item : userIdArray) {
                addUserId(userIds, item);
            }
        }
        for (String field : List.of("userId", "targetId", "id", "value")) {
            addUserId(userIds, node.path(field));
        }
    }

    private static void addUserId(LinkedHashSet<String> userIds, JsonNode node) {
        if (node != null && !node.isMissingNode() && !node.isNull()) {
            String value = node.asText();
            if (value != null && !value.isBlank()) {
                userIds.add(value.trim());
            }
        }
    }

    private static String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return null;
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "USER" -> "USER";
            case "ROLE", "DEPT", "DEPARTMENT", "ORG", "ORGANIZATION", "RELATIVE", "INITIATOR_SELF" ->
                    type.trim().toUpperCase(Locale.ROOT);
            default -> type.trim().toUpperCase(Locale.ROOT);
        };
    }

    private static PlatformException unsupported(String nodeKey) {
        return new PlatformException("workflow participant policy only supports user:<userId> or user JSON: "
                + nodeKey);
    }

    record ParticipantPolicy(List<String> userIds) {
        ParticipantPolicy {
            userIds = userIds == null ? List.of() : List.copyOf(userIds);
        }

        String requireSingleUser(String emptyMessage, String multiMessage) {
            if (userIds.isEmpty()) {
                throw new PlatformException(emptyMessage);
            }
            if (userIds.size() > 1) {
                throw new PlatformException(multiMessage);
            }
            return userIds.getFirst();
        }
    }
}
