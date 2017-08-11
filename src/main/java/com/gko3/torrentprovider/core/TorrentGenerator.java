package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.bean.FailedRetryInfo;
import com.gko3.torrentprovider.bean.FailedRetryQueue;
import com.gko3.torrentprovider.bean.ProcessKeeper;
import com.gko3.torrentprovider.bean.URIProcessQueue;
import com.gko3.torrentprovider.bean.URIStatistics;
import com.gko3.torrentprovider.bean.URITorrentMap;
import org.apache.log4j.Logger;

import com.gko3.torrentprovider.thrift.TorrentStatus;
import com.gko3.torrentprovider.common.ExitHandler;

/**
 * Torrent generator for one thread
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentGenerator implements Runnable {
    private static final Logger LOG = Logger.getLogger(TorrentGenerator.class);

    @Override
    public void run() {
        while (true) {
            try {
                URIProcessQueue queue = URIProcessQueue.getInstance();
                String uri = queue.take();
                // if torrent is not in cache, we need generate from hdfs, and this may take more time
                if (!tryGetFromCache(uri)) {
                    URIStatistics.getInstance().addURICacheProcess(false);
                    generate(uri);
                } else {
                    URIStatistics.getInstance().addURICacheProcess(true);
                }
                // because hadoop will modify signal handle, so we need set it again
                // is this necessary? may be not ...
                ExitHandler.setHandler();
            } catch (InterruptedException e) {
                LOG.warn("torrent generator warn:" + e);
            } catch (Exception e) {
                LOG.warn("some error:" + e);
            }
        }
    }

    /**
     * generate torrent from uri by reading hdfs, put result into URITorrentMap
     * <p/>
     * and modify ProcessKeeper status
     *
     * @param uri uri for torrent generation
     */
    private void generate(String uri) {
        LOG.info("start generate uri[" + uri + "]");
        URITorrent torrent = new URITorrent();
        LOG.debug("set uri for torrent");
        torrent.setUri(uri);
        LOG.debug("prepare to call torrent.generateTorrentCode()");
        GeneralStatus ret = torrent.generateTorrentCode();
        LOG.debug("finish call torrent.generateTorrentCode()");
        if (ret == GeneralStatus.STATUS_OK) {
            LOG.debug("call torrent.generateTorrentCode() with OK");
            torrent.setTorrentStatus(TorrentStatus.STATUS_OK);
            URIStatistics.getInstance().addHdfsProcess(true);
        } else if (ret != GeneralStatus.STATUS_ERROR) {
            LOG.debug("call torrent.generateTorrentCode() with FILE_NOT_EXIST");
            if (ret == GeneralStatus.STATUS_FILE_NOT_EXIST) {
                torrent.setTorrentStatus(TorrentStatus.STATUS_ERROR_FILE_NOT_EXIST);
            } else {
                torrent.setTorrentStatus(TorrentStatus.STATUS_ERROR);
            }
            FailedRetryQueue.getInstance().removeMapKey(uri);
            URIStatistics.getInstance().addHdfsProcess(false);
        } else {
            LOG.debug("call torrent.generateTorrentCode() with ERROR");
            torrent.setTorrentStatus(TorrentStatus.STATUS_ERROR);
            // put it into failed retry queue
            if (FailedRetryQueue.getInstance().put(uri, FailedRetryInfo.KEY_TYPE_URI)) {
                // not reach max retry times, continue
                torrent.release();
                return;
            }
            FailedRetryQueue.getInstance().removeMapKey(uri);
            URIStatistics.getInstance().addHdfsProcess(false);
        }
        torrent.setUpdateTimestamp(System.currentTimeMillis());

        ProcessKeeper.getInstance().removeKey(uri);
        URITorrentMap.getInstance().putTorrent(uri, torrent);
        LOG.info("end generate uri[" + uri + "], status=" + ret);
        torrent.release();
    }

    /**
     * try to get torrent from cache, and also check hdfs timestamp for modify
     *
     * @param uri uri for getting cache torrent
     * @return if uri torrent in cache exist and modify time has not change since last store, return true; else false
     */
    private boolean tryGetFromCache(String uri) {
        TorrentCache cache = TorrentCache.getTorrentCache();
        URITorrent torrent = cache.getTorrent(uri);
        if (torrent == null) {
            LOG.info("can not get torrent from cache");
            return false;
        }
        String latestModifyTime = torrent.getUriLastModifyTime();
        String getModifyTime = torrent.getLastModifyTime();
        if (!latestModifyTime.equals(getModifyTime)) {
            LOG.info("dir has been modified, last:" + getModifyTime + ", now:" + latestModifyTime);
            torrent.release();
            return false;
        }

        torrent.setUpdateTimestamp(System.currentTimeMillis());
        ProcessKeeper.getInstance().removeKey(uri);
        URITorrentMap.getInstance().putTorrent(uri, torrent);
        LOG.info("get torrent from cache success, uri[" + uri + "]");
        torrent.release();
        return true;
    }
}
