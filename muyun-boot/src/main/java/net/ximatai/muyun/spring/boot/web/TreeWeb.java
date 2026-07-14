package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface TreeWeb<T extends EntityContract & TreeCapable, S extends TreeAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    default int sort(HttpServletRequest httpRequest,
                                  @PathVariable String id,
                                  @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            requireSortInput(normalized);
            requireTreeSortScope(httpRequest, id, normalized);
            moveTree(httpRequest, id, normalized);
            return 1;
        });
    }

    @GetMapping("/tree")
    @ActionEndpoint(PlatformAction.TREE)
    default WebListResponse<?> tree(HttpServletRequest request,
                                    @RequestParam(defaultValue = "false") boolean flat) {
        return webScope(() -> {
            List<T> roots = treeChildren(request, TreeAbility.ROOT_ID);
            if (flat) {
                List<T> rows = new ArrayList<>();
                for (T root : roots) {
                    rows.add(root);
                    appendDescendants(request, root.getId(), rows);
                }
                return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
            }
            return new WebListResponse<>(roots.stream().map(root -> treeNode(request, root)).toList());
        });
    }

    @GetMapping("/tree/{id}")
    @ActionEndpoint(PlatformAction.TREE)
    default WebListResponse<?> tree(HttpServletRequest request,
                                    @PathVariable String id,
                                    @RequestParam(defaultValue = "false") boolean flat,
                                    @RequestParam(defaultValue = "true") boolean includeSelf) {
        return webScope(() -> {
            T root = treeSelect(request, id);
            if (root == null) {
                return new WebListResponse<>(List.of());
            }
            if (!flat) {
                if (includeSelf) {
                    return new WebListResponse<>(List.of(treeNode(request, root)));
                }
                return new WebListResponse<>(treeChildren(request, root.getId()).stream()
                        .map(child -> treeNode(request, child))
                        .toList());
            }
            List<T> rows = new ArrayList<>();
            if (includeSelf) {
                rows.add(root);
            }
            appendDescendants(request, root.getId(), rows);
            return new WebListResponse<>(WebOutputSupport.records(service(), rows, FieldOutputContext.VIEW));
        });
    }

    default void moveTree(HttpServletRequest request, String id, TreeSortWebRequest sortRequest) {
        service().moveInTree(id, sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId());
    }

    default T treeSelect(HttpServletRequest request, String id) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            T record = (T) dataScopeAbility.selectForAction(PlatformAction.TREE, id);
            return record;
        }
        return service().select(id);
    }

    default List<T> treeChildren(HttpServletRequest request, String parentId) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            List<T> records = (List<T>) dataScopeAbility.childrenForAction(PlatformAction.TREE, parentId);
            return records;
        }
        return service().children(parentId);
    }

    default T treeSortSelect(HttpServletRequest request, String id) {
        return service().select(id);
    }

    default List<T> treeSortChildren(HttpServletRequest request, String parentId) {
        return service().children(parentId);
    }

    private void appendDescendants(HttpServletRequest request, String parentId, List<T> rows) {
        for (T child : treeChildren(request, parentId)) {
            rows.add(child);
            appendDescendants(request, child.getId(), rows);
        }
    }

    private WebTreeNode<T> treeNode(HttpServletRequest request, T record) {
        return new WebTreeNode<>(WebOutputSupport.record(service(), record, FieldOutputContext.VIEW),
                treeChildren(request, record.getId()).stream().map(child -> treeNode(request, child)).toList());
    }

    default void requireTreeSortScope(HttpServletRequest request, String id, TreeSortWebRequest sortRequest) {
        if (!(service() instanceof DataScopeAbility<?> dataScopeAbility)) {
            return;
        }
        DataScopeAbility<?> dataScope = DataScopeAbility.cast(dataScopeAbility);
        Set<String> explicitIds = treeSortExplicitIds(id,
                sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId());
        DataScopeCriteriaResult scope = dataScope.requireRecordScopeResult(PlatformAction.SORT.executionPolicy(), explicitIds);
        Set<String> scopedIds = dataScope.withDataScopeTenant(scope,
                () -> treeSortScopeRecordIds(request, id,
                        sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId()));
        dataScope.requireRecordScopeResult(PlatformAction.SORT.executionPolicy(), scopedIds);
    }

    private Set<String> treeSortScopeRecordIds(HttpServletRequest request,
                                               String id,
                                               String previousId,
                                               String nextId,
                                               String parentId) {
        LinkedHashSet<String> recordIds = new LinkedHashSet<>(treeSortExplicitIds(id, previousId, nextId, parentId));
        T moving = treeSortSelect(request, id);
        if (moving == null) {
            return java.util.Collections.unmodifiableSet(recordIds);
        }
        String targetParentId = normalizeParentId(parentId);
        if (targetParentId == null) {
            targetParentId = neighborParentId(request, previousId);
        }
        if (targetParentId == null) {
            targetParentId = neighborParentId(request, nextId);
        }
        if (targetParentId == null) {
            targetParentId = normalizeParentId(moving.getParentId());
        }
        if (targetParentId == null) {
            targetParentId = TreeAbility.ROOT_ID;
        }
        if (!TreeAbility.ROOT_ID.equals(targetParentId)) {
            recordIds.add(targetParentId);
        }
        treeSortChildren(request, targetParentId).stream()
                .map(EntityContract::getId)
                .forEach(recordIds::add);
        return java.util.Collections.unmodifiableSet(recordIds);
    }

    private Set<String> treeSortExplicitIds(String id, String previousId, String nextId, String parentId) {
        LinkedHashSet<String> recordIds = new LinkedHashSet<>(normalizeIds(id, previousId, nextId));
        String normalizedParentId = normalizeParentId(parentId);
        if (normalizedParentId != null && !TreeAbility.ROOT_ID.equals(normalizedParentId)) {
            recordIds.add(normalizedParentId);
        }
        return java.util.Collections.unmodifiableSet(recordIds);
    }

    private String neighborParentId(HttpServletRequest request, String neighborId) {
        if (neighborId == null || neighborId.isBlank()) {
            return null;
        }
        T neighbor = treeSortSelect(request, neighborId);
        return neighbor == null ? null : normalizeParentId(neighbor.getParentId());
    }

    private String normalizeParentId(String parentId) {
        return parentId == null || parentId.isBlank() ? null : parentId;
    }

    private void requireSortInput(TreeSortWebRequest request) {
        if ((request.previousId() == null || request.previousId().isBlank())
                && (request.nextId() == null || request.nextId().isBlank())
                && (request.parentId() == null || request.parentId().isBlank())) {
            throw new IllegalArgumentException("tree sort requires previousId, nextId, or parentId");
        }
    }

    private Set<String> normalizeIds(String... ids) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        Arrays.stream(ids)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(normalized::add);
        return java.util.Collections.unmodifiableSet(normalized);
    }
}
