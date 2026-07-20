package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class WorkflowTaskActionRequest {
    private final String taskId;
    private final String operatorId;
    private final String targetAssigneeId;
    private final WorkflowAddSignMode addSignMode;
    private final WorkflowAddSignSegment addSignSegment;
    private final WorkflowRejectResubmitMode rejectResubmitMode;
    private final String reason;
    private final Instant operatedAt;
    private final String selectedRouteKey;
    private final String selectedReason;
    private final List<WorkflowManualRouteSelection> manualRouteSelections;
    private final String semanticJson;
    private final String layoutJson;

    private WorkflowTaskActionRequest(String taskId,
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
                                      String layoutJson) {
        manualRouteSelections = manualRouteSelections == null ? List.of() : List.copyOf(manualRouteSelections);
        semanticJson = blankToNull(semanticJson);
        layoutJson = blankToNull(layoutJson);
        this.taskId = taskId;
        this.operatorId = operatorId;
        this.targetAssigneeId = targetAssigneeId;
        this.addSignMode = addSignMode;
        this.addSignSegment = addSignSegment;
        this.rejectResubmitMode = rejectResubmitMode;
        this.reason = reason;
        this.operatedAt = operatedAt;
        this.selectedRouteKey = selectedRouteKey;
        this.selectedReason = selectedReason;
        this.manualRouteSelections = manualRouteSelections;
        this.semanticJson = semanticJson;
        this.layoutJson = layoutJson;
    }

    public String taskId() { return taskId; }
    public String operatorId() { return operatorId; }
    public String targetAssigneeId() { return targetAssigneeId; }
    public WorkflowAddSignMode addSignMode() { return addSignMode; }
    public WorkflowAddSignSegment addSignSegment() { return addSignSegment; }
    public WorkflowRejectResubmitMode rejectResubmitMode() { return rejectResubmitMode; }
    public String reason() { return reason; }
    public Instant operatedAt() { return operatedAt; }
    public String selectedRouteKey() { return selectedRouteKey; }
    public String selectedReason() { return selectedReason; }
    public List<WorkflowManualRouteSelection> manualRouteSelections() { return manualRouteSelections; }
    public String semanticJson() { return semanticJson; }
    public String layoutJson() { return layoutJson; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof WorkflowTaskActionRequest that)) return false;
        return Objects.equals(taskId, that.taskId)
                && Objects.equals(operatorId, that.operatorId)
                && Objects.equals(targetAssigneeId, that.targetAssigneeId)
                && addSignMode == that.addSignMode
                && Objects.equals(addSignSegment, that.addSignSegment)
                && rejectResubmitMode == that.rejectResubmitMode
                && Objects.equals(reason, that.reason)
                && Objects.equals(operatedAt, that.operatedAt)
                && Objects.equals(selectedRouteKey, that.selectedRouteKey)
                && Objects.equals(selectedReason, that.selectedReason)
                && Objects.equals(manualRouteSelections, that.manualRouteSelections)
                && Objects.equals(semanticJson, that.semanticJson)
                && Objects.equals(layoutJson, that.layoutJson);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, operatorId, targetAssigneeId, addSignMode, addSignSegment, rejectResubmitMode,
                reason, operatedAt, selectedRouteKey, selectedReason, manualRouteSelections, semanticJson, layoutJson);
    }

    @Override
    public String toString() {
        return "WorkflowTaskActionRequest[taskId=" + taskId
                + ", operatorId=" + operatorId
                + ", targetAssigneeId=" + targetAssigneeId
                + ", addSignMode=" + addSignMode
                + ", addSignSegment=" + addSignSegment
                + ", rejectResubmitMode=" + rejectResubmitMode
                + ", reason=" + reason
                + ", operatedAt=" + operatedAt
                + ", selectedRouteKey=" + selectedRouteKey
                + ", selectedReason=" + selectedReason
                + ", manualRouteSelections=" + manualRouteSelections
                + ", semanticJson=" + semanticJson
                + ", layoutJson=" + layoutJson + "]";
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
            validateActionSpecificFields();
            return new WorkflowTaskActionRequest(taskId, operatorId, targetAssigneeId, addSignMode, addSignSegment,
                    rejectResubmitMode, reason, operatedAt, selectedRouteKey, selectedReason, manualRouteSelections,
                    semanticJson, layoutJson);
        }

        private void validateActionSpecificFields() {
            int actionSpecificGroups = 0;
            actionSpecificGroups += targetAssigneeId == null ? 0 : 1;
            actionSpecificGroups += addSignMode == null && addSignSegment == null ? 0 : 1;
            actionSpecificGroups += rejectResubmitMode == null ? 0 : 1;
            if (actionSpecificGroups > 1) {
                throw new IllegalArgumentException(
                        "workflow transfer, add-sign and reject options cannot be combined");
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
