package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.core.TorrentInfo;
import com.gko3.torrentprovider.thrift.TorrentStatus;

/**
 * Infohash torrent Map info
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class InfohashTorrentMap {
    private static final Logger LOG = Logger.getLogger(InfohashTorrentMap.class);
    private static InfohashTorrentMap infohashTorrentMap = new InfohashTorrentMap();

    /**
     * @return singleton of InfohashTorrentMap
     */
    public static InfohashTorrentMap getInstance() {
        return infohashTorrentMap;
    }

    // map for infohash->torrent
    private ConcurrentHashMap<String, TorrentInfo> map = new ConcurrentHashMap<String, TorrentInfo>();

    // max keep time for one torrent in the map
    private long maxKeeptime;

    // max keep time for one status error torrent int the map
    private long maxKeeptimeForError;

    private InfohashTorrentMap() {
        maxKeeptime = TorrentProviderConfig.mapKeeptime() * 1000;
        maxKeeptimeForError = TorrentProviderConfig.mapKeeptimeForError() * 1000;
        // add a new thread for clean overdue torrent
        Thread cleanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleanOverdueTorrent();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        LOG.warn("infohash torrent clean thread has been interrupt:" + e);
                    }
                }
            }
        });
        cleanThread.start();
    }

    /**
     * put torrent into map by infohash
     *  @param infohash map key, infohash
     * @param torrent   map value, torrent
     */
    public void putTorrent(String infohash, TorrentInfo torrent) {
        map.put(infohash, torrent);
        LOG.info("put infohash[" + infohash + "], source:" + torrent.getSource());
    }

    /**
     * remove torrent from map
     *
     * @param infohash   torrent infohash
     */
    public void removeTorrent(String infohash) {
        map.remove(infohash);
        LOG.info("remove infohash[" + infohash + "]");
    }

    /**
     * get torrent info by infohash
     *
     * @param infohash   torrent infohash
     * @return TorrentInfo for this infohash, if not exist, return null
     */
    public TorrentInfo getTorrent(String infohash) {
        return map.get(infohash);
    }

    /**
     * @return keys set(all infohashes in memory) in the map
     */
    public Set<String> uriSet() {
        return map.keySet();
    }

    public long size() {
        return map.size();
    }

    private void cleanOverdueTorrent() {
        long currentTimestamp = System.currentTimeMillis();
        Set<String> uriSet = this.uriSet();
        for (String infohash : uriSet) {
            checkAndRemove(currentTimestamp, infohash);
        }
    }

    /**
     * map overdue key check and clean function
     *  @param currentTimestamp  currentTimestamp
     * @param infohash               infohash for check
     */
    private void checkAndRemove(long currentTimestamp, String infohash) {
        TorrentInfo torrent = this.getTorrent(infohash);
        if (torrent == null) {
            return;
        }

        if (torrent.getTorrentStatus() == TorrentStatus.STATUS_ERROR) {
            if ((currentTimestamp - torrent.getUpdateTime()) > maxKeeptimeForError) {
                LOG.info("torrent[" + infohash + "] is overdue, status is error, just remove it!");
                this.removeTorrent(infohash);
            }
        } else {
            if ((currentTimestamp - torrent.getUpdateTime()) > maxKeeptime) {
                LOG.info("torrent[" + infohash + "] is overdue, put it into cache and remove it from map!");
                this.removeTorrent(infohash);
            }
        }
    }
}
