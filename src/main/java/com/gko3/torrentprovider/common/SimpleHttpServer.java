package com.gko3.torrentprovider.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * a light weight http server
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class SimpleHttpServer {
    private static final Logger LOG = Logger.getLogger(SimpleHttpServer.class);

    private int port;
    private String context;
    private HttpHandler handler;
    private static HttpServer server;

    public SimpleHttpServer(int port, String context, HttpHandler handler) {
        this.port = port;
        this.context = context;
        this.handler = handler;
    }

    public synchronized void start() {
        LOG.info("http server start begin.");
        InetSocketAddress address = new InetSocketAddress(port);
        try {
            server = HttpServer.create(address, 0);
        } catch (IOException e) {
            LOG.error("start http server fail!", e);
            return;
        }

        HttpContext httpContext = server.createContext(context, handler);
        httpContext.getFilters().add(new ParameterFilter());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        LOG.info("http server start end at port:" + port);
    }

    public synchronized void stop() {
        server.stop(0);
    }
}
