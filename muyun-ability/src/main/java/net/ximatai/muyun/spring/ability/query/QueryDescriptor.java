package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class QueryDescriptor {
    private final String scopeName;
    private final Map<String, QueryField> fields;
    private final Map<String, Function<Object, Criteria>> externalCriteriaResolvers;
    private final Sort[] defaultSorts;

    private QueryDescriptor(Builder builder) {
        this.scopeName = builder.scopeName;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.fields));
        this.externalCriteriaResolvers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.externalCriteriaResolvers));
        this.defaultSorts = builder.defaultSorts.toArray(Sort[]::new);
    }

    public static Builder builder(String scopeName) {
        return new Builder(scopeName);
    }

    public String scopeName() {
        return scopeName;
    }

    public QueryField field(String fieldName) {
        return fields.get(fieldName);
    }

    public List<QueryField> fields() {
        return List.copyOf(fields.values());
    }

    public List<QueryField> quickSearchFields() {
        return fields.values().stream().filter(QueryField::quickSearch).toList();
    }

    public Set<String> externalCriteriaKeys() {
        return externalCriteriaResolvers.keySet();
    }

    public Function<Object, Criteria> externalCriteriaResolver(String key) {
        return externalCriteriaResolvers.get(key);
    }

    public Sort[] defaultSorts() {
        return defaultSorts.clone();
    }

    public static final class Builder {
        private final String scopeName;
        private final Map<String, QueryField> fields = new LinkedHashMap<>();
        private final Map<String, Function<Object, Criteria>> externalCriteriaResolvers = new LinkedHashMap<>();
        private final List<Sort> defaultSorts = new ArrayList<>();

        private Builder(String scopeName) {
            if (scopeName == null || scopeName.isBlank()) {
                throw new IllegalArgumentException("query scope name must not be blank");
            }
            this.scopeName = scopeName;
        }

        public Builder field(QueryField field) {
            fields.put(field.fieldName(), field);
            return this;
        }

        public Builder externalCriteria(String key, Function<Object, Criteria> resolver) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("external query key must not be blank");
            }
            externalCriteriaResolvers.put(key, resolver);
            return this;
        }

        public Builder defaultSort(Sort sort) {
            defaultSorts.add(sort);
            return this;
        }

        public QueryDescriptor build() {
            return new QueryDescriptor(this);
        }
    }
}
