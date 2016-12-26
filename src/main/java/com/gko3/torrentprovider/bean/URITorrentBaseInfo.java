package com.gko3.torrentprovider.bean;

import com.gko3.torrentprovider.thrift.TorrentStatus;

import java.io.Serializable;

/**
 * URI torrent base info without operation, including torrent zip code, torrent status .etc
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class URITorrentBaseInfo implements Serializable {
    private String uri;
    private String lastModifyTime;
    private long updateTimestamp;
    private byte[] torrentZipCode;
    private TorrentStatus torrentStatus;
    private String message;

    public String getUri() {
        return uri;
    }

    public String getLastModifyTime() {
        return this.lastModifyTime;
    }

    public long getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public byte[] getTorrentZipCode() {
        return this.torrentZipCode;
    }

    public TorrentStatus getTorrentStatus() {
        return this.torrentStatus;
    }

    public String getMessage() {
        return this.message;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public void setLastModifyTime(String modifyTime) {
        this.lastModifyTime = modifyTime;
    }

    public void setUpdateTimestamp(long updateTime) {
        this.updateTimestamp = updateTime;
    }

    public void setTorrentZipCode(byte[] buffer) {
        this.torrentZipCode = buffer;
    }

    public void setTorrentStatus(TorrentStatus status) {
        this.torrentStatus = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

