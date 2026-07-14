package net.ximatai.muyun.spring.ability.action;

public record ActionMessage(
        String code,
        String text,
        ActionMessageType type
) {
    public ActionMessage {
        code = normalize(code);
        text = normalize(text);
        type = type == null ? ActionMessageType.SUCCESS : type;
    }

    public static ActionMessage success(String code, String text) {
        return new ActionMessage(code, text, ActionMessageType.SUCCESS);
    }

    public static ActionMessage info(String code, String text) {
        return new ActionMessage(code, text, ActionMessageType.INFO);
    }

    public static ActionMessage warning(String code, String text) {
        return new ActionMessage(code, text, ActionMessageType.WARNING);
    }

    public static ActionMessage error(String code, String text) {
        return new ActionMessage(code, text, ActionMessageType.ERROR);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
