package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * URI process status keeper, for checking if a special uri is being processing
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class ProcessKeeper {
    private static final Logger LOG = Logger.getLogger(ProcessKeeper.class);

    private static ProcessKeeper processKeeper = new ProcessKeeper();

    /**
     * @return singleton of ProcessKeeper
     */
    public static ProcessKeeper getInstance() {
        return processKeeper;
    }

    // HashSet for keeping in processing uris
    private HashSet uriSet = new HashSet();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private ProcessKeeper() {
    }

    /**
     * add uri to keeper, this is thread-safe
     *
     * @param key   key want to add
     */
    public void addKey(String key) {
        lock.writeLock().lock();
        uriSet.add(key);
        LOG.debug("process keeper add key:" + key);
        lock.writeLock().unlock();
    }

    /**
     * remove uri from keeper, this is thread-safe
     *
     * @param key   key want to delete
     */
    public void removeKey(String key) {
        lock.writeLock().lock();
        uriSet.remove(key);
        LOG.debug("process keeper remove key:" + key);
        lock.writeLock().unlock();
    }

    /**
     * check if special uri is in processing, this is thread-safe
     *
     * @param key   key want to check
     * @return ture if exist other false
     */
    public boolean isExists(String key) {
        boolean contains = false;
        lock.readLock().lock();
        contains = uriSet.contains(key);
        lock.readLock().unlock();
        return contains;
    }
}
