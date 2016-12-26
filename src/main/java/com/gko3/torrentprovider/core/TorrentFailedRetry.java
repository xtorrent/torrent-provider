package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.bean.FailedRetryInfo;
import com.gko3.torrentprovider.bean.FailedRetryQueue;
import com.gko3.torrentprovider.bean.InfohashProcessQueue;
import org.apache.log4j.Logger;

import com.gko3.torrentprovider.bean.URIProcessQueue;
import com.gko3.torrentprovider.common.TorrentProviderConfig;

/**
 * Torrent generator for one thread
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentFailedRetry implements Runnable {
    private static final Logger LOG = Logger.getLogger(TorrentFailedRetry.class);

    @Override
    public void run() {
        long failedRetryInterval = TorrentProviderConfig.failedRetryInterval() * 1000;
        FailedRetryQueue queue = FailedRetryQueue.getInstance();

        while (true) {
            try {
                long currentTime = System.currentTimeMillis();
                while (queue.peekAndCheckNeedRetry(currentTime, failedRetryInterval)) {
                    String key = queue.take();
                    int keyType = queue.getKeyType(key);

                    // take key from failed retry queue and put it into process queue
                    LOG.info("begin to retry key[" + key + "], type:" + keyType);
                    if (keyType == FailedRetryInfo.KEY_TYPE_URI) {
                        URIProcessQueue.getInstance().put(key);
                    } else if (keyType == FailedRetryInfo.KEY_TYPE_INFOHASH) {
                        InfohashProcessQueue.getInstance().put(key);
                    } else {
                        LOG.warn("key[" + key + "], type:" + keyType + " unknown!");
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.warn("torrent failed retry  warn:" + e);
            }
        }
    }

}
