package net.ximatai.muyun.spring.web;

import java.util.List;

public record WebListResponse<T>(List<T> records) {
}
