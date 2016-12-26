package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.bean.FailedRetryInfo;
import com.gko3.torrentprovider.bean.FailedRetryQueue;
import com.gko3.torrentprovider.bean.InfohashProcessQueue;
import com.gko3.torrentprovider.bean.InfohashTorrentMap;
import com.gko3.torrentprovider.bean.ProcessKeeper;
import com.gko3.torrentprovider.database.TorrentDB;
import com.gko3.torrentprovider.thrift.TorrentStatus;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 *  database worker for one thread
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class DatabaseWorker implements Runnable {
    private static final Logger LOG = Logger.getLogger(DatabaseWorker.class);

    @Override
    public void run() {
        while (true) {
            String infohash = new String();
            try {
                InfohashProcessQueue queue = InfohashProcessQueue.getInstance();
                infohash = queue.take();
                TorrentInfo torrent = TorrentDB.getInstance().getTorrentByInfohash(infohash);
                ProcessKeeper.getInstance().removeKey(infohash);
                InfohashTorrentMap.getInstance().putTorrent(infohash, torrent);
            } catch (SQLException e) {
                LOG.error("sql error:" + e);
                // failed retry
                if (!infohash.isEmpty()) {
                    if (FailedRetryQueue.getInstance().put(infohash, FailedRetryInfo.KEY_TYPE_INFOHASH)) {
                        LOG.info("infohash:" + infohash + " retry");
                    } else {
                        ProcessKeeper.getInstance().removeKey(infohash);
                        TorrentInfo info = new TorrentInfo();
                        info.setMessage("internal error...");
                        info.setTorrentStatus(TorrentStatus.STATUS_ERROR);
                        InfohashTorrentMap.getInstance().putTorrent(infohash, info);
                    }
                }
            } catch (InterruptedException e) {
                LOG.warn("database worker warn:" + e);
            } catch (Exception e) {
                LOG.warn("some error:" + e);
            }
        }
    }
}

