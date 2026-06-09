package net.ximatai.muyun.spring.platform.code;

import lombok.Getter;

@Getter
public enum CodeIssueLogStatus {
    SUCCESS("success"),
    FAILED("failed");

    private final String code;

    CodeIssueLogStatus(String code) {
        this.code = code;
    }
}
