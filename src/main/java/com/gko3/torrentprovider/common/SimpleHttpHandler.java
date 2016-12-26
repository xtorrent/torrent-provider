package com.gko3.torrentprovider.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * this class is responsible for deal with http request,support server status report for monitor to collect and other
 * http request
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public abstract class SimpleHttpHandler implements HttpHandler {

    public String serverReportUrl;

    public SimpleHttpHandler(String serverReportUrl) {
        this.serverReportUrl = serverReportUrl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String path = exchange.getRequestURI().getPath();

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(200, 0);

        String response = null;
        if (serverReportUrl.equals(path)) {
            response = generateServerResponseMessage();
        } else {
            response = generateOtherResponseMessage(params, path);
        }

        if (response == null) {
            exchange.sendResponseHeaders(404, 0);
            ReturnMessage<String> returnMessage = new ReturnMessage<String>();
            returnMessage.setSuccess(false);
            returnMessage.setMessage("not support this url");
            response = new Gson().toJson(returnMessage);
        }

        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response.getBytes());
        responseBody.close();
    }

    protected String generateServerResponseMessage() {
        OrderedProperties properties = generateServerReport();
        return properties.toString();
    }

    protected abstract OrderedProperties generateServerReport();

    protected abstract String generateOtherResponseMessage(Map<String, Object> params, String path);
}
