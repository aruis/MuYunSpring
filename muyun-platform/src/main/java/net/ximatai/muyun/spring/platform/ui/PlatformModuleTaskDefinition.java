package net.ximatai.muyun.spring.platform.ui;

import java.util.List;

public record PlatformModuleTaskDefinition(
        String moduleAlias,
        String taskCode,
        String title,
        PlatformModuleTaskType taskType,
        PlatformModuleTaskOriginType originType,
        String originId,
        boolean managed,
        boolean system,
        boolean enabled,
        Integer sortOrder,
        String diagnosticPath,
        List<PlatformModuleTaskGuideDefinition> guides,
        List<PlatformModuleTaskCheckDefinition> checks
) {
    public PlatformModuleTaskDefinition {
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        taskCode = requireText(taskCode, "taskCode");
        title = normalize(title);
        taskType = taskType == null ? PlatformModuleTaskType.BUSINESS_COMPLETION : taskType;
        originType = originType == null ? PlatformModuleTaskOriginType.MANUAL : originType;
        originId = normalize(originId);
        sortOrder = sortOrder == null ? 0 : sortOrder;
        diagnosticPath = normalize(diagnosticPath);
        guides = guides == null ? List.of() : List.copyOf(guides);
        checks = checks == null || checks.isEmpty()
                ? List.of(new PlatformModuleTaskCheckDefinition(taskCode, PlatformTaskCheckType.MANUAL,
                null, null, null, null, null, 1, diagnosticPath))
                : List.copyOf(checks);
    }

    PlatformTaskBlock toBlock() {
        List<PlatformTaskCheckBlock> checkBlocks = checks.stream()
                .map(PlatformModuleTaskCheckDefinition::toBlock)
                .toList();
        PlatformTaskCheckBlock first = checkBlocks.getFirst();
        return new PlatformTaskBlock(null, taskCode, title, first.checkType(), first.associationViewCode(),
                first.queryTemplateId(), first.externalRecordIdKey(), diagnosticPath, first.targetModuleAlias(),
                first.generationRuleId(), first.expectedCount(), checkBlocks);
    }

    private static String requireText(String value, String name) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
