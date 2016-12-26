package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.bean.InfohashProcessQueue;
import com.gko3.torrentprovider.bean.InfohashTorrentMap;
import com.gko3.torrentprovider.bean.ProcessKeeper;
import com.gko3.torrentprovider.bean.URIProcessQueue;
import com.gko3.torrentprovider.bean.URIStatistics;
import com.gko3.torrentprovider.bean.URITorrentMap;
import org.apache.log4j.Logger;

import com.gko3.torrentprovider.common.SimpleHttpHandler;
import com.gko3.torrentprovider.common.OrderedProperties;

import java.util.Map;

/**
 * Torrent http handler 
 * for status monitor and control
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentHttpHandler extends SimpleHttpHandler {
    private static final Logger LOG = Logger.getLogger(TorrentHttpHandler.class);

    private static URITorrentMap uriTorrentMap = URITorrentMap.getInstance();
    private static ProcessKeeper processKeeper = ProcessKeeper.getInstance();
    private static URIProcessQueue uriProcessQueue = URIProcessQueue.getInstance();
    private static URIStatistics uriStatistics = URIStatistics.getInstance();
    private static TorrentCache torrentCache = TorrentCache.getTorrentCache();
    private static InfohashTorrentMap infohashMap = InfohashTorrentMap.getInstance();
    private static InfohashProcessQueue infohashProcessQueue = InfohashProcessQueue.getInstance();

    public TorrentHttpHandler(String serverReportUrl) {
        super(serverReportUrl);
    }

    protected String generateOtherResponseMessage(Map<String, Object> params, String path) {
        if ("/control/deleteUri".equals(path)) {
            return generateControlDeleteUri(params);
        }
        if ("/query/uri".equals(path)) {
            return generateUriInfo(params);
        }
        return null;
    }

    protected OrderedProperties generateServerReport() {
        LOG.info("generateServerReport");

        OrderedProperties properties = new OrderedProperties();
        properties.put("URIMapTotal", uriTorrentMap.size());
        properties.put("URIWaitSize", uriProcessQueue.size());
        properties.put("TotalURIRequest", uriStatistics.getTotalURIRequest());
        properties.put("TotalURISuccessRequest", uriStatistics.getTotalURISuccessRequest());
        properties.put("TotalURIProcess", uriStatistics.getTotalURIProcess());
        properties.put("TotalSuccessURIProcess", uriStatistics.getTotalSuccessURIProcess());
        properties.put("TotalURICacheProcess", uriStatistics.getTotalURICacheProcess());
        properties.put("TotalURISuccessCacheProcess", uriStatistics.getTotalURISuccessCacheProcess());

        properties.put("InfohashMapTotal", infohashMap.size());
        properties.put("InfohashWaitSize", infohashProcessQueue.size());
        properties.put("TotalInfohashRequest", uriStatistics.getTotalInfohashRequest());
        properties.put("TotalSuccessInfohashRequest", uriStatistics.getTotalSuccessInfohashReqeust());
        properties.put("TotalUploadRequest", uriStatistics.getTotalUploadRequest());
        properties.put("TotalSuccessUploadRequest", uriStatistics.getTotalSuccessUploadRequest());

        properties.put("StartTime", uriStatistics.getStartTimestamp() / 1000);
        properties.put("EndTime", System.currentTimeMillis() / 1000);
        return properties;
    }

    private String generateControlDeleteUri(Map<String, Object> params) {
        String uri = (String) params.get("uri");
        LOG.info("delete URIInfo:" + uri);

        if (uri == null) {
            return "request uri param!\n";
        }

        URITorrent torrentFromMap = uriTorrentMap.getTorrent(uri);
        URITorrent torrentFromCache = torrentCache.getTorrent(uri);
        if (torrentFromMap == null && torrentFromCache == null) {
            LOG.info("delete but not exist uri:" + uri);
            return "uri not exist, delete success!\n";
        }

        if (torrentFromMap != null) {
            uriTorrentMap.removeTorrent(uri);
            LOG.info("remove torrent uri:" + uri + " from map by http");
        }

        if (torrentFromCache != null) {
            torrentCache.deleteTorrent(uri);
            LOG.info("remove torrent uri:" + uri + " from cache by http");
        }

        return "delete success!\n";
    }

    private String generateUriInfo(Map<String, Object> params) {
        String uri = (String) params.get("uri");
        LOG.info("get URIInfo:" + uri);

        if (uri == null) {
            return "request uri param!\n";
        }

        URITorrent torrent = uriTorrentMap.getTorrent(uri);
        if (torrent != null) {
            LOG.info("get success:" + uri);
            return torrent.toString() + "\n";
        }

        // if not get, put it into process queue
        if (!processKeeper.isExists(uri)) {
            processKeeper.addKey(uri);
            uriProcessQueue.put(uri);
            LOG.info("uri[" + uri + "] add to queue by http");
        }
        LOG.info("get URIInfo by http in processing:" + uri);
        return "waiting for process!\n";
    }
}

