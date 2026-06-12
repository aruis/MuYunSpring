package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

public abstract class SystemStandardBusinessService<T extends EntityContract> extends AbstractAbilityService<T>
        implements SystemManagedAbility<T> {
    protected SystemStandardBusinessService(String moduleAlias, Class<T> modelClass, BaseDao<T, String> dao) {
        super(moduleAlias, modelClass, dao);
    }

    @Override
    public void beforeInsert(T entity) {
        validateBeforeSave(entity);
        validateBeforeInsert(entity);
    }

    @Override
    public void beforeUpdate(T entity) {
        requireSystemMutationContext();
        normalizeBeforeMutation(entity);
        validateBeforeSave(entity);
        validateBeforeUpdate(entity);
    }

    protected void validateBeforeSave(T entity) {
    }

    protected void validateBeforeInsert(T entity) {
    }

    protected void validateBeforeUpdate(T entity) {
    }
}
