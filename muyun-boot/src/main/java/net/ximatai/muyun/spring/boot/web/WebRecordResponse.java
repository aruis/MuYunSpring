package net.ximatai.muyun.spring.boot.web;

public record WebRecordResponse<T>(T record, String message) {
}
