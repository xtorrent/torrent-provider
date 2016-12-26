package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.core.URITorrent;
import com.gko3.torrentprovider.core.TorrentCache;
import com.gko3.torrentprovider.thrift.TorrentStatus;

/**
 * URI torrent Map info
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class URITorrentMap {
    private static final Logger LOG = Logger.getLogger(URITorrentMap.class);
    private static URITorrentMap uriTorrentMap = new URITorrentMap();

    /**
     * @return singleton of URITorrentMap
     */
    public static URITorrentMap getInstance() {
        return uriTorrentMap;
    }

    // map for uri->torrent
    private ConcurrentHashMap<String, URITorrent> map = new ConcurrentHashMap<String, URITorrent>();

    // max keep time for one torrent in the map
    private long maxKeeptime;

    // max keep time for one status error torrent int the map
    private long maxKeeptimeForError;

    private URITorrentMap() {
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
                        LOG.warn("uri torrent clean thread has been interrupt:" + e);
                    }
                }
            }
        });
        cleanThread.start();
    }

    /**
     * put torrent into map by uri
     *
     * @param uri       map key, uri
     * @param torrent   map value, torrent 
     */
    public void putTorrent(String uri, URITorrent torrent) {
        map.put(uri, torrent);
        LOG.info("put uri[" + uri + "], lastModifyTime:" + torrent.getLastModifyTime());
    }

    /**
     * remove torrent from map 
     *
     * @param uri   torrent uri
     */
    public void removeTorrent(String uri) {
        map.remove(uri);
        LOG.info("remove uri[" + uri + "]");
    }

    /**
     * get torrent info by uri
     *
     * @param uri   torrent uri
     * @return URITorrent for this uri, if not exist, return null
     */
    public URITorrent getTorrent(String uri) {
        return map.get(uri);
    }

    /**
     * @return keys set(all uris in memory) in the map
     */
    public Set<String> uriSet() {
        return map.keySet();
    }

    public void putAllTorrentIntoCache() {
        LOG.info("put all hdfsUris into cache success start, size is " + map.size());
        for (Map.Entry<String, URITorrent> e : map.entrySet()) {
            TorrentCache.getTorrentCache().putTorrent(e.getValue());
        }
        LOG.info("put all hdfsUris into cache success end ...");
    }

    public long size() {
        return map.size();
    }

    private void cleanOverdueTorrent() {
        long currentTimestamp = System.currentTimeMillis();
        Set<String> uriSet = this.uriSet();
        for (String uri : uriSet) {
            checkAndRemove(currentTimestamp, uri);
        }
    }

    /**
     * map overdue key check and clean function
     *
     * @param currentTimestamp  currentTimestamp
     * @param uri               uri for check
     */
    private void checkAndRemove(long currentTimestamp, String uri) {
        URITorrent torrent = this.getTorrent(uri);
        if (torrent == null) {
            return;
        }

        if (torrent.getTorrentStatus() == TorrentStatus.STATUS_ERROR) {
            if ((currentTimestamp - torrent.getUpdateTimestamp()) > maxKeeptimeForError) {
                LOG.info("torrent[" + uri + "] is overdue, status is error, just remove it!");
                this.removeTorrent(uri);
            }
        } else {
            if ((currentTimestamp - torrent.getUpdateTimestamp()) > maxKeeptime) {
                LOG.info("torrent[" + uri + "] is overdue, put it into cache and remove it from map!");
                TorrentCache.getTorrentCache().putTorrent(torrent);
                this.removeTorrent(uri);
            }
        }
    }
}
