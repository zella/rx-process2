package com.github.zella.rxprocess2;

import java.util.Arrays;
import java.util.Objects;

public class ProcessChunk {
    public final byte[] data;
    public final boolean isStdErr;

    public ProcessChunk(byte[] data, boolean isStdErr) {
        this.data = data;
        this.isStdErr = isStdErr;
    }

    @Override
    public String toString() {
        return new String(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessChunk that = (ProcessChunk) o;
        return isStdErr == that.isStdErr &&
                Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(isStdErr);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
