package net.ximatai.muyun.spring.ability.option;

public interface StaticOptionFieldValueValidator {
    StaticOptionFieldValueValidator NONE = (modelClass, entity) -> {
    };

    void validate(Class<?> modelClass, Object entity);
}
