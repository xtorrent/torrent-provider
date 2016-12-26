package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.thrift.InfohashTorrent;
import com.gko3.torrentprovider.thrift.TorrentStatus;

import java.nio.ByteBuffer;

/**
 * Created by hechaobin01 on 2015/7/6.
 */
public class TorrentInfo {
    private long id;
    private String infohash;
    private String source;
    private ByteBuffer torrentCode;
    private TorrentStatus torrentStatus = TorrentStatus.STATUS_UNKNOWN;
    private String message = "OK";
    private long createTime = 0;
    private long requestCount = 0;
    private long updateTime = 0;

    public TorrentInfo() {
        updateTime = System.currentTimeMillis();
    }

    public TorrentInfo(InfohashTorrent info) {
        infohash = info.getInfohash();
        source = info.getSource();
        torrentCode = ByteBuffer.allocate(info.getTorrentZipCode().length);
        torrentCode.put(info.getTorrentZipCode(), 0, info.getTorrentZipCode().length);
        torrentStatus = info.getTorrentStatus();
        message = info.getMessage();
        updateTime = System.currentTimeMillis();
    }

    public InfohashTorrent generateInfohashTorrent() {
        InfohashTorrent info = new InfohashTorrent();
        info.setTorrentStatus(torrentStatus);
        info.setSource(source);

        if (torrentCode != null) {
            info.setTorrentZipCode(torrentCode.array());
        }

        info.setMessage(message);
        info.setInfohash(infohash);

        return info;
    }

    public long getId() {
        return this.id;
    }

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
        return this.createTime;
    }

    public long getRequestCount() {
        return this.requestCount;
    }

    public long getUpdateTime() {
        return this.updateTime;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setInfohash(String infohash) {
        if (!infohash.isEmpty() && infohash.length() == 40) {
            this.infohash = infohash;
        }
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

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
