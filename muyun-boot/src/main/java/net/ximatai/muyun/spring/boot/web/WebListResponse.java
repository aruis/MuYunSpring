package net.ximatai.muyun.spring.boot.web;

import java.util.List;

public record WebListResponse<T>(List<T> records) {
}
