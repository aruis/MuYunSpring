package net.ximatai.muyun.spring.ability.action;

import org.junit.jupiter.api.Test;

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
}
