package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.event.ActionEventPayload;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActionEventPayloadTest {
    @Test
    void shouldBuildSuccessfulActionPayloadWithOnlyPlatformSummary() {
        Map<String, Object> payload = ActionEventPayload.executed(
                "DIALOG",
                "RECORD",
                "DIALOG",
                null,
                false,
                null,
                true,
                null
        );

        assertThat(payload)
                .containsEntry(ActionEventPayload.EXECUTOR_TYPE, "DIALOG")
                .containsEntry(ActionEventPayload.ACTION_LEVEL, "RECORD")
                .containsEntry(ActionEventPayload.RESULT_TYPE, "DIALOG")
                .containsEntry(ActionEventPayload.INTERACTION_ONLY, true)
                .doesNotContainKeys(ActionEventPayload.AVAILABLE, ActionEventPayload.RESULT);
    }

    @Test
    void shouldBuildFailedActionPayloadWithFailureContext() {
        Map<String, Object> payload = ActionEventPayload.failed(
                "SERVICE",
                "RECORD",
                true,
                "execute",
                "submit failed",
                IllegalStateException.class.getName()
        );

        assertThat(payload)
                .containsEntry(ActionEventPayload.EXECUTOR_TYPE, "SERVICE")
                .containsEntry(ActionEventPayload.ACTION_LEVEL, "RECORD")
                .containsEntry(ActionEventPayload.AVAILABLE, true)
                .containsEntry(ActionEventPayload.FAILURE_STAGE, "execute")
                .containsEntry(ActionEventPayload.ERROR_MESSAGE, "submit failed")
                .containsEntry(ActionEventPayload.ERROR_TYPE, IllegalStateException.class.getName());
        assertThat(ActionEventPayload.text(payload, ActionEventPayload.ERROR_MESSAGE)).isEqualTo("submit failed");
        assertThat(ActionEventPayload.bool(payload, ActionEventPayload.AVAILABLE)).isTrue();
    }
}
