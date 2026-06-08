package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowParticipantPolicyCodecTest {
    @Test
    void shouldParseLegacyAndJsonUserPolicies() {
        assertThat(WorkflowParticipantPolicyCodec.parse("user:approver-1", "approve").userIds())
                .containsExactly("approver-1");
        assertThat(WorkflowParticipantPolicyCodec.parse("approver-1, approver-2", "approve").userIds())
                .containsExactly("approver-1", "approver-2");
        assertThat(WorkflowParticipantPolicyCodec.parse("""
                {"userIds":["approver-1","approver-2"]}
                """, "approve").userIds())
                .containsExactly("approver-1", "approver-2");
        assertThat(WorkflowParticipantPolicyCodec.parse("""
                {"rules":[{"type":"USER","targetId":"approver-1"}]}
                """, "approve").userIds())
                .containsExactly("approver-1");
        assertThat(WorkflowParticipantPolicyCodec.parse("""
                ["approver-1"]
                """, "approve").userIds())
                .containsExactly("approver-1");
    }

    @Test
    void shouldRejectUnsupportedParticipantTypes() {
        assertThatThrownBy(() -> WorkflowParticipantPolicyCodec.parse("""
                {"rules":[{"type":"ROLE","targetId":"finance"}]}
                """, "approve"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports user:<userId>");
        assertThatThrownBy(() -> WorkflowParticipantPolicyCodec.parse("role:finance", "approve"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("only supports user:<userId>");
    }

    @Test
    void shouldRequireSingleUserForCurrentRuntimeBoundary() {
        WorkflowParticipantPolicyCodec.ParticipantPolicy policy = WorkflowParticipantPolicyCodec.parse(
                "{\"userIds\":[\"approver-1\",\"approver-2\"]}", "approve");

        assertThatThrownBy(() -> policy.requireSingleUser("empty", "multi"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("multi");
    }
}
