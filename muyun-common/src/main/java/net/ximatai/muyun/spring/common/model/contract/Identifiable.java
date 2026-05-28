package net.ximatai.muyun.spring.common.model.contract;

/**
 * Minimal identity contract for platform records.
 */
public interface Identifiable {
    String getId();

    void setId(String id);
}
