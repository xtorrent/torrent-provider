package com.gko3.torrentprovider.client;

import com.gko3.torrentprovider.thrift.GeneralResponse;
import com.gko3.torrentprovider.thrift.InfohashTorrent;
import com.gko3.torrentprovider.thrift.TorrentProviderService;
import com.gko3.torrentprovider.thrift.TorrentResponse;
import com.gko3.torrentprovider.thrift.TorrentStatus;
import org.apache.log4j.Logger;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * torrent provider server performance test
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class ServerPerformanceTest extends Thread {
    private static final Logger LOG = Logger.getLogger(ServerPerformanceTest.class);

    // connect timeout
    private static final int TIMEOUT = 30000;

    // number of test urls
    private static final int TEST_SOURCE_NUMBER = 9;

    private static final String SOURCE_STR = "gko3://testhost.com:/home/work/test";

    // good hdfs url
    private static final String HDFSURL0 =
            "hdfs://user:pass@test.hdfs.com:54310/test/hechaobin01";
    private static final String HDFSURL1 =
            "hdfs://user:pass@test.hdfs.com:54310/test/hechaobin01/noah-postman";
    private static final String HDFSURL2 =
            "hdfs://user:pass@test.hdfs.com:54310/test/hechaobin01/dest";

    // bad hdfs url
    private static final String HDFSURL3 =
            "hdfs://user:pass@test.hdfs.com:54310/badtest/hechaobin01";
    private static final String HDFSURL4 = "hdfs://noah:noahxxtest@ss:x";

    // good infohashes
    private static final String INFOHASH0 = "fcaea518a8099df0563c4d11524dd1304f62f111";
    private static final String INFOHASH1 = "b3395b8971ad4271c44426959ec2aa025da20112";

    // not exist infohashes
    private static final String INFOHASH2 = "b3395b8971ad4271c44426959ec2aa025da2000";

    // bad infohashes
    private static final String INFOHASH3 = "a2225b8971ad4271c44426959ec2aa025da21";

    private String serverHost;
    private int serverPort;
    private int testCount;

    private static int totalFaileCount = 0;
    private static int hdfsCount = 0;
    private static int hdfsFailedCount = 0;
    private static int infohashCount = 0;
    private static int infohashFailedCount = 0;

    ServerPerformanceTest(String serverHost, int serverPort, int testCount) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.testCount = testCount;
    }


    @Override
    public void run() {
        // System.out.println("thread start ...");
        String[] urls = new String[TEST_SOURCE_NUMBER];
        urls[0] = HDFSURL0;
        urls[1] = HDFSURL1;
        urls[2] = HDFSURL2;
        urls[3] = HDFSURL3;
        urls[4] = HDFSURL4;

        urls[5] = INFOHASH0;
        urls[6] = INFOHASH1;
        urls[7] = INFOHASH2;
        urls[8] = INFOHASH3;

        for (int i = 0; i < testCount; ++i) {
            try {
                int index = (int) Math.round(Math.random() * (TEST_SOURCE_NUMBER - 1));
                TSocket socket = new TSocket(serverHost, serverPort, TIMEOUT);
                TFramedTransport transport = new TFramedTransport(socket);
                TProtocol protocol = new TBinaryProtocol(transport);
                TorrentProviderService.Client client = new TorrentProviderService.Client(protocol);
                transport.open();

                if (index <= 4) {
                    hdfsCount++;
                    String hdfsUrl = urls[index];
                    TorrentResponse response = client.getTorrentZipCode(hdfsUrl);
                    if (response.getTorrentStatus() == TorrentStatus.STATUS_PROCESS) {
                        LOG.debug("success process index[" + index + "]");
                        continue;
                    }

                    if (index == 3 || index == 4) {
                        if (response.getTorrentStatus() != TorrentStatus.STATUS_ERROR) {
                            LOG.warn("bad case:some in error in index["
                                    + index + "], status is " + response.getTorrentStatus());
                            hdfsFailedCount++;
                            continue;
                        }
                    } else if (response.getTorrentStatus() != TorrentStatus.STATUS_OK) {
                        LOG.warn("good case:some in error in index["
                                + index + "], status is " + response.getTorrentStatus());
                        hdfsFailedCount++;
                        continue;
                    }
                    LOG.debug("success process index[" + index + "]");
                } else {
                    infohashCount++;
                    InfohashTorrent info = client.getInfohashTorrent(urls[index]);
                    if (info.getTorrentStatus() == TorrentStatus.STATUS_PROCESS) {
                        continue;
                    }
                    if (index == 7 || index == 8) {
                        if (info.getTorrentStatus() != TorrentStatus.STATUS_ERROR) {
                            infohashFailedCount++;
                        }
                    } else if (info.getTorrentStatus() != TorrentStatus.STATUS_OK) {
                        infohashFailedCount++;
                    }
                }
                transport.close();

            } catch (TException e) {
                LOG.fatal("client error:" + e);
                totalFaileCount++;
                // break;
            }

        }  // for
        // System.out.println("thread exit ...");
    }  // run

    public static boolean setUpload(String serverHost, int serverPort) {
        try {
            int index = (int) Math.round(Math.random() * (TEST_SOURCE_NUMBER - 1));
            TSocket socket = new TSocket(serverHost, serverPort, TIMEOUT);
            TFramedTransport transport = new TFramedTransport(socket);
            TProtocol protocol = new TBinaryProtocol(transport);
            TorrentProviderService.Client client = new TorrentProviderService.Client(protocol);
            transport.open();
            InfohashTorrent info = new InfohashTorrent();
            info.setInfohash(INFOHASH0);
            info.setTorrentZipCode(new byte[] {'a', 'b', 'c'});
            info.setSource(SOURCE_STR);
            info.setTorrentStatus(TorrentStatus.STATUS_OK);
            GeneralResponse response = client.uploadTorrent(info);
            if (response.getRetCode() != 0) {
                LOG.error("upload torrent for infohash0 failed:" + response.getMessage());
                return false;
            }

            info.setInfohash(INFOHASH1);
            response = client.uploadTorrent(info);
            if (response.getRetCode() != 0) {
                LOG.error("upload torrent for infohash1 failed:" + response.getMessage());
                return false;
            }
            transport.close();
        } catch (Exception e) {
            LOG.error("upload fatal:" + e);
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Wrong parameter number\r\n\r\nUse:\r\n"
                               + "ServerPerformanceTest <threadNumber> <testCount> <serverHost> <serverPort>");
            System.exit(0);
        }

        int threadNumber = Integer.parseInt(args[0]);
        int testCount = Integer.parseInt(args[1]);
        String serverHost = new String(args[2]);
        int serverPort = Integer.parseInt(args[3]);
        if (!setUpload(serverHost, serverPort)) {
            System.out.println("upload failed!");
            System.exit(1);
        }

        System.out.println("threadNumber is: " + threadNumber + ", testCount is: " + testCount);
        long startTime = System.currentTimeMillis();

        ArrayList<Thread> threadList = new ArrayList();
        System.out.println("all threads begin to start ...");
        for (int i = 0; i < threadNumber; ++i) {
            Thread run = new ServerPerformanceTest(serverHost, serverPort, testCount);
            Thread tThread = new Thread(run);
            threadList.add(tThread);
            tThread.start();
        }

        Iterator<Thread> it = threadList.iterator();
        while (it.hasNext()) {
            try {
                it.next().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("all threads end ...");
        long endTime = System.currentTimeMillis();

        System.out.println("start is:" + startTime + ", end is: " + endTime
                + ", diff is: " + (endTime - startTime) + "(ms)");
        System.out.println("qps is: " + (threadNumber * testCount) / ((double) (endTime - startTime) / 1000));
        System.out.println("testTotal:" + threadNumber * testCount + ", failed total:" + totalFaileCount);
        System.out.println("hdfsCount:" + hdfsCount + ", hdfsFailed:" + hdfsFailedCount);
        System.out.println("infohashCount:" + infohashCount + ", infohashFailed:" + infohashFailedCount);

        System.exit(0);
    }

}
