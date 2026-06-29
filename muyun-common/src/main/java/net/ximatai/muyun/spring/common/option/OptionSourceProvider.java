package net.ximatai.muyun.spring.common.option;

public interface OptionSourceProvider {
    String sourceType();

    OptionSource source(OptionBinding binding);
}
