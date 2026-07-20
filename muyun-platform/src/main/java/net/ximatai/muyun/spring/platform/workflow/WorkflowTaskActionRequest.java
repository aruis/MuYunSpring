package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowTaskActionRequest(
        String taskId,
        String operatorId,
        String targetAssigneeId,
        WorkflowAddSignMode addSignMode,
        WorkflowAddSignSegment addSignSegment,
        WorkflowRejectResubmitMode rejectResubmitMode,
        String reason,
        Instant operatedAt,
        String selectedRouteKey,
        String selectedReason,
        List<WorkflowManualRouteSelection> manualRouteSelections,
        String semanticJson,
        String layoutJson
) {
    public WorkflowTaskActionRequest {
        manualRouteSelections = manualRouteSelections == null ? List.of() : List.copyOf(manualRouteSelections);
        semanticJson = blankToNull(semanticJson);
        layoutJson = blankToNull(layoutJson);
    }

    public static Builder builder(String taskId, String operatorId) {
        return new Builder(taskId, operatorId);
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason) {
        return builder(taskId, operatorId).reason(reason).build();
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason,
                                                     String selectedRouteKey, String selectedReason) {
        return builder(taskId, operatorId)
                .reason(reason)
                .selectedRoute(selectedRouteKey, selectedReason)
                .build();
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason,
                                                     List<WorkflowManualRouteSelection> manualRouteSelections) {
        return builder(taskId, operatorId)
                .reason(reason)
                .manualRouteSelections(manualRouteSelections)
                .build();
    }

    public static WorkflowTaskActionRequest reject(String taskId, String operatorId,
                                                   WorkflowRejectResubmitMode rejectResubmitMode,
                                                   String reason) {
        return builder(taskId, operatorId)
                .rejectResubmitMode(rejectResubmitMode)
                .reason(reason)
                .build();
    }

    public static WorkflowTaskActionRequest transfer(String taskId, String operatorId,
                                                     String targetAssigneeId, String reason) {
        return builder(taskId, operatorId)
                .targetAssigneeId(targetAssigneeId)
                .reason(reason)
                .build();
    }

    public static WorkflowTaskActionRequest addSign(String taskId, String operatorId,
                                                    WorkflowAddSignSegment addSignSegment,
                                                    String reason) {
        return builder(taskId, operatorId)
                .addSignSegment(addSignSegment)
                .reason(reason)
                .build();
    }

    public static final class Builder {
        private final String taskId;
        private final String operatorId;
        private String targetAssigneeId;
        private WorkflowAddSignMode addSignMode;
        private WorkflowAddSignSegment addSignSegment;
        private WorkflowRejectResubmitMode rejectResubmitMode;
        private String reason;
        private Instant operatedAt;
        private String selectedRouteKey;
        private String selectedReason;
        private List<WorkflowManualRouteSelection> manualRouteSelections = List.of();
        private String semanticJson;
        private String layoutJson;

        private Builder(String taskId, String operatorId) {
            this.taskId = taskId;
            this.operatorId = operatorId;
        }

        public Builder targetAssigneeId(String targetAssigneeId) {
            this.targetAssigneeId = targetAssigneeId;
            return this;
        }

        public Builder addSignMode(WorkflowAddSignMode addSignMode) {
            this.addSignMode = addSignMode;
            return this;
        }

        public Builder addSignSegment(WorkflowAddSignSegment addSignSegment) {
            this.addSignSegment = addSignSegment;
            return this;
        }

        public Builder rejectResubmitMode(WorkflowRejectResubmitMode rejectResubmitMode) {
            this.rejectResubmitMode = rejectResubmitMode;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder operatedAt(Instant operatedAt) {
            this.operatedAt = operatedAt;
            return this;
        }

        public Builder selectedRoute(String selectedRouteKey, String selectedReason) {
            this.selectedRouteKey = selectedRouteKey;
            this.selectedReason = selectedReason;
            return this;
        }

        public Builder manualRouteSelections(List<WorkflowManualRouteSelection> manualRouteSelections) {
            this.manualRouteSelections = manualRouteSelections == null ? List.of() : List.copyOf(manualRouteSelections);
            return this;
        }

        public Builder designerSnapshot(String semanticJson, String layoutJson) {
            this.semanticJson = semanticJson;
            this.layoutJson = layoutJson;
            return this;
        }

        public WorkflowTaskActionRequest build() {
            return new WorkflowTaskActionRequest(taskId, operatorId, targetAssigneeId, addSignMode, addSignSegment,
                    rejectResubmitMode, reason, operatedAt, selectedRouteKey, selectedReason, manualRouteSelections,
                    semanticJson, layoutJson);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
