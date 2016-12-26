package com.gko3.torrentprovider.torrent;

import org.apache.log4j.Logger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.io.IOException;

import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.bean.HdfsFileSystemInfo;

/**
 * manager all hdfs connection, thus same hdfs only use one connection
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class HdfsConnectionManager {
    private static final Logger LOG = Logger.getLogger(HdfsConnectionManager.class);
    private static HdfsConnectionManager hdfsConnectionManager = new HdfsConnectionManager();

    /**
     * @return singleton of HdfsConnectionManager
     */
    public static HdfsConnectionManager getHdfsConnectionManager() {
        return hdfsConnectionManager;
    }

    // map for hdfsHost->FileSystem
    private ConcurrentHashMap<String, HdfsFileSystemInfo> map = new ConcurrentHashMap<String, HdfsFileSystemInfo>();

    // max keep time for one FileSystem not used in the map
    private long maxKeeptime;


    private HdfsConnectionManager() {
        maxKeeptime = TorrentProviderConfig.hdfsFileSystemKeepTime() * 1000;
        // add a new thread for clean overdue torrent
        Thread cleanThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        cleanOverdueFileSystem();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        LOG.warn("Thread has been interrupt:" + e);
                    }
                }
            }
        });
        cleanThread.start();
    }

    /**
     * get FileSystem
     *
     * @param uri   map key, hdfs Host and user password, format is
     *              user,pass@hdfs://test.hdfs.com:54310 
     * @return FileSystem   if param error, return null 
     * @throws IOException
     */
    public FileSystem getFileSystem(String uri) throws IOException {
        // check if host in map
        HdfsFileSystemInfo hdfsInfo = this.get(uri);
        if (hdfsInfo == null) {
            int lastAtPos = uri.lastIndexOf("@");
            if (lastAtPos == -1) {
                return null;
            }

            String userPassword = uri.substring(0, lastAtPos);
            String host = uri.substring(lastAtPos + 1);
            LOG.info("userpassword:" + userPassword + ", host:" + host);
            Configuration conf = new Configuration();
            conf.set("hadoop.job.ugi", userPassword);
            conf.set("fs.default.name", host);
            conf.setInt("ipc.ping.interval", 30);
            conf.setInt("ipc.client.connect.timeout", 1000);  // 1s
            conf.setInt("ipc.client.connect.max.retries", 1);

            FileSystem fs = FileSystem.get(conf);
            HdfsFileSystemInfo tmpInfo = new HdfsFileSystemInfo();
            tmpInfo.setFileSystem(fs);
            this.put(uri, tmpInfo);
            return fs;
        }

        return hdfsInfo.getFileSystem();
    }

    private HdfsFileSystemInfo get(String uri) {
        return map.get(uri);
    }

    private void put(String uri, HdfsFileSystemInfo hdfsInfo) {
        map.put(uri, hdfsInfo);
    }

    /**
     * @return keys set(all uris in memory) in the map
     */
    public Set<String> uriSet() {
        return map.keySet();
    }

    public long size() {
        return map.size();
    }

    public void closeAllFileSystem() {
        LOG.info("close all file system start, size is " + map.size());
        Set<String> uriSet = this.uriSet();
        for (String uri : uriSet) {
            HdfsFileSystemInfo hdfsInfo = map.get(uri);
            if (hdfsInfo == null) {
                continue;
            }
            try {
                hdfsInfo.getFileSystem().close();
                map.remove(uri);
            } catch (IOException e) {
                LOG.warn("some error happens in close uri:" + uri + ", " + e);
            }
        }

        LOG.info("close all file system end");
    }

    private void cleanOverdueFileSystem() {
        long currentTimestamp = System.currentTimeMillis();
        Set<String> uriSet = this.uriSet();
        for (String uri : uriSet) {
            checkAndRemove(currentTimestamp, uri);
        }
    }

    /**
     * map overdue key check and clean function
     *
     * @param currentTimestamp  currentTimestamp
     * @param uri               uri for check
     */
    private void checkAndRemove(long currentTimestamp, String uri) {
        HdfsFileSystemInfo hdfsInfo = map.get(uri);
        if (hdfsInfo == null) {
            return;
        }

        if (currentTimestamp - hdfsInfo.getLastAccessTimestamp() > maxKeeptime) {
            LOG.info("uri:" + uri + " has overder, close fileSystem");
            try {
                hdfsInfo.getFileSystem().close();
            } catch (IOException e) {
                LOG.error("uri:" + uri + " some error happend:" + e);
                return;
            }
            map.remove(uri);
        }
    }
}
