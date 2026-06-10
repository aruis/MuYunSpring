package net.ximatai.muyun.spring.platform.impact;

import net.ximatai.muyun.spring.common.exception.PlatformException;

public record RecordOriginContext(
        RecordImpactType impactType,
        String sourceModuleAlias,
        String sourceRecordId,
        String targetModuleAlias,
        String generationRuleId,
        String actionCode,
        String batchId,
        String draftKey
) {
    public void validateForTarget(String moduleAlias) {
        requireImpactType(impactType);
        requireText(sourceModuleAlias, "sourceModuleAlias");
        requireText(sourceRecordId, "sourceRecordId");
        String validTargetModuleAlias = requireText(targetModuleAlias, "targetModuleAlias");
        String expectedModuleAlias = requireText(moduleAlias, "moduleAlias");
        if (!validTargetModuleAlias.equals(expectedModuleAlias)) {
            throw new PlatformException("Record origin targetModuleAlias mismatch: "
                    + validTargetModuleAlias + " != " + expectedModuleAlias);
        }
    }

    private static void requireImpactType(RecordImpactType value) {
        if (value == null) {
            throw new PlatformException("Record origin impactType must not be null");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("Record origin " + fieldName + " must not be blank");
        }
        return value;
    }
}
