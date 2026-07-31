package net.ximatai.muyun.spring.web;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class WebSupport<S> implements ScopedWeb<S> {
    @Autowired
    protected S service;

    @Override
    public S service() {
        return service;
    }
}
