package com.gko3.torrentprovider.server;

import com.gko3.torrentprovider.core.DatabaseWorker;
import com.gko3.torrentprovider.core.TorrentFailedRetry;
import com.gko3.torrentprovider.core.TorrentGenerator;
import com.gko3.torrentprovider.core.TorrentHttpHandler;
import org.apache.log4j.Logger;

import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.common.ExitHandler;
import com.gko3.torrentprovider.common.SimpleHttpServer;
import com.gko3.torrentprovider.database.TorrentDB;
import com.gko3.torrentprovider.leveldb.LevelDbManager;

/**
 * main class of torrent provider
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentProviderMain {
    private static final Logger LOG = Logger.getLogger(TorrentProviderMain.class);

    public static void main(String[] args) {
        LOG.info("start....");
        try {
            // set signal processor
            ExitHandler.setHandler();

            // init levelDb
            LevelDbManager.getLevelDbManager().open(TorrentProviderConfig.levelDbWriteBufferSize(),
                    TorrentProviderConfig.levelDbMaxOpenFiles(),
                    TorrentProviderConfig.levelDbPath());
            
            // create some threads for generate torrent
            int threadNumber = TorrentProviderConfig.generatorNumber();
            for (int i = 0; i < threadNumber; ++i) {
                TorrentGenerator generator = new TorrentGenerator();
                Thread threadGenerator = new Thread(generator);
                threadGenerator.start();
            }
            
            // create failed retry threads
            Thread threadRetry = new Thread(new TorrentFailedRetry());
            threadRetry.start();

            // init TorrentDB
            // TODO: do not support databse now, but will support in the feature 
            // TorrentDB.getInstance().init();

            // start database worker
            for (int i = 0; i < TorrentProviderConfig.databaseWorkerNumber(); ++i) {
                DatabaseWorker worker = new DatabaseWorker();
                Thread threadGenerator = new Thread(worker);
                threadGenerator.start();
            }

            // start http server
            SimpleHttpServer httpServer = new SimpleHttpServer(
                    TorrentProviderConfig.httpPort(),
                    "/",
                    new TorrentHttpHandler("/server/report"));
            httpServer.start();

            // start rpc server
            TorrentProviderRpcServer server = new TorrentProviderRpcServer();
            Thread tServer = new Thread(server);
            tServer.start();
        } catch (Exception e) {
            LOG.fatal(e);
            System.exit(1);
        }
    }
}
