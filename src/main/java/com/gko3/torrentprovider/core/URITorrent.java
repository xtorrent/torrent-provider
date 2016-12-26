package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.torrent.BaseTorrentProcessor;
import com.gko3.torrentprovider.torrent.GKOTorrentProcessor;
import org.apache.log4j.Logger;

import com.gko3.torrentprovider.torrent.HdfsTorrentProcessor;
import com.gko3.torrentprovider.bean.URITorrentBaseInfo;

import com.gko3.torrentprovider.thrift.TorrentStatus;

import java.nio.ByteBuffer;

/**
 * URI torrent info, including torrent snappy code, torrent status .etc
 * 
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class URITorrent {
    private static final Logger LOG = Logger.getLogger(URITorrent.class);
    private String uri;
    private String lastModifyTime;
    private ByteBuffer torrentZipCode;
    private TorrentStatus torrentStatus;
    private long updateTimestamp;
    private String message;
    BaseTorrentProcessor processor = null;

    public static URITorrent getTorrentFromBaseInfo(URITorrentBaseInfo baseInfo) {
        URITorrent torrent = new URITorrent();
        torrent.setUri(baseInfo.getUri());
        torrent.setLastModifyTime(baseInfo.getLastModifyTime());
        torrent.setTorrentZipCode(baseInfo.getTorrentZipCode());
        torrent.setTorrentStatus(baseInfo.getTorrentStatus());
        torrent.setUpdateTimestamp(baseInfo.getUpdateTimestamp());
        torrent.setMessage(baseInfo.getMessage());
        return torrent;
    }

    public String getUri() {
        return uri;
    }

    public String getLastModifyTime() {
        return this.lastModifyTime;
    }

    public byte[] getTorrentZipCode() {
        return this.torrentZipCode == null ? null : this.torrentZipCode.array();
    }

    public TorrentStatus getTorrentStatus() {
        return this.torrentStatus;
    }

    public long getUpdateTimestamp() {
        return this.updateTimestamp;
    }

    public String getMessage() {
        return this.message;
    }

    public void setUri(String uri) {
        if (uri.startsWith("hdfs://")) {
            processor = new HdfsTorrentProcessor();
        } else if (uri.startsWith("gko3://")) {
            processor = new GKOTorrentProcessor();
        } else {
            LOG.error("unknown protocol for uri:" + uri);
            return;
        }
        this.uri = uri;
    }
    
    public void setLastModifyTime(String modifyTime) {
        this.lastModifyTime = modifyTime;
    }

    public void setTorrentZipCode(byte[] buffer) {
        if (buffer == null) {
            return;
        }

        if (this.torrentZipCode != null) {
            this.torrentZipCode.clear();
        }
        this.torrentZipCode = ByteBuffer.allocate(buffer.length);
        this.torrentZipCode.put(buffer, 0, buffer.length);
    }

    public void setTorrentStatus(TorrentStatus status) {
        this.torrentStatus = status;
    }

    public void setUpdateTimestamp(long timestamp) {
        this.updateTimestamp = timestamp;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public URITorrentBaseInfo getBaseInfo() {
        URITorrentBaseInfo baseInfo = new URITorrentBaseInfo();
        baseInfo.setUri(getUri());
        baseInfo.setTorrentZipCode(getTorrentZipCode());
        baseInfo.setLastModifyTime(getLastModifyTime());
        baseInfo.setTorrentStatus(getTorrentStatus());
        baseInfo.setUpdateTimestamp(getUpdateTimestamp());
        baseInfo.setMessage(getMessage());
        return baseInfo;
    }

    /**
     * generate torrent Zip code
     *
     * @param   uri
     * @return  HdfsStatus
     */
    public GeneralStatus generateTorrentCode(String uri) {
        if (processor == null) {
            setMessage("processor is null when generate torrentCode!");
            return GeneralStatus.STATUS_INVALID_PARAM;
        }

        GeneralStatus ret = processor.setBasicConfig(uri);
        if (ret != GeneralStatus.STATUS_OK) {
            setMessage(processor.getErrorMessage());
            LOG.error("set uri basic conf failed:" + this.message);
            return ret;
        }

        ret = processor.addDirectory();
        if (ret != GeneralStatus.STATUS_OK) {
            setMessage(processor.getErrorMessage());
            LOG.error("add directory failed:" + this.message);
            return ret;
        }

        String modifyTime = processor.directoryModifyTime();
        if (modifyTime.isEmpty()) {
            setMessage(processor.getErrorMessage());
            LOG.error("get dir modify time failed:" + this.message);
            return GeneralStatus.STATUS_ERROR;
        }

        setLastModifyTime(modifyTime);

        byte[] torrentData = processor.generateTorrent();
        if (torrentData == null || torrentData.length == 0) {
            setMessage(processor.getErrorMessage());
            LOG.error("generate torrent snappy code failed:" + this.message);
            return GeneralStatus.STATUS_ERROR;
        }

        setTorrentZipCode(torrentData);
        setMessage("OK");
        return GeneralStatus.STATUS_OK;
    }

    public GeneralStatus generateTorrentCode() {
        return this.generateTorrentCode(this.uri);
    }

    public  String getUriLastModifyTime() {
        if (processor == null) {
            LOG.error("processor is null when get uri last modify time!");
            return new String();
        }
        if (processor.setBasicConfig(getUri()) != GeneralStatus.STATUS_OK) {
            setMessage(processor.getErrorMessage());
            LOG.error("get dir modify time failed:" + this.message);
            return new String();
        }
        return processor.directoryModifyTime();
    }

    public void release() {
        if (processor != null) {
            processor.release();
        }
    }

    @Override
    public String toString() {
        String dump = "URI: " + this.getUri() + "\n";
        dump += "STATUS: "  + this.getTorrentStatus() + "\n";

        if (this.lastModifyTime != null) {
            dump += "LAST_MODIFY_TIME: " + Long.parseLong(this.getLastModifyTime()) / 1000 + "\n";
        } else {
            dump += "LAST_MODIFY_TIME: null\n";
        }
        dump += "UPDATE_TIME: " + this.getUpdateTimestamp() / 1000 + "\n";

        if (this.torrentZipCode != null) {
            dump += "TORRENT_CODE_LENGTH: " + this.getTorrentZipCode().length + "\n";
        }

        dump += "MESSAGE: " + this.getMessage();
        return dump;
    }
}

