package net.ximatai.muyun.spring.boot.web;

public record WebQueryRequest(WebPageRequest page) {
    public WebPageRequest pageOrDefault() {
        return page == null ? WebPageRequest.DEFAULT : page;
    }
}
