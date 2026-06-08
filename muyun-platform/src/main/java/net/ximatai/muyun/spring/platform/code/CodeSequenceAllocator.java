package net.ximatai.muyun.spring.platform.code;

public interface CodeSequenceAllocator {
    long allocateNextValue(CodeSequenceAllocation allocation);
}
