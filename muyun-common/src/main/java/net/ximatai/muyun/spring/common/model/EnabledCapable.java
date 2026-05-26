package net.ximatai.muyun.spring.common.model;

public interface EnabledCapable extends EntityContract {
    Boolean getEnabled();

    void setEnabled(Boolean enabled);
}
