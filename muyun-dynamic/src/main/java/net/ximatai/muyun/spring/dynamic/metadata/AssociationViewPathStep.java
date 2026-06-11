package net.ximatai.muyun.spring.dynamic.metadata;

public record AssociationViewPathStep(
        AssociationViewPathStepType type,
        String code,
        String sourceEntityAlias,
        String targetModuleAlias,
        String targetEntityAlias
) {
    public AssociationViewPathStep {
        type = type == null ? AssociationViewPathStepType.RELATION : type;
    }

    public static AssociationViewPathStep relation(String code,
                                                   String sourceEntityAlias,
                                                   String targetModuleAlias,
                                                   String targetEntityAlias) {
        return new AssociationViewPathStep(AssociationViewPathStepType.RELATION, code, sourceEntityAlias,
                targetModuleAlias, targetEntityAlias);
    }

    public static AssociationViewPathStep reference(String fieldName,
                                                    String sourceEntityAlias,
                                                    String targetModuleAlias,
                                                    String targetEntityAlias) {
        return new AssociationViewPathStep(AssociationViewPathStepType.REFERENCE, fieldName, sourceEntityAlias,
                targetModuleAlias, targetEntityAlias);
    }
}
