package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.TreeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface TreeAbility<T extends TreeModel> extends CrudAbility<T> {
    String ROOT_ID = "root";

    default List<T> children(String parentId) {
        return getDao().query(Criteria.of().eq("parentId", parentId), new PageRequest(0, Integer.MAX_VALUE));
    }

    default List<String> ancestorIds(String id) {
        T current = select(id);
        if (current == null) {
            return List.of();
        }

        List<String> ancestors = new ArrayList<>();
        String parentId = current.getParentId();
        while (parentId != null && !parentId.isBlank() && !ROOT_ID.equals(parentId)) {
            T parent = select(parentId);
            if (parent == null) {
                break;
            }
            ancestors.add(parent.getId());
            parentId = parent.getParentId();
        }
        Collections.reverse(ancestors);
        return ancestors;
    }

    default List<String> ancestorIdsAndSelf(String id) {
        List<String> ids = new ArrayList<>(ancestorIds(id));
        ids.add(id);
        return ids;
    }
}
