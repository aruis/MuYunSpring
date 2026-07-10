package net.ximatai.muyun.spring.ability.reference;

import java.util.List;

public interface ModuleReadProjectionContributor {
    default List<ModuleReadProjection> moduleReadProjections() {
        return List.of();
    }
}
