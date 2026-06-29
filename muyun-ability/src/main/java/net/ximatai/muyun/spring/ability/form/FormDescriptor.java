package net.ximatai.muyun.spring.ability.form;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormDescriptor {
    private final String scopeName;
    private final String title;
    private final Map<String, FormField> fields;

    private FormDescriptor(Builder builder) {
        this.scopeName = builder.scopeName;
        this.title = builder.title;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.fields));
    }

    public static Builder builder(String scopeName) {
        return new Builder(scopeName);
    }

    public String scopeName() {
        return scopeName;
    }

    public String title() {
        return title;
    }

    public List<FormField> fields() {
        return List.copyOf(fields.values());
    }

    public static final class Builder {
        private final String scopeName;
        private final Map<String, FormField> fields = new LinkedHashMap<>();
        private String title;

        private Builder(String scopeName) {
            if (scopeName == null || scopeName.isBlank()) {
                throw new IllegalArgumentException("form scope name must not be blank");
            }
            this.scopeName = scopeName;
        }

        public Builder title(String title) {
            this.title = title == null || title.isBlank() ? null : title.trim();
            return this;
        }

        public Builder field(FormField field) {
            fields.put(field.fieldName(), field);
            return this;
        }

        public FormDescriptor build() {
            return new FormDescriptor(this);
        }
    }
}
