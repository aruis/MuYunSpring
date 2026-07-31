package net.ximatai.muyun.spring.web;

import java.util.List;

public record WebTreeNode<T>(T record, List<WebTreeNode<T>> children) {
    public WebTreeNode {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
