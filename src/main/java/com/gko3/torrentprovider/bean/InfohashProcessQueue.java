package com.gko3.torrentprovider.bean;

/**
 * infohash process queue
 * Created by hechaobin01 on 2015/7/7.
 */
public class InfohashProcessQueue extends  ProcessQueue {
    private static InfohashProcessQueue ourInstance = new InfohashProcessQueue();

    public static InfohashProcessQueue getInstance() {
        return ourInstance;
    }

    private InfohashProcessQueue() {
    }
}
