package com.gko3.torrentprovider.bean;

/**
 * uri failed retry base info, include retryTimes and update timestamp
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class FailedRetryInfo {
    public static final int KEY_TYPE_URI = 0;
    public static final int KEY_TYPE_INFOHASH = 1;
    public static final int KEY_TYPE_ERROR = -1;

    private int retryTimes;
    private long updateTimestamp;
    private int keyType = KEY_TYPE_URI;

    public FailedRetryInfo() {
        this.retryTimes = 0;
        this.updateTimestamp = System.currentTimeMillis();
    }

    public long getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public int getRetryTimes() {
        return this.retryTimes;
    }

    public int getKeyType() {
        return keyType;
    }

    public void setKeyType(int keyType) {
        if (keyType == KEY_TYPE_URI || keyType == KEY_TYPE_INFOHASH) {
            this.keyType = keyType;
        }
    }

    public void addRetryTimes() {
        this.updateTimestamp = System.currentTimeMillis();
        this.retryTimes = retryTimes + 1;
    }

    public String toString() {
        return "retryTimes:" + this.retryTimes + ",updateTime:" + this.updateTimestamp + ", type:" + this.keyType;
    }
}
