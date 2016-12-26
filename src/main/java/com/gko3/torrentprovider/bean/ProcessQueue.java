package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.gko3.torrentprovider.common.TorrentProviderConfig;

/**
 * base class of process queue, put by main request thread and take by process threads
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class ProcessQueue {
    private static final Logger LOG = Logger.getLogger(ProcessQueue.class);

    // inner blocking queue for torrent in processing
    public  BlockingQueue<String> queue = new LinkedBlockingDeque<String>(TorrentProviderConfig.processQueueSize());

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
     * put key into queue
     *
     * @param key   key want to put
     */
    public void put(String key) {
        try {
            queue.put(key);
        } catch (InterruptedException e) {
            LOG.warn("put key[" + key + "] error:" + e);
        }
    }

    /**
     * take key from queue
     *
     * @return the head uri string of the queue
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
     * queue size
     *
     * @return queue size
     */
    public int size() {
        return queue.size();
    }
}
