package net.ximatai.muyun.spring.platform.config;

public enum LowCodePackageDependencyType {
    MODULE,
    DICTIONARY,
    MEASURE_UNIT,
    CURRENCY,
    EXCHANGE_RATE_TYPE,
    ACTION,
    WORKFLOW,
    FILE_SERVICE,
    EXTERNAL;

    public boolean platformResolvedByDefault() {
        return switch (this) {
            case MODULE, DICTIONARY, MEASURE_UNIT, CURRENCY, EXCHANGE_RATE_TYPE, ACTION -> true;
            case WORKFLOW, FILE_SERVICE, EXTERNAL -> false;
        };
    }
}
