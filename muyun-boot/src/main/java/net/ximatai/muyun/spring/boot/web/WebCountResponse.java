package net.ximatai.muyun.spring.boot.web;

public record WebCountResponse(int count, String message) {
    public WebCountResponse(int count) {
        this(count, null);
    }
}
