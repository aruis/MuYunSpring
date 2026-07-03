package net.ximatai.muyun.spring.boot.web;

import jakarta.inject.Inject;

public abstract class WebSupport<S> implements ScopedWeb<S> {
    @Inject
    protected S service;

    @Override
    public S service() {
        return service;
    }
}
