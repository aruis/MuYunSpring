package net.ximatai.muyun.spring.platform.config;

public enum LowCodePackageDependencyType {
    MODULE,
    DICTIONARY,
    ACTION,
    WORKFLOW,
    FILE_SERVICE,
    EXTERNAL;

    public boolean platformResolvedByDefault() {
        return switch (this) {
            case MODULE, DICTIONARY, ACTION -> true;
            case WORKFLOW, FILE_SERVICE, EXTERNAL -> false;
        };
    }
}
