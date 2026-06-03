package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

public interface TreeWeb<T extends EntityContract & TreeCapable, S extends TreeAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    default WebCountResponse sort(@PathVariable String id,
                                  @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            requireSortInput(normalized);
            service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            return new WebCountResponse(1);
        });
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    default WebListResponse<?> tree(@RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            List<T> roots = treeChildren(TreeAbility.ROOT_ID);
            if (flat) {
                List<T> rows = new ArrayList<>();
                for (T root : roots) {
                    rows.add(root);
                    appendDescendants(root.getId(), rows);
                }
                return new WebListResponse<>(rows);
            }
            return new WebListResponse<>(roots.stream().map(this::treeNode).toList());
        });
    }

    @GetMapping("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    default WebListResponse<?> tree(@PathVariable String id,
                                    @RequestParam(defaultValue = "false") boolean flat,
                                    @RequestParam(defaultValue = "true") boolean includeSelf) {
        return webScope(() -> {
            T root = treeSelect(id);
            if (root == null) {
                return new WebListResponse<>(List.of());
            }
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(treeNode(root)));
                }
                return new WebListResponse<>(treeChildren(root.getId()).stream().map(this::treeNode).toList());
            }
            List<T> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(root.getId(), rows);
            return new WebListResponse<>(rows);
        });
    }

    private void appendDescendants(String parentId, List<T> rows) {
        for (T child : treeChildren(parentId)) {
            rows.add(child);
            appendDescendants(child.getId(), rows);
        }
    }

    private WebTreeNode<T> treeNode(T record) {
        return new WebTreeNode<>(record, treeChildren(record.getId()).stream().map(this::treeNode).toList());
    }

    private T treeSelect(String id) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            T record = (T) dataScopeAbility.selectForAction(PlatformAction.TREE, id);
            return record;
        }
        return service().select(id);
    }

    private List<T> treeChildren(String parentId) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            List<T> records = (List<T>) dataScopeAbility.childrenForAction(PlatformAction.TREE, parentId);
            return records;
        }
        return service().children(parentId);
    }

    private void requireSortInput(TreeSortWebRequest request) {
        if ((request.previousId() == null || request.previousId().isBlank())
                && (request.nextId() == null || request.nextId().isBlank())
                && (request.parentId() == null || request.parentId().isBlank())) {
            throw new IllegalArgumentException("tree sort requires previousId, nextId, or parentId");
        }
    }
}
