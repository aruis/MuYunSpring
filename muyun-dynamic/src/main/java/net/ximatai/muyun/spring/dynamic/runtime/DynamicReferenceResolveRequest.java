package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;

import java.util.List;

public record DynamicReferenceResolveRequest(
        DynamicReferenceResolveMode mode,
        DynamicReferenceMatchMode matchMode,
        String fuzzy,
        List<Object> values,
        Criteria criteria,
        PageRequest pageRequest,
        boolean includeProjections
) {
    private static final PageRequest DEFAULT_PAGE = PageRequest.of(1, 20);

    public DynamicReferenceResolveRequest {
        mode = mode == null ? DynamicReferenceResolveMode.QUERY : mode;
        matchMode = matchMode == null ? DynamicReferenceMatchMode.AUTO : matchMode;
        values = values == null ? List.of() : List.copyOf(values);
        pageRequest = pageRequest == null ? DEFAULT_PAGE : pageRequest;
    }

    public static DynamicReferenceResolveRequest query(String fuzzy) {
        return new DynamicReferenceResolveRequest(DynamicReferenceResolveMode.QUERY, DynamicReferenceMatchMode.AUTO,
                fuzzy, List.of(), null, DEFAULT_PAGE, true);
    }

    public static DynamicReferenceResolveRequest translate(List<Object> values) {
        return new DynamicReferenceResolveRequest(DynamicReferenceResolveMode.TRANSLATE, DynamicReferenceMatchMode.AUTO,
                null, values, null, DEFAULT_PAGE, true);
    }

    public DynamicReferenceResolveRequest withCriteria(Criteria value) {
        return new DynamicReferenceResolveRequest(mode, matchMode, fuzzy, values, value, pageRequest, includeProjections);
    }

    public DynamicReferenceResolveRequest withMatchMode(DynamicReferenceMatchMode value) {
        return new DynamicReferenceResolveRequest(mode, value, fuzzy, values, criteria, pageRequest, includeProjections);
    }

    public DynamicReferenceResolveRequest withPage(PageRequest value) {
        return new DynamicReferenceResolveRequest(mode, matchMode, fuzzy, values, criteria, value, includeProjections);
    }

    public DynamicReferenceResolveRequest withoutProjections() {
        return new DynamicReferenceResolveRequest(mode, matchMode, fuzzy, values, criteria, pageRequest, false);
    }
}
