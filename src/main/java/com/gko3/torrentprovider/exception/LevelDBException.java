package com.gko3.torrentprovider.exception;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * level db related exception
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class LevelDBException extends Exception {

    private int errorCode;

    public LevelDBException(Throwable e) {
        super(e);
        try {
            Field field = e.getClass().getDeclaredField("errorCode");
            field.setAccessible(true);
            errorCode = (Integer) field.get(e);
        } catch (Exception ex) {
            errorCode = 0;
        }
    }

    public LevelDBException(ErrorCode errorCode, String errorMsg) {
        super(errorCode.toString() + errorMsg);
        this.errorCode = errorCode.getValue();
    }

    public LevelDBException(ErrorCode errorCode) {
        super(errorCode.toString());
        this.errorCode = errorCode.getValue();
    }

    public int getErrorCode() {
        return errorCode;
    }

    public enum ErrorCode {
        OPEN_CONNECT_TO_LEVEL_DB_FAIL(1),
        CLOSE_CONNECT_FROM_LEVEL_DB_FAIL(2),
        READ_FAIL(3),
        WRITE_FAIL(4),
        IO_EXCEPTION(5);

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
