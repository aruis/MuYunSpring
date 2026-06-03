package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

public interface TreeWeb<T extends EntityContract & TreeCapable, S extends TreeAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/tree")
    default WebListResponse<T> tree() {
        return webScope(() -> new WebListResponse<>(service().children(TreeAbility.ROOT_ID)));
    }

    @PostMapping("/tree/{id}")
    default WebListResponse<T> tree(@PathVariable String id) {
        return webScope(() -> {
            T root = service().select(id);
            if (root == null) {
                return new WebListResponse<>(List.of());
            }
            List<T> rows = new ArrayList<>();
            rows.add(root);
            appendDescendants(root.getId(), rows);
            return new WebListResponse<>(rows);
        });
    }

    private void appendDescendants(String parentId, List<T> rows) {
        for (T child : service().children(parentId)) {
            rows.add(child);
            appendDescendants(child.getId(), rows);
        }
    }
}
