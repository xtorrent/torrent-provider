package com.gko3.torrentprovider.leveldb;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.log4j.Logger;

import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.Range;

import com.gko3.torrentprovider.exception.LevelDBException;

import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;

/**
 * This class provide level db access tool,such as {@link #put(String, byte[])} and {@link #get(String)}.
 * Remember to {@link #open(int, int, String)} level db before use and {@link #close()} after used.
 * <p/>
 * All the exception is convert do LevelDBException and record by error LOG.
 * All the method is thread safe.
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class LevelDbManager {
    private static final Logger LOG = Logger.getLogger(LevelDbManager.class);

    private static LevelDbManager levelDbManager = new LevelDbManager();

    private LevelDbManager() {
    }

    public static LevelDbManager getLevelDbManager() {
        return levelDbManager;
    }

    private DB client;
    private Options options;

    /**
     * level db write buffer itemSizeCount
     */
    private int writeBufferSize;

    /**
     * level db max open file number,default is 1024
     */
    private int maxOpenFiles;

    /**
     * level db store file name
     */
    private String levelDbPath;

    /**
     * open connect to level db
     *
     * @param writeBufferSize write buffer itemSizeCount
     * @param maxOpenFiles    store file name
     * @param levelDbPath     store file name
     * @throws LevelDBException when connect fail and encounter {@link IOException}
     */
    public void open(int writeBufferSize, int maxOpenFiles, String levelDbPath) throws LevelDBException {
        LOG.info("open connect to level db begin");

        this.writeBufferSize = writeBufferSize;
        this.maxOpenFiles = maxOpenFiles;
        this.levelDbPath = levelDbPath;

        options = new Options();
        options.createIfMissing(true);
        options.writeBufferSize(writeBufferSize);
        options.maxOpenFiles(maxOpenFiles);
        options.compressionType(CompressionType.SNAPPY);

        try {
            client = factory.open(new File(levelDbPath), options);
            LOG.info("open connect to level db end");
        } catch (IOException e) {
            // if open level db fail,no need to repair level db,java jni call has no method to check level db status
            LOG.fatal("client open connect to level db fail!", e);
            throw new LevelDBException(LevelDBException.ErrorCode.OPEN_CONNECT_TO_LEVEL_DB_FAIL);
        }
    }

    /**
     * close connect to level db
     *
     * @throws LevelDBException when close fail and encounter {@link IOException}
     */
    public void close() throws LevelDBException {
        LOG.info("client close connect from level db begin");

        try {
            if (client != null) {
                client.close();
            }
            LOG.info("client close connect from level db end");
        } catch (IOException e) {
            LOG.error("client close connect from level db fail!", e);
            throw new LevelDBException(LevelDBException.ErrorCode.CLOSE_CONNECT_FROM_LEVEL_DB_FAIL);
        }
    }

    /**
     * put an entry to level db
     *
     * @param key   entry key
     * @param value entry value
     * @throws LevelDBException when write fail and encounter {@link org.iq80.leveldb.DBException}
     */
    public void put(String key, byte[] value) throws LevelDBException {
        try {
            client.put(bytes(key), value);
        } catch (org.iq80.leveldb.DBException e) {
            LOG.error("write to level db fail", e);
            throw new LevelDBException(LevelDBException.ErrorCode.WRITE_FAIL);
        }
    }

    /**
     * read an entry from level db
     *
     * @param key entry key
     * @return entry value
     * @throws LevelDBException when  read fail and encounter {@link org.iq80.leveldb.DBException}
     */
    public byte[] get(String key) throws LevelDBException {
        try {
            return client.get(bytes(key));
        } catch (org.iq80.leveldb.DBException e) {
            LOG.error("read from level db fail", e);
            throw new LevelDBException(LevelDBException.ErrorCode.READ_FAIL);
        }
    }

    /**
     * delete an entry from level db
     *
     * @param key entry key
     * @throws LevelDBException when write fail and encounter {@link org.iq80.leveldb.DBException}
     */
    public void delete(String key) throws LevelDBException {
        try {
            client.delete(bytes(key));
        } catch (org.iq80.leveldb.DBException e) {
            LOG.error("write to level db fail", e);
            throw new LevelDBException(LevelDBException.ErrorCode.WRITE_FAIL);
        }
    }

    /**
     * destroy all the data in level db
     *
     * @throws LevelDBException when call {@link #delete(String)}
     */
    public void destroy() throws LevelDBException {
        LOG.info("destroy level db begin");

        try {
            factory.destroy(new File(levelDbPath), options);
            LOG.info("destroy level db end");
        } catch (IOException e) {
            LOG.error("destroy level db fail", e);
            throw new LevelDBException(LevelDBException.ErrorCode.IO_EXCEPTION);
        }
    }

    /**
     * get the key set from level db
     *
     * @return key set in level db
     * @throws LevelDBException when encounter {@link IOException}
     */
    public Set<String> keySet() throws LevelDBException {
        int size = 0;
        Set<String> keySet = new HashSet();
        DBIterator iterator = client.iterator();
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            try {
                keySet.add(new String(iterator.peekNext().getKey(), "ISO-8859-1"));
            } catch (Exception e) {
                continue;
            }
        }
        try {
            iterator.close();
        } catch (IOException e) {
            LOG.error("close iterator from level db fail", e);
            throw new LevelDBException(LevelDBException.ErrorCode.IO_EXCEPTION);
        }

        return keySet;
    }

    /**
     * get file size of level db
     *
     * @return file size of level db
     * @throws LevelDBException
     */
    public double fileSizeMb() {
        byte[] firstKey = null;
        byte[] lastKey = null;

        DBIterator iterator = client.iterator();

        iterator.seekToFirst();
        Map.Entry<byte[], byte[]> firstItem = iterator.next();
        if (firstItem != null) {
            firstKey = firstItem.getKey();
        }

        iterator.seekToLast();
        Map.Entry<byte[], byte[]> lastItem = iterator.next();
        if (lastItem != null) {
            lastKey = lastItem.getKey();
        }

        if (firstKey == null || lastKey == null) {
            return 0;
        }

        Range range = new Range(firstKey, lastKey);
        long[] approximateSizes = client.getApproximateSizes(range);

        if (approximateSizes.length > 0) {
            double totalByte = (double) approximateSizes[0];
            return totalByte / (1024 * 1024);
        }

        return 0;
    }
}
