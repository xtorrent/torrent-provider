package com.gko3.torrentprovider.core;

import org.apache.log4j.Logger;

import com.gko3.torrentprovider.bean.URITorrentBaseInfo;
import com.gko3.torrentprovider.leveldb.LevelDbManager;
import com.gko3.torrentprovider.common.SerializeUtil;
import com.gko3.torrentprovider.common.TorrentProviderConfig;

import java.util.Set;

/**
 * Torrent cache manager
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentCache {
    private static final Logger LOG = Logger.getLogger(TorrentCache.class);

    private static TorrentCache torrentCache = new TorrentCache();

    // max keep time for leveldb item(days)
    private long maxKeeptime;

    private TorrentCache() {
        // in day unit
        maxKeeptime = TorrentProviderConfig.levelDbKeeptime() * 1000 * 86400;

        // add a new thread for clean overdue leveldb torrent
        Thread cleanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleanOverdueCache();
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        LOG.warn("Thread has been interrupt:" + e);
                    }
                }
            }
        });
        cleanThread.start();
    }

    /**
     * @return singleton of TorrentCache
     */
    public static TorrentCache getTorrentCache() {
        return torrentCache;
    }

    /**
     * get torrent from cache by uri
     *
     * @param uri   cache key
     * @return torrent in cache for this uri, if not exist, return null
     */
    public URITorrent getTorrent(String uri) {
        LOG.debug("try to get torrent from level db uri[" + uri + "]");
        try {
            byte[] bytes = LevelDbManager.getLevelDbManager().get(uri);
            if (bytes == null) {
                return null;
            }
            URITorrent torrent =
                URITorrent.getTorrentFromBaseInfo((URITorrentBaseInfo) SerializeUtil.unserialize(bytes));
            if (torrent == null) {
                LOG.error("serialize from bytes to URITorrent failed, uri:" + uri);
                return null;
            }
            return torrent;
        } catch (Exception e) {
            LOG.error("uri [" + uri + "] get torrent from leveldb exception:" + e);
            return null;
        }
    }

    /**
     * put torrent into cache, only push URITorrentBaseInfo
     *
     * @param torrent   URItorrent info
     */
    public void putTorrent(URITorrent torrent) {
        URITorrentBaseInfo baseInfo = torrent.getBaseInfo();
        try {
            byte[] existBytes = LevelDbManager.getLevelDbManager().get(torrent.getUri());
            if (existBytes != null) {
                LOG.debug("uri:" + torrent.getUri() + " has already in leveldb, delete it and update!");
                LevelDbManager.getLevelDbManager().delete(torrent.getUri());
            }
            byte[] bytes = SerializeUtil.serialize(baseInfo);
            if (bytes == null) {
                LOG.error("serialize from baseInfo failed, uri:" + torrent.getUri());
                return;
            }
            LevelDbManager.getLevelDbManager().put(torrent.getUri(), bytes);
            LOG.debug("put torrent uri:" + torrent.getUri() + " into leveldb, bytes length is " + bytes.length);
        } catch (Exception e) {
            LOG.error("put torrent to leveldb exception:" + e);
        }
    }

    /**
     * delete torrent from cache
     *
     * @param uri  cache key for delete 
     */
    public void deleteTorrent(String uri) {
        LOG.info("delete uri:" + uri + " from cache!");
        try {
            LevelDbManager.getLevelDbManager().delete(uri);
        } catch (Exception e) {
            LOG.error("delete uri:" + uri + " exception:" + e);
        }
    }

    private void cleanOverdueCache() {
        long currentTimestamp = System.currentTimeMillis();
        try {
            Set<String> uriSet = LevelDbManager.getLevelDbManager().keySet();
            for (String uri : uriSet) {
                checkAndRemove(currentTimestamp, uri);
            }
        } catch (Exception e) {
            LOG.info("cleanOverdueCache exception:" + e);
        }
    }

    /**
     * check and clean overdue cache item
     */
    private void checkAndRemove(long currentTimestamp, String uri) {
        URITorrent torrent = this.getTorrent(uri);
        if (torrent == null) {
            return;
        }

        if ((currentTimestamp - torrent.getUpdateTimestamp()) > maxKeeptime) {
            LOG.info("torrent[" + uri + "] is overdue, currentTime=" + currentTimestamp + ", updateTime="
                    + torrent.getUpdateTimestamp() + ", remove it from leveldb!");
            deleteTorrent(uri);
        }
    }
}
