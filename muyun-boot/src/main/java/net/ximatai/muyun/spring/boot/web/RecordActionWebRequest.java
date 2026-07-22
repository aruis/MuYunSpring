package net.ximatai.muyun.spring.boot.web;

/**
 * Client snapshot required by destructive or state-changing record actions.
 */
public record RecordActionWebRequest(Integer version) {
    public RecordActionWebRequest {
        if (version == null) {
            throw new IllegalArgumentException("version is required for record action");
        }
    }
}
