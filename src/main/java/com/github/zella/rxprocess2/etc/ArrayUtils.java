package com.github.zella.rxprocess2.etc;

public class ArrayUtils {

    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public static byte[] toPrimitive(final Byte[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }
}
