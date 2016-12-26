package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.thrift.TorrentStatus;

import java.nio.ByteBuffer;

/**
 * Created by hechaobin01 on 2015/7/6.
 */
public class InfohashTorrent {
    private String infohash;
    private String source;
    private ByteBuffer torrentCode;
    private TorrentStatus torrentStatus;
    private String message = "OK";
    private long createTime;
    private long requestCount;

    public byte[] getTorrentCode() {
        return this.torrentCode == null ? null : this.torrentCode.array();
    }

    public String getInfohash() {
        return this.infohash;
    }

    public String getSource() {
        return this.source;
    }

    public TorrentStatus getTorrentStatus() {
        return this.torrentStatus;
    }

    public String getMessage() {
        return this.message;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setInfohash(String infohash) {
        this.infohash = infohash;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setTorrentCode(byte[] buffer) {
        if (buffer == null) {
            return;
        }

        if (this.torrentCode != null) {
            this.torrentCode.clear();
        }
        this.torrentCode = ByteBuffer.allocate(buffer.length);
        this.torrentCode.put(buffer, 0, buffer.length);
    }

    public void setTorrentStatus(TorrentStatus torrentStatus) {
        this.torrentStatus = torrentStatus;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }
}
