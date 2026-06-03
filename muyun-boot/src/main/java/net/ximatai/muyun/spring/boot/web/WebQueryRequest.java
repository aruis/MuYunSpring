package net.ximatai.muyun.spring.boot.web;

import java.util.List;

public record WebQueryRequest(WebPageRequest page,
                              List<WebQueryCondition> conditions,
                              List<WebSort> sorts) {
    public WebQueryRequest {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public WebPageRequest pageOrDefault() {
        return page == null ? WebPageRequest.DEFAULT : page;
    }
}
