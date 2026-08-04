package net.ximatai.muyun.spring.platform.metadata;

/** A concrete configuration resource and field that consumes another configuration record. */
public record ConfigurationReference(String resourceKey, String resourceName, String referenceField) {
}
