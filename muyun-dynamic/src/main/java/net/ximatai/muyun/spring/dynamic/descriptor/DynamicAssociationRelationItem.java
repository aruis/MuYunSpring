package net.ximatai.muyun.spring.dynamic.descriptor;

public record DynamicAssociationRelationItem(
        String type,
        String code,
        String sourceModuleAlias,
        String sourceEntityAlias,
        String targetModuleAlias,
        String targetEntityAlias,
        String associationViewCode
) {
}
