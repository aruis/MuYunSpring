package net.ximatai.muyun.spring.platform.workflow;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkflowNodeInstanceStateService {
    public void applyActivation(List<WorkflowNodeInstance> nodes, WorkflowActivationResult activation, Instant operatedAt) {
        if (nodes == null || activation == null) {
            return;
        }
        Instant now = operatedAt == null ? Instant.now() : operatedAt;
        Set<String> activeNodes = new HashSet<>();
        activeNodes.addAll(activation.blockingApprovalNodeKeys());
        activeNodes.addAll(activation.blockingTaskNodeKeys());
        activeNodes.addAll(activation.waitingConvergeNodeKeys());
        Set<String> activatedNodes = new HashSet<>(activation.activatedNodeKeys());
        for (WorkflowNodeInstance node : nodes) {
            if (!activatedNodes.contains(node.getNodeKey())) {
                continue;
            }
            if (node.getActivatedAt() == null) {
                node.setActivatedAt(now);
            }
            if (activeNodes.contains(node.getNodeKey())) {
                node.setNodeStatus(WorkflowNodeStatus.ACTIVE);
            } else {
                node.setNodeStatus(WorkflowNodeStatus.COMPLETED);
                if (node.getCompletedAt() == null) {
                    node.setCompletedAt(now);
                }
            }
        }
    }
}
