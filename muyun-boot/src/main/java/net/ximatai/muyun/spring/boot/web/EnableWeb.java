package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

public interface EnableWeb<T extends EntityContract & EnabledCapable, S extends EnableAbility<T>> extends ScopedWeb<S> {
    @PostMapping("/enable/{id}")
    default WebCountResponse enable(@PathVariable String id) {
        return webScope(() -> new WebCountResponse(service().enable(id)));
    }

    @PostMapping("/disable/{id}")
    default WebCountResponse disable(@PathVariable String id) {
        return webScope(() -> new WebCountResponse(service().disable(id)));
    }
}
