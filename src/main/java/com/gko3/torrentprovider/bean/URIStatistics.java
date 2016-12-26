package com.gko3.torrentprovider.bean;

import org.apache.log4j.Logger;

import com.gko3.torrentprovider.common.TorrentProviderConfig;

/**
 * URI request statistics, for monitor
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class URIStatistics {
    private static final Logger LOG = Logger.getLogger(URIStatistics.class);

    private static URIStatistics uriStatistics = new URIStatistics();

    // uri statistics
    private long totalURIRequest = 0;
    private long totalURISuccessRequest = 0;

    private long totalURIProcess = 0;
    private long totalSuccessURIProcess = 0;

    private long totalURICacheProcess = 0;
    private long totalURISuccessCacheProcess = 0;

    // infohash statistics
    private long totalInfohashRequest = 0;
    private long totalSuccessInfohashReqeust = 0;

    private long totalUploadRequest = 0;
    private long totalSuccessUploadRequest = 0;

    private long startTimestamp;
    private long statisticsKeepTime;

    /**
     * @return singleton of URIStatistics
     */
    public static URIStatistics getInstance() {
        return uriStatistics;
    }

    private URIStatistics() {
        this.startTimestamp = System.currentTimeMillis();
        statisticsKeepTime = TorrentProviderConfig.statisticsKeepTime() * 1000;

        // add a new thread to clean statistics
        Thread updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        long currentTimestamp = System.currentTimeMillis();
                        if ((currentTimestamp - startTimestamp) > statisticsKeepTime) {
                            cleanStatisticsCounter();
                            startTimestamp = currentTimestamp;
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        LOG.warn("Thread has been interrupt:" + e);
                    }
                }
            }
        });
        updateThread.start();
    }

    synchronized void cleanStatisticsCounter() {
        this.totalURIRequest = 0;
        this.totalURISuccessRequest = 0;
        this.totalURIProcess = 0;
        this.totalSuccessURIProcess = 0;
        this.totalURICacheProcess = 0;
        this.totalURISuccessCacheProcess = 0;

        this.totalInfohashRequest = 0;
        this.totalSuccessInfohashReqeust = 0;
        this.totalUploadRequest = 0;
        this.totalSuccessUploadRequest = 0;
    }

    public synchronized long getTotalURIRequest() {
        return this.totalURIRequest;
    }

    public synchronized long getTotalURISuccessRequest() {
        return this.totalURISuccessRequest;
    }

    public synchronized long getTotalURIProcess() {
        return this.totalURIProcess;
    }

    public synchronized long getTotalSuccessURIProcess() {
        return this.totalSuccessURIProcess;
    }

    public synchronized long getTotalURICacheProcess() {
        return this.totalURICacheProcess;
    }

    public synchronized long getTotalURISuccessCacheProcess() {
        return this.totalURISuccessCacheProcess;
    }

    public synchronized long getTotalFailedRequest() {
        return this.totalURIRequest - this.totalURISuccessRequest;
    }

    public synchronized long getStartTimestamp() {
        return this.startTimestamp;
    }

    public synchronized long getTotalInfohashRequest() {
        return totalInfohashRequest;
    }

    public synchronized long getTotalUploadRequest() {
        return totalUploadRequest;
    }

    public synchronized long getTotalSuccessInfohashReqeust() {
        return totalSuccessInfohashReqeust;
    }

    public synchronized long getTotalSuccessUploadRequest() {
        return totalSuccessUploadRequest;
    }

    public synchronized void addURIRequest(boolean isSuccess) {
        this.totalURIRequest++;
        if (isSuccess) {
            this.totalURISuccessRequest++;
        }
    }

    public synchronized void addHdfsProcess(boolean isSuccess) {
        this.totalURIProcess++;
        if (isSuccess) {
            this.totalSuccessURIProcess++;
        }
    }

    public synchronized void addURICacheProcess(boolean isSuccess) {
        this.totalURICacheProcess++;
        if (isSuccess) {
            this.totalURISuccessCacheProcess++;
        }
    }

    public synchronized  void addInfohashRequest(boolean isSuccess) {
        this.totalInfohashRequest++;
        if (isSuccess) {
            this.totalSuccessInfohashReqeust++;
        }
    }

    public synchronized void addUploadRequest(boolean isSuccess) {
        this.totalUploadRequest++;
        if (isSuccess) {
            this.totalSuccessUploadRequest++;
        }
    }
}

