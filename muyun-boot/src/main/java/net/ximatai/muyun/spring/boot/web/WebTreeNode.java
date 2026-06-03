package net.ximatai.muyun.spring.boot.web;

import java.util.List;

public record WebTreeNode<T>(T record, List<WebTreeNode<T>> children) {
    public WebTreeNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
