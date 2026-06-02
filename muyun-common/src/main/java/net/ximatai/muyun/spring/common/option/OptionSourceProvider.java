package net.ximatai.muyun.spring.common.option;

public interface OptionSourceProvider {
    String sourceType();

    boolean supports(OptionBinding binding);

    OptionSource source(OptionBinding binding);
}
