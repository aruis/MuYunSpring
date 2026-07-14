package net.ximatai.muyun.spring.ability.action;

public class ActionMessageReporter {
    public void success(String code, String text) {
        MutationContextHolder.current().ifPresent(context -> context.message(ActionMessage.success(code, text)));
    }

    public void info(String code, String text) {
        MutationContextHolder.current().ifPresent(context -> context.message(ActionMessage.info(code, text)));
    }

    public void warning(String code, String text) {
        MutationContextHolder.current().ifPresent(context -> context.message(ActionMessage.warning(code, text)));
    }

    public void error(String code, String text) {
        MutationContextHolder.current().ifPresent(context -> context.message(ActionMessage.error(code, text)));
    }
}
