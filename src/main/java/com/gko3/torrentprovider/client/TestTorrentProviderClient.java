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

import java.io.FileOutputStream;

import com.gko3.torrentprovider.common.SnappyTool;

/**
 *
 * torrent provider rpc client for test 
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TestTorrentProviderClient {
    private static final Logger LOG = Logger.getLogger(TestTorrentProviderClient.class);

    // connect timeout
    private static final int TIMEOUT = 30000;

    // max retry time
    private static final int MAXRETRYTIME = 5;

    public static void main(String[] args) {
        if (args.length < 4) {
            LOG.error("Wrong parameter number\r\n\r\nUse:\r\n"
                + "TestTorrentProviderClient <HdfsUri> <serverHost> <serverPort> <infohash> [outputFile]");
            System.exit(0);
        }

        String hdfsUrl = new String(args[0]);
        String serverHost = new String(args[1]);
        int serverPort = Integer.parseInt(args[2]);
        String infohash = new String(args[3]);
        try {
            TSocket socket = new TSocket(serverHost, serverPort, TIMEOUT);
            TFramedTransport transport = new TFramedTransport(socket); 
            TProtocol protocol = new TBinaryProtocol(transport);
            TorrentProviderService.Client client = new TorrentProviderService.Client(protocol);

            transport.open();

            int counter = 0;
            boolean isSuccess = true;

            // test getTorrentZipCode
            TorrentResponse torrentResponse = client.getTorrentZipCode(hdfsUrl);
            while (torrentResponse.getTorrentStatus() == TorrentStatus.STATUS_PROCESS) {
                ++counter;
                if (counter >= MAXRETRYTIME) {
                    isSuccess = false;
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    break;
                }
                torrentResponse = client.getTorrentZipCode(hdfsUrl);
            }
            if (!isSuccess) {
                LOG.error("getTorrentZipCode error, counter:" + counter);
                System.exit(1);
            }

            LOG.info("torrentResponse.url: " + torrentResponse.getTorrentUrl());
            LOG.info("torrentResponse.status: " + torrentResponse.getTorrentStatus());
            LOG.info("torrentResponse.message: " + torrentResponse.getMessage());
            LOG.info("torrentResponse.modifyTime: " + torrentResponse.getModifyTime());

            // test uploadTorrent
            InfohashTorrent infohashTorrent = new InfohashTorrent();
            infohashTorrent.setInfohash(infohash);
            infohashTorrent.setSource(hdfsUrl);
            infohashTorrent.setTorrentZipCode(torrentResponse.getTorrentZipCode());
            infohashTorrent.setTorrentStatus(TorrentStatus.STATUS_OK);
            GeneralResponse generalResponse = client.uploadTorrent(infohashTorrent);
            LOG.info("generalResponse code:" + generalResponse.getRetCode()
                    + ", message:" + generalResponse.getMessage());

            if (generalResponse.getRetCode() != 0) {
                LOG.error("upload torrent error!");
                System.exit(1);
            }

            // test getInfohashTorrent
            counter = 0;
            isSuccess = true;
            InfohashTorrent newTorrent = client.getInfohashTorrent(infohash);
            while (newTorrent.getTorrentStatus() == TorrentStatus.STATUS_PROCESS) {
                ++counter;
                if (counter >= MAXRETRYTIME) {
                    isSuccess = false;
                    break;
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    break;
                }
                newTorrent = client.getInfohashTorrent(infohash);
            }
            if (!isSuccess) {
                LOG.error("getInfohashTorrent error, counter:" + counter);
                System.exit(1);
            }

            if (!newTorrent.getInfohash().equals(infohash)
                    || !newTorrent.getSource().equals(hdfsUrl)
                    || newTorrent.getTorrentStatus() != TorrentStatus.STATUS_OK
                    || newTorrent.getTorrentZipCode().length != infohashTorrent.getTorrentZipCode().length) {
                LOG.error("getInfohashTorrent error!");
                System.exit(1);
            }

            if (args.length == 5 && newTorrent.getTorrentStatus() == TorrentStatus.STATUS_OK) {
                try {
                    FileOutputStream fos = new FileOutputStream(args[4]);
                    LOG.info("torrent size is " + newTorrent.getTorrentZipCode().length);
                    fos.write(SnappyTool.uncompress(newTorrent.getTorrentZipCode()));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                transport.close();
                System.exit(0);
            }

            transport.close();
        } catch (TException e) {
            LOG.fatal("client error:" + e);
            System.exit(1);
        }


        System.exit(0);
    }

}
