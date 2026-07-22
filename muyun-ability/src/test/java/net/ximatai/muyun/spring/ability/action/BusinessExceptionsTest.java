package net.ximatai.muyun.spring.ability.action;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionsTest {
    @Test
    void warningUsesUnprocessableEntityAndActionMessage() {
        BusinessException exception = BusinessExceptions.warning("demo.rule-denied", "规则不满足");

        assertThat(exception.httpStatus()).isEqualTo(422);
        assertThat(exception.actionMessage()).isEqualTo(
                new ActionMessage("demo.rule-denied", "规则不满足", ActionMessageType.WARNING));
    }

    @Test
    void conflictKeepsBusinessWarningMessageWithConflictStatus() {
        BusinessException exception = BusinessExceptions.conflict("demo.duplicate", "记录已存在");

        assertThat(exception.httpStatus()).isEqualTo(409);
        assertThat(exception.actionMessage().type()).isEqualTo(ActionMessageType.WARNING);
    }

    @Test
    void warningKeepsDisplayArgumentsSeparateFromTechnicalDetails() {
        BusinessException exception = BusinessExceptions.warning("demo.rule-denied", "记录 {recordTitle} 不满足规则",
                Map.of("recordTitle", "示例记录"));

        assertThat(exception.messageArgs()).containsExactlyEntriesOf(Map.of("recordTitle", "示例记录"));
        assertThat(exception.details()).isEmpty();
        assertThat(exception.actionMessage().messageArgs()).containsExactlyEntriesOf(Map.of("recordTitle", "示例记录"));
    }
}
