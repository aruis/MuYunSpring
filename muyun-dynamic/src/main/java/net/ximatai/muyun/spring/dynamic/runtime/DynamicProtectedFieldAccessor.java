package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.security.ProtectedFieldAccessor;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;

final class DynamicProtectedFieldAccessor implements ProtectedFieldAccessor<DynamicRecord> {
    private final FieldDefinition field;

    DynamicProtectedFieldAccessor(FieldDefinition field) {
        this.field = field;
    }

    @Override
    public String fieldName() {
        return field.fieldName();
    }

    @Override
    public FieldProtectionDefinition protection() {
        return field.protection();
    }

    @Override
    public Object get(DynamicRecord entity) {
        return entity.getValue(field.fieldName());
    }

    @Override
    public void set(DynamicRecord entity, Object value) {
        entity.putPlatformValue(field.fieldName(), value);
    }

    @Override
    public String signatureFieldName() {
        return FieldCompanionRules.signatureFieldName(field.fieldName());
    }

    @Override
    public Object getSignature(DynamicRecord entity) {
        return entity.getPlatformValue(signatureFieldName());
    }

    @Override
    public void setSignature(DynamicRecord entity, String signature) {
        entity.putPlatformValue(signatureFieldName(), signature);
    }

    @Override
    public boolean shouldWrite(DynamicRecord entity) {
        return entity.explicitFieldCodes().contains(field.fieldName()) || entity.getPlatformValues().containsKey(field.fieldName());
    }
}
