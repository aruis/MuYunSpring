package net.ximatai.muyun.spring.boot.web;

public record WebPageRequest(int pageNum, int pageSize) {
    public static final WebPageRequest DEFAULT = new WebPageRequest(1, 20);

    public WebPageRequest {
        if (pageNum <= 0) {
            pageNum = DEFAULT.pageNum();
        }
        if (pageSize <= 0) {
            pageSize = DEFAULT.pageSize();
        }
        if (pageSize > 500) {
            pageSize = 500;
        }
    }
}
