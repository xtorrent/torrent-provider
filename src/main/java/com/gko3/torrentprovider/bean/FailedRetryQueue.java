package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ConcurrentHashMap;

import com.gko3.torrentprovider.common.TorrentProviderConfig;

/**
 * Failed retry queue, including key and infohash failed
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class FailedRetryQueue {
    private static final Logger LOG = Logger.getLogger(FailedRetryQueue.class);
    private static FailedRetryQueue failedRetryQueue = new FailedRetryQueue();

    /**
     * @return singleton of FailedRetryQueue
     */
    public static FailedRetryQueue getInstance() {
        return failedRetryQueue;
    }

    private FailedRetryQueue() {
    }

    // inner blocking queue for torrent in processing
    public  BlockingQueue<String> queue = new LinkedBlockingDeque<String>(TorrentProviderConfig.processQueueSize());

    // map for key->retry times
    private ConcurrentHashMap<String, FailedRetryInfo> map = new ConcurrentHashMap<String, FailedRetryInfo>();

    /**
     * offer key
     *
     * @param key   key want to offer
     * @return true if success other false
     */
    public boolean offer(String key) {
        return queue.offer(key);
    }

    /**
     * remove map key if put failed
     *
     * @param key   key want to offer
     */
    public void removeMapKey(String key) {
        map.remove(key);
        LOG.info("remove retry map key[" + key + "]");
    }

    public int getKeyType(String key) {
        FailedRetryInfo info = map.get(key);
        if (info == null) {
            return FailedRetryInfo.KEY_TYPE_ERROR;
        }

        return info.getKeyType();
    }

    /**
     * put key into queue
     *
     * @param key   key want to put
     * @return true if success other false
     */
    public boolean put(String key, int keyType) {
        try {
            FailedRetryInfo info = map.get(key);
            if (info != null && info.getRetryTimes() >= TorrentProviderConfig.maxFailedRetryTime()) {
                LOG.info("key[" + key + "]reach max retry time, failed");
                return false;
            }

            if (info != null) {
                info.addRetryTimes();
            } else {
                info = new FailedRetryInfo();
                info.setKeyType(keyType);
            }

            map.put(key, info);
            queue.put(key);
            LOG.info("key[" + key + "] retry:" + info);
        } catch (InterruptedException e) {
            LOG.warn("put key[" + key + "] error:" + e);
        }
        return true;
    }

    /**
     * take key from queue
     *
     * @return the head key string of the queue
     * @throws InterruptedException
     */
    public String take() throws InterruptedException {
        String key = "";
        try {
            key = queue.take();
        } catch (InterruptedException e) {
            LOG.warn("take key error:" + e.getMessage());
            throw e;
        }
        return key;
    }

    /**
     * peek queue
     *
     * @return key
     */
    public String peek() {
        return queue.peek();
    }

    /**
     * peek queue and check if queue head can start retry
     *
     * @param currentTime    current timestamp, for checking 
     * @param retryInterval  retryInterval for each key, in ms unit
     * @return true if success else false
     */
    public boolean peekAndCheckNeedRetry(long currentTime, long retryInterval) {
        String key = queue.peek();
        if (key == null) {
            // if null, means not key, just return false for next retry
            return false;
        }
        FailedRetryInfo info = map.get(key);
        if (info == null) {
            // something wrong, just return true and start retry
            // thus should not be block in this key
            LOG.info("something wrong with key:" + key);
            return true;
        }

        LOG.debug("info is " + info + ", currentTime:" + currentTime + ", interval:" + retryInterval);
        if (currentTime - info.getUpdateTimestamp() > retryInterval) {
            return true;
        }

        return false;
    }

    /**
     * queue size
     *
     * @return queue size
     */
    public int size() {
        return queue.size();
    }
}
