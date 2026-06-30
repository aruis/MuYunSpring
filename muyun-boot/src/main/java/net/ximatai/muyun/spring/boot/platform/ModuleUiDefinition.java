package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ModuleUiDefinition(String moduleAlias,
                                 List<ViewDefinition> views) {
    public ModuleUiDefinition {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        views = views == null ? List.of() : List.copyOf(views);
    }

    public static Builder builder(String moduleAlias) {
        return new Builder(moduleAlias);
    }

    public static final class Builder {
        private final String moduleAlias;
        private final List<ViewDefinition> views = new ArrayList<>();

        private Builder(String moduleAlias) {
            this.moduleAlias = moduleAlias;
        }

        public Builder listView(Consumer<ViewDefinition.Builder> customizer) {
            ViewDefinition.Builder builder = ViewDefinition.list();
            if (customizer != null) {
                customizer.accept(builder);
            }
            views.add(builder.build());
            return this;
        }

        public Builder formView(Consumer<ViewDefinition.Builder> customizer) {
            ViewDefinition.Builder builder = ViewDefinition.form();
            if (customizer != null) {
                customizer.accept(builder);
            }
            views.add(builder.build());
            return this;
        }

        public ModuleUiDefinition build() {
            return new ModuleUiDefinition(moduleAlias, views);
        }
    }
}
