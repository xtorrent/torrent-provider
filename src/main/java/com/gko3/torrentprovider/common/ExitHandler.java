package com.gko3.torrentprovider.common;

import org.apache.log4j.Logger;

import com.gko3.torrentprovider.bean.URITorrentMap;
import com.gko3.torrentprovider.torrent.HdfsConnectionManager;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * exit signal handler
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class ExitHandler extends Thread {
    private static final Logger LOG = Logger.getLogger(ExitHandler.class);

    private static Signal signalType = new Signal("TERM");
    private static SignalHandler signalHandler = new SignalHandler() {
        @Override
        public void handle(Signal signal) {
            LOG.info("reveive TERM signal ...");
            URITorrentMap.getInstance().putAllTorrentIntoCache();
            HdfsConnectionManager.getHdfsConnectionManager().closeAllFileSystem();
            System.runFinalization();
            System.exit(0);
        }
    };

    public static void setHandler() {
        Signal.handle(signalType, signalHandler);
    }
}

