package net.ximatai.muyun.spring.common.option;

public interface OptionSourceProvider {
    boolean supports(OptionBinding binding);

    OptionSource source(OptionBinding binding);
}
