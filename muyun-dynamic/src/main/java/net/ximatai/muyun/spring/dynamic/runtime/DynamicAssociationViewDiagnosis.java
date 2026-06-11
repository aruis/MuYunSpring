package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicAssociationViewDescriptor;

public record DynamicAssociationViewDiagnosis(
        DynamicAssociationViewDescriptor view,
        Criteria associationCriteria,
        Criteria requestCriteria,
        Criteria targetCriteria,
        long targetCount,
        DynamicAssociationViewDiagnosisStatus status,
        String message
) {
}
