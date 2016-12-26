package com.gko3.torrentprovider.bean;

/**
 * URI process queue, put by main request thread and take by hdfs torrent process thread
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class URIProcessQueue extends  ProcessQueue {
    private static URIProcessQueue uriProcessQueue = new URIProcessQueue();

    /**
     * @return singleton of URIProcessQueue
     */
    public static URIProcessQueue getInstance() {
        return uriProcessQueue;
    }

    private URIProcessQueue() {
    }
}

