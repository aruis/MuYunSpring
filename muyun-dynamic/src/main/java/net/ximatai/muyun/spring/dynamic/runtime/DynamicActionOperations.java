package net.ximatai.muyun.spring.dynamic.runtime;

public interface DynamicActionOperations {
    DynamicRecord newRecord();

    DynamicRecord select(String id);

    int update(DynamicRecord record);

    int delete(String id);
}
