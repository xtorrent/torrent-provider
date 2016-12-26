package com.gko3.torrentprovider.common;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

/**
 * torrent provider config utils
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentProviderConfig {
    private static final Logger LOG = Logger.getLogger(TorrentProviderConfig.class);
    private static Properties properties = new Properties();

    static {
        init();
    }

    /**
     * init properties file
     */
    private static void init() {
        try {
            properties.load(TorrentProviderConfig.class.getClassLoader()
                    .getResourceAsStream("torrentProvider.properties"));
        } catch (IOException e) {
            LOG.fatal("load properties file failed:" + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * get property by key
     *
     * @param key   key for get
     * @return string value of this key
     */
    private static String getProperty(String key) {
        String ret = "";
        try {
            ret = properties.getProperty(key).trim();
        } catch (NullPointerException e) {
            LOG.fatal("lack " + key + " in the properties file" );
            System.exit(1);
        }
        return ret;
    }
    
    /**
     * @return server port of torrent-provider
     */
    public static int serverPort() {
        return Integer.parseInt(getProperty("server_port"));
    }

    /**
     * @return process queue size for hdfs torrent generator
     */
    public static int processQueueSize() {
        return Integer.parseInt(getProperty("process_queue_size"));
    }

    /**
     * @return default piece size, this value may be user config in the feature
     */
    public static int defaultPieceSize() {
        return Integer.parseInt(getProperty("default_piece_size"));
    }

    /**
     * @return thread number of hdfs torrent generators
     */
    public static int generatorNumber() {
        return Integer.parseInt(getProperty("generator_number"));
    }

    /**
     * @return thread number of database worker
     */
    public static int databaseWorkerNumber() {
        return Integer.parseInt(getProperty("database_worker_number"));
    }
    /**
     * @return URITorrentMap overdute time for normal torrent(seconds)
     */
    public static int mapKeeptime() {
        return Integer.parseInt(getProperty("map_keeptime"));
    }

    /**
     * @return URITorrentMap overdute time for error torrent (seconds)
     */
    public static int mapKeeptimeForError() {
        return Integer.parseInt(getProperty("map_keeptime_for_error"));
    }

    /**
     * @return leveldb overdute time(days)
     */
    public static int levelDbKeeptime() {
        return Integer.parseInt(getProperty("leveldb_keeptime"));
    }

    /**
     * @return number of rpc worker threads
     */
    public static int rpcWorkerNumber() {
        return Integer.parseInt(getProperty("rpc_worker_number"));
    }

    /**
     * @return number of rpc selectors
     */
    public static int rpcSelectorNumber() {
        return Integer.parseInt(getProperty("rpc_selector_number"));
    }

    /**
     * @return leveldb cache data path
     */
    public static String levelDbPath() {
        return getProperty("leveldb_path");
    }

    /**
     * @return leveldb write buffer size
     */
    public static int levelDbWriteBufferSize() {
        return Integer.parseInt(getProperty("leveldb_write_buffer_size"));
    }

    /**
     * @return leveldb max open files
     */
    public static int levelDbMaxOpenFiles() {
        return Integer.parseInt(getProperty("leveldb_max_open_files"));
    }

    /**
     * @return http port
     */
    public static int httpPort() {
        return Integer.parseInt(getProperty("http_port"));
    }

    /**
     * @return statistics keep time
     */
    public static int statisticsKeepTime() {
        return Integer.parseInt(getProperty("statistics_keep_time"));
    }

    /**
     * @return max falied time
     */
    public static int maxFailedRetryTime() {
        return Integer.parseInt(getProperty("max_failed_retry_time"));
    }

    /**
     * @return failed retry interval
     */
    public static int failedRetryInterval() {
        return Integer.parseInt(getProperty("failed_retry_interval"));
    }

    /**
     * @return hdfs fileSystem keep time
     */
    public static int hdfsFileSystemKeepTime() {
        return Integer.parseInt(getProperty("hdfs_file_system_keep_time"));
    }

    /**
     * @return database jdbc string
     */
    public static String databaseJdbc() {
        return getProperty("database_jdbc");
    }

    /**
     * @return database user
     */
    public static String databaseUser() {
        return getProperty("database_user");
    }

    /**
     * @return database passwd
     */
    public static String databasePasswd() {
        return getProperty("database_passwd");
    }

    /**
     * @return max infohash torrent keep time
     */
    public static int maxInfohashTorrentKeepTime() {
        return Integer.parseInt(getProperty("max_infohash_torrent_keep_time"));
    }

    /**
     * @return initial_pool_size
     */
    public static int initialPoolSize() {
        return Integer.parseInt(getProperty("initial_pool_size"));
    }

    /**
     * @return min_pool_size 
     */
    public static int minPoolSize() {
        return Integer.parseInt(getProperty("min_pool_size"));
    }

    /**
     * @return max_pool_size 
     */
    public static int maxPoolSize() {
        return Integer.parseInt(getProperty("max_pool_size"));
    }

    /**
     * @return acquire_increment 
     */
    public static int acquireIncrement() {
        return Integer.parseInt(getProperty("acquire_increment"));
    }

    /**
     * @return max_idle_time 
     */
    public static int maxIdleTime() {
        return Integer.parseInt(getProperty("max_idle_time"));
    }

    /**
     * @return checkout_timeout
     */
    public static int checkoutTimeout() {
        return Integer.parseInt(getProperty("checkout_timeout"));
    }

    /**
     * @return idle_connect_test_period
     */
    public static int idleConnectionTestPeriod() {
        return Integer.parseInt(getProperty("idle_connect_test_period"));
    }

    /**
     * @return uri_agent_default_port
     */
    public static int uriAgentDefaultPort() {
        return Integer.parseInt(getProperty("uri_agent_default_port"));
    }
}

