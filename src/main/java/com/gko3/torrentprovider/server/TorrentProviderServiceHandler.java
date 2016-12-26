package com.gko3.torrentprovider.server;

import com.gko3.torrentprovider.bean.InfohashProcessQueue;
import com.gko3.torrentprovider.bean.InfohashTorrentMap;
import com.gko3.torrentprovider.bean.ProcessKeeper;
import com.gko3.torrentprovider.bean.URIProcessQueue;
import com.gko3.torrentprovider.bean.URIStatistics;
import com.gko3.torrentprovider.bean.URITorrentMap;
import com.gko3.torrentprovider.core.TorrentInfo;
import com.gko3.torrentprovider.database.TorrentDB;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;

import com.gko3.torrentprovider.thrift.GeneralResponse;
import com.gko3.torrentprovider.thrift.InfohashTorrent;
import com.gko3.torrentprovider.thrift.ProviderServerCmd;
import com.gko3.torrentprovider.thrift.TorrentResponse;
import com.gko3.torrentprovider.thrift.TorrentStatus;
import com.gko3.torrentprovider.thrift.TorrentProviderService;

import com.gko3.torrentprovider.core.URITorrent;

/**
 * torrent provider service handler
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentProviderServiceHandler implements TorrentProviderService.Iface {
    private static final Logger LOG = Logger.getLogger(TorrentProviderServiceHandler.class);

    // ret code for control command
    private static final int OK = 0;
    private static final int FAIL = 1;

    /**
     * get torrent zip code by uri from rpc client
     *
     * @param uri   uri for getting
     * @return torrent response, status store in torrentStatus
     * @throws TException
     */
    public TorrentResponse getTorrentZipCode(String uri) throws TException {
        LOG.info("get uri:" + uri);
        TorrentResponse response = new TorrentResponse();
        response.setTorrentUrl(uri);

        URITorrent torrent = URITorrentMap.getInstance().getTorrent(uri);
        if (torrent != null) {
            LOG.debug("get torrent[" + uri + "] from map");
            response.setTorrentStatus(torrent.getTorrentStatus());
            response.setTorrentZipCode(torrent.getTorrentZipCode());
            String modifyTime = torrent.getLastModifyTime();
            if (modifyTime != null) {
                response.setModifyTime(Long.parseLong(torrent.getLastModifyTime()));
            }
            response.setMessage(torrent.getMessage());
            if (torrent.getTorrentStatus() == TorrentStatus.STATUS_ERROR
                    || torrent.getTorrentStatus() == TorrentStatus.STATUS_ERROR_FILE_NOT_EXIST) {
                URIStatistics.getInstance().addURIRequest(false);
            } else {
                URIStatistics.getInstance().addURIRequest(true);
            }
            return response;
        }
        URIStatistics.getInstance().addURIRequest(true);

        // check if is in processing
        response.setTorrentStatus(TorrentStatus.STATUS_PROCESS);
        ProcessKeeper keeper = ProcessKeeper.getInstance();
        if (!keeper.isExists(uri)) {
            keeper.addKey(uri);
            URIProcessQueue.getInstance().put(uri);
            LOG.info("uri[" + uri + "] add to queue");
        } else {
            LOG.info("uri[" + uri + "] is already in queue, waiting for or in process");
        }

        response.setMessage("torrent is in processing");
       
        return response;
    }

    /**
     * control server from rpc client, this function needs consummate
     *
     * @param cmd   rpc client cmd for server
     * @return cmd result
     * @throws TException
     */
    public GeneralResponse controlServer(ProviderServerCmd cmd) throws TException {
        LOG.info("get provider server cmd type: " + cmd.type + ", cmd:" + cmd.cmd);
        GeneralResponse response = new GeneralResponse();
        response.setRetCode(TorrentProviderServiceHandler.OK);
        response.setMessage("OK");
        return response;
    }

    /**
     * upload torrent info from gko3 client
     *
     * @param torrent torrent info, including infohash, torrent code, etg.
     * @return GeneralResponse
     * @throws TException
     */
    public GeneralResponse uploadTorrent (InfohashTorrent torrent) throws TException {
        GeneralResponse response = TorrentDB.getInstance().uploadTorrent(torrent);
        URIStatistics.getInstance().addUploadRequest(response.getRetCode() == OK ? true : false);
        return response;
    }

    /**
     * get infohash torrent info for gko3 client
     *
     * @param infohash request infohash
     * @return torrent info for request infohash to gko3 client
     * @throws TException
     */
    public InfohashTorrent getInfohashTorrent(String infohash) throws TException {
        LOG.info("get infohash:" + infohash);

        TorrentInfo info = InfohashTorrentMap.getInstance().getTorrent(infohash);
        if (info != null) {
            LOG.debug("get infohash[" + infohash + "] from map");
            if (info.getTorrentStatus() == TorrentStatus.STATUS_ERROR) {
                URIStatistics.getInstance().addInfohashRequest(false);
            } else {
                URIStatistics.getInstance().addInfohashRequest(true);
            }
            return info.generateInfohashTorrent();
        }

        URIStatistics.getInstance().addInfohashRequest(true);

        InfohashTorrent infohashTorrent = new InfohashTorrent();
        infohashTorrent.setInfohash(infohash);
        infohashTorrent.setTorrentStatus(TorrentStatus.STATUS_PROCESS);
        infohashTorrent.setMessage("torrent in processing...");
        // check if is in processing
        ProcessKeeper keeper = ProcessKeeper.getInstance();
        if (!keeper.isExists(infohash)) {
            keeper.addKey(infohash);
            InfohashProcessQueue.getInstance().put(infohash);
            LOG.info("infohash[" + infohash + "] add to queue");
        } else {
            LOG.info("infohash[" + infohash + "] is already in queue, waiting for or in process");
        }
        return infohashTorrent;
    }
}
