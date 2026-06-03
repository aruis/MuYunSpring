package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.capability.DataScopeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.util.List;

public interface DataScopeAbility<T extends EntityContract & DataScopeCapable> extends CrudAbility<T> {
    DataScopeCriteriaService getDataScopeCriteriaService();

    default DataScopeCriteriaResult readScope(PlatformAction action, Criteria criteria) {
        return getDataScopeCriteriaService().resolveReadScope(
                getModuleAlias(),
                action.code(),
                criteria == null ? Criteria.of() : criteria,
                CurrentUserContext.currentUser()
        );
    }

    default PageResult<T> pageQueryForAction(PlatformAction action,
                                             Criteria criteria,
                                             PageRequest pageRequest,
                                             Sort... sorts) {
        return pageQuery(readScope(action, criteria).criteria(), pageRequest, sorts);
    }

    default List<T> listForAction(PlatformAction action,
                                  Criteria criteria,
                                  PageRequest pageRequest,
                                  Sort... sorts) {
        return list(readScope(action, criteria).criteria(), pageRequest, sorts);
    }

    default long countForAction(PlatformAction action, Criteria criteria) {
        return count(readScope(action, criteria).criteria());
    }

    default T selectForAction(PlatformAction action, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        DataScopeCriteriaResult scoped = readScope(action, Criteria.of().eq(StandardEntitySchema.ID_FIELD, id));
        if (count(scoped.criteria()) == 0) {
            return null;
        }
        return select(id);
    }

    default List<T> sortedListForAction(PlatformAction action, Criteria criteria) {
        return listForAction(action, criteria, new PageRequest(0, Integer.MAX_VALUE),
                Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    default List<T> childrenForAction(PlatformAction action, String parentId) {
        if (!(this instanceof TreeAbility<?>)) {
            throw new IllegalStateException("childrenForAction requires TreeAbility: " + getModuleAlias());
        }
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!TreeAbility.ROOT_ID.equals(parentId) && selectForAction(action, parentId) == null) {
            return List.of();
        }
        Criteria criteria = Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
        return sortedListForAction(action, criteria);
    }

    @SuppressWarnings("unchecked")
    static <T extends EntityContract & DataScopeCapable> DataScopeAbility<T> cast(CrudAbility<?> ability) {
        return (DataScopeAbility<T>) ability;
    }
}
