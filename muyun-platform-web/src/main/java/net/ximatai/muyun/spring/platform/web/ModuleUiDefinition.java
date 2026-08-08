package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ModuleUiDefinition(String moduleAlias,
                                 List<ViewDefinition> views,
                                 List<UiActionDefinition> actions,
                                 ScopedListWorkspaceDefinition scopedListWorkspace) {
    public ModuleUiDefinition {
        moduleAlias = PlatformNameRules.requireModuleAlias(moduleAlias);
        views = views == null ? List.of() : List.copyOf(views);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public ModuleUiDefinition(String moduleAlias, List<ViewDefinition> views) {
        this(moduleAlias, views, List.of(), null);
    }

    public ModuleUiDefinition(String moduleAlias, List<ViewDefinition> views, List<UiActionDefinition> actions) {
        this(moduleAlias, views, actions, null);
    }

    public static Builder builder(String moduleAlias) {
        return new Builder(moduleAlias);
    }

    public static final class Builder {
        private final String moduleAlias;
        private final List<ViewDefinition> views = new ArrayList<>();
        private final List<UiActionDefinition> actions = new ArrayList<>();
        private ScopedListWorkspaceDefinition scopedListWorkspace;

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

        public Builder formView(String viewCode, Consumer<ViewDefinition.Builder> customizer) {
            ViewDefinition.Builder builder = ViewDefinition.form(viewCode);
            if (customizer != null) {
                customizer.accept(builder);
            }
            views.add(builder.build());
            return this;
        }

        public Builder typedTextConfirmation(String actionCode, String requiredField) {
            actions.add(UiActionDefinition.typedTextConfirmation(actionCode, requiredField));
            return this;
        }

        public Builder scopedListWorkspace(String scopeModuleAlias, String scopeField,
                                           String scopeTitle, String scopeSearchPlaceholder) {
            scopedListWorkspace = new ScopedListWorkspaceDefinition(scopeModuleAlias, scopeField,
                    scopeTitle, scopeSearchPlaceholder);
            return this;
        }

        public ModuleUiDefinition build() {
            return new ModuleUiDefinition(moduleAlias, views, actions, scopedListWorkspace);
        }
    }
}
