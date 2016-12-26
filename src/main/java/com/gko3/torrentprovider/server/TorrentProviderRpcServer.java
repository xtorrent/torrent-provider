package com.gko3.torrentprovider.server;

import org.apache.log4j.Logger;

import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.server.TThreadedSelectorServer;

import com.gko3.torrentprovider.thrift.TorrentProviderService;
import com.gko3.torrentprovider.common.TorrentProviderConfig;

/**
 * torrent provider rpc server
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentProviderRpcServer implements Runnable {
    private static final Logger LOG = Logger.getLogger(TorrentProviderRpcServer.class);

    @Override
    public void run() {
        try {
            TNonblockingServerSocket socket = new TNonblockingServerSocket(TorrentProviderConfig.serverPort());
            TProcessor processor = new TorrentProviderService.Processor<TorrentProviderServiceHandler>(
                    new TorrentProviderServiceHandler());
            TThreadedSelectorServer .Args arg = new TThreadedSelectorServer.Args(socket);
            arg.protocolFactory(new TBinaryProtocol.Factory());
            arg.transportFactory(new TFramedTransport.Factory(32 * 1024 * 1024));
            arg.processorFactory(new TProcessorFactory(processor));
            arg.workerThreads(TorrentProviderConfig.rpcWorkerNumber());
            arg.selectorThreads(TorrentProviderConfig.rpcSelectorNumber());

            TServer server = new TThreadedSelectorServer(arg);
            server.serve();
        } catch (TTransportException e) {
            LOG.fatal("start server wrong: " + e);
            System.exit(1);
        }
    }
}
