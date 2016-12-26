package com.gko3.torrentprovider.exception;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * snappy compress and uncompress related exception
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class SnappyException extends Exception {

    private int errorCode;

    public SnappyException(Throwable e) {
        super(e);
        try {
            Field field = e.getClass().getDeclaredField("errorCode");
            field.setAccessible(true);
            errorCode = (Integer) field.get(e);
        } catch (Exception ex) {
            errorCode = 0;
        }
    }

    public SnappyException(ErrorCode errorCode, String errorMsg) {
        super(errorCode.toString() + errorMsg);
        this.errorCode = errorCode.getValue();
    }

    public SnappyException(ErrorCode errorCode) {
        super(errorCode.toString());
        this.errorCode = errorCode.getValue();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        SNAPPY_COMPRESS_FAIL(1),
        SNAPPY_UNCOMPRESS_FAIL(2);

        private static Map<Integer, ErrorCode> hash = new HashMap<Integer, ErrorCode>();
        private final int value;

        private ErrorCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static ErrorCode getEnumItem(int value) {
            if (hash.isEmpty()) {
                initEnumHash();
            }
            return hash.get(value);
        }

        private static void initEnumHash() {
            for (ErrorCode errorCode : ErrorCode.values()) {
                hash.put(errorCode.getValue(), errorCode);
            }
        }

        @Override
        public String toString() {
            String packageName = getClass().getPackage().getName();
            String className = getClass().getName();
            className = className.replace("$", ".");
            return "[" + className.substring(packageName.length() + 1, className.length()) + "." + name() + "] ";
        }
    }
}
