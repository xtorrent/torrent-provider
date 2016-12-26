package com.gko3.torrentprovider.torrent;

import com.gko3.torrentprovider.common.SnappyTool;
import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.core.GeneralStatus;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * uri agent torrent processor, including generate torrent and check modify time
 * if one agent has N uri requests, we have N connectons to this agent rather than one
 * because manager only one connection for this agent is too complex now
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK 1.6
 */
public class GKOTorrentProcessor implements  BaseTorrentProcessor {
    private static final Logger LOG = Logger.getLogger(GKOTorrentProcessor.class);

    // basic handshake msg and return msg from agent, we need check this
    private static final String HANDSHAKE_MSG =
            "BEGIN_MESSAGE_FOR_HANDSHAKE_INFOHASH_FORPROVIDER_AND_PEER_ID_FORTEST";
    private static final String HANDSHAKE_RETURN_MSG =
            "BEGIN_MESSAGE_FOR_HANDSHAKE_INFOHASH_FORPROVIDER_AaD_PEER_ID_FORTEST";
    private static final int HANDSHAKE_SIZE = 68;

    // int size define
    private static final int INT32_BYTE_SIZE = 4;
    private static final int INT8_BYTE_SIZE = 1;

    // success message return by uri-agent
    private static final String OK_MESSAGE = "OK";

    // extend type number for uri-agent
    private static final int BT_TYPE_EXTEND = 20;

    // generate torrent plugin type for uri-agent
    private static final int BT_GENERATE_TORRENT_TYPE = 30;
    // check timestamp plugin type for uri-agent
    private static final int BT_CHECK_TIMESTAMP_TYPE = 31;
    // check file or dir plugin type for uri-agent
    private static final int BT_CHECK_FILE_DIRECTORY = 32;

    // socket connect timeout, this may need be configed by config file, but now we just set it as a static member
    private static final int SOCKET_SO_TIMEOUT  = 15000;  // 15s

    // default uri-agent port, in fact, uri is always including agent port
    private static final int DEFAULT_URI_AGENT_PORT = TorrentProviderConfig.uriAgentDefaultPort();

    private String agentHost;
    private int agentPort;
    private String gkoPath;
    private String errorMessage;

    // we need this follow two flags for judging weather need resend handshake or reconnect
    private boolean hasSendHandshake = false;
    private boolean isConnected = false;

    // we need set this three stream as class member because if stream close, socket may close?
    // thus may have some problems when we send/recv next message
    private PrintStream writer = null;
    private BufferedInputStream streamReader = null;
    private BufferedReader bufferReader = null;

    // connect socket
    private Socket socket = null;

    public GKOTorrentProcessor() {
    }

    /**
     * set basic config from uri, including connecting to agent and send/recv handshake message
     * @param orgUri uri string
     * @return GeneralStatus
     */
    public GeneralStatus setBasicConfig(String orgUri) {
        // uri format is gko3://db-oped-dev01.db01/home/work/path/to/file
        // or gko3://db-oped-dev01.db01:1234/home/work/path/to/file
        if (!orgUri.startsWith("gko3://")) {
            this.errorMessage = "uri[" + orgUri + "] is invalid!";
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_INVALID_PARAM;
        }

        // set agent host and port
        String uri = orgUri.substring(7);
        int posSlash = uri.indexOf('/');
        if (posSlash == -1) {
            this.errorMessage = "uri[" + orgUri + "] is invalid!";
            LOG.error(this.errorMessage);
            return GeneralStatus.STATUS_INVALID_PARAM;
        }
        gkoPath = uri.substring(posSlash);
        int posPort = uri.indexOf(':');
        if (posPort == -1) {
            agentHost = uri.substring(0, posSlash);
            agentPort = DEFAULT_URI_AGENT_PORT;
        } else {
            agentHost = uri.substring(0, posPort);
            agentPort = Integer.parseInt(uri.substring(posPort + 1, posSlash));
        }

        if (!isConnected) {
            // connect to agent
            try {
                connectToAgent();
            } catch (UnknownHostException e) {
                this.errorMessage = "host:" + agentHost + " unknown:" + e;
                LOG.error(this.errorMessage);
                return GeneralStatus.STATUS_UNKNOWN_HOST;
            } catch (IOException e) {
                this.errorMessage = toString() + " io exception:" + e;
                LOG.error(this.errorMessage);
                // io exception, need retry
                return GeneralStatus.STATUS_ERROR;
            }
        }

        if (!hasSendHandshake) {
            // we haven't sent handshake or some error occurs, send it
            if (!sendAndRecvHandshake()) {
                this.errorMessage = "send and recv handshake error!";
                LOG.error(this.errorMessage);
                closeConnection();
                // handshake error, retry it
                return GeneralStatus.STATUS_ERROR;
            }
        }

        hasSendHandshake = true;
        LOG.info("set basic config success, uri:" + orgUri);
        return GeneralStatus.STATUS_OK;
    }

    /**
     * add directory, in fact, this function check gkoPath is exist in uri-agent
     *
     * @return GeneralStatus
     */
    public GeneralStatus addDirectory() {
        // check if directory exists
        if (!isConnected || !hasSendHandshake) {
            this.errorMessage = "agent not connected or handshake not send!";
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_NOT_CONNECT;
        }
        if (gkoPath == null || gkoPath.isEmpty()) {
            this.errorMessage = "gko path is empty!";
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_INVALID_PARAM;
        }

        // send request to agent
        byte[] sendBytes = constructRequestMessage(BT_CHECK_FILE_DIRECTORY);
        writer.write(sendBytes, 0, sendBytes.length);
        try {
            String line = bufferReader.readLine();
            if (line == null || !line.equals(OK_MESSAGE)) {
                this.errorMessage = "check file or directory error";
                if (line != null) {
                    this.errorMessage += ":" + line;
                }
                LOG.warn(this.errorMessage);
                return GeneralStatus.STATUS_FILE_NOT_EXIST;
            } else {
                return GeneralStatus.STATUS_OK;
            }
        } catch (IOException e) {
            this.errorMessage = "io exception when check file or directory:" + e;
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_ERROR;
        }
    }

    /**
     * get error message
     *
     * @return error message
     */
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * close connection to uri-agent
     */
    public void release() {
        closeConnection();
    }

    /**
     * get directory or file modify time
     *
     * @return directory modify time, if some error happens, return an empty String
     */
    public String directoryModifyTime() {
        String modifyTime = new String();
        if (!isConnected || !hasSendHandshake) {
            this.errorMessage = "agent not connected or handshake not send!";
            LOG.warn(this.errorMessage);
            return modifyTime;
        }
        if (gkoPath == null || gkoPath.isEmpty()) {
            this.errorMessage = "gko path is empty!";
            LOG.warn(this.errorMessage);
            return modifyTime;
        }

        // send request to agent
        byte[] sendBytes = constructRequestMessage(BT_CHECK_TIMESTAMP_TYPE);
        writer.write(sendBytes, 0, sendBytes.length);
        try {
            String line;
            int counter = 0;
            while (true) {
                ++counter;
                line = bufferReader.readLine();
                if (line == null) {
                    this.errorMessage = "read data from agent failed when check dir!";
                    LOG.warn(this.errorMessage);
                    return modifyTime;
                }
                if (counter == 1)  {
                    // message field
                    if (!line.equals(OK_MESSAGE)) {
                        this.errorMessage = "check timestamp error:" + line;
                        LOG.warn(this.errorMessage);
                        return modifyTime;
                    }
                } else if (counter == 2) {
                    modifyTime = line;
                }

                if (line.equals("TORRENT_END") || counter >= 3) {
                    break;
                }
            }

        } catch (IOException e) {
            this.errorMessage = "io exception when check modify time:" + e;
            LOG.warn(this.errorMessage);
        }

        LOG.info("director modify time is " + modifyTime);
        return modifyTime;
    }

    /**
     * generate Torrent for gkoPath
     * this function just send request to uri-agent and get generated torrent info
     * this function also snappy compress torrent info
     *
     * @return torrent info, if some errors occur, return null
     */
    public byte[] generateTorrent() {
        if (!isConnected || !hasSendHandshake) {
            this.errorMessage = "agent not connected or handshake not send!";
            LOG.warn(this.errorMessage);
            return null;
        }

        if (gkoPath == null || gkoPath.isEmpty()) {
            this.errorMessage = "gko path is empty!";
            LOG.warn(this.errorMessage);
            return null;
        }

        byte[] sendBytes = constructRequestMessage(BT_GENERATE_TORRENT_TYPE);
        writer.write(sendBytes, 0, sendBytes.length);

        // receive message
        String torrentString = new String();
        try {
            String line;
            int counter = 0;
            while (true) {
                ++counter;
                line = bufferReader.readLine();
                if (line == null) {
                    this.errorMessage = "read from agent failed when get torrent!";
                    LOG.warn(this.errorMessage);
                    return null;
                }
                if (counter == 1) {
                    // message field
                    if (!line.equals(OK_MESSAGE)) {
                        // some error happend
                        this.errorMessage = "generate torrent error:" + line;
                        LOG.warn(this.errorMessage);
                        return null;
                    }
                } else if (counter == 2) {
                    // infohash field
                    LOG.info("infohash is: " + line + toString());
                } else if (counter == 3) {
                    // torrent code
                    torrentString = line;
                }

                if (line.equals("TORRENT_END") || counter >= 5) {
                    break;
                }
            }
        } catch (IOException e) {
            this.errorMessage = "generate torrent exception:" + e;
            LOG.warn(this.errorMessage);
            return null;
        }

        try {
            return SnappyTool.compress(torrentString.getBytes());
        } catch (Exception e) {
            this.errorMessage = "compress failed:" + e;
            LOG.error(this.errorMessage);
        }
        return null;
    }

    /**
     * connection to uri-agent
     * @throws IOException
     */
    private void connectToAgent() throws IOException {
        if (socket != null && !socket.isClosed()) {
            isConnected = true;
            return;
        }

        socket = new Socket(agentHost, agentPort);
        socket.setSoTimeout(SOCKET_SO_TIMEOUT);
        writer = new PrintStream(socket.getOutputStream(), false);
        streamReader = new BufferedInputStream(socket.getInputStream());
        bufferReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        isConnected = true;
        LOG.info("connecting to " + agentHost + ":" + agentPort + " success!");
    }

    /**
     * close connection to uri-agent and set connected/handshake flag
     */
    private void closeConnection() {
        if (writer != null) {
            writer.close();
        }

        if (streamReader != null) {
            try {
                streamReader.close();
            } catch (IOException e) {
                LOG.error("close streamReader error:" + e);
            }
        }

        if (bufferReader != null) {
            try {
                bufferReader.close();
            } catch (IOException e) {
                LOG.error("close buffer reader error:" + e);
            }
        }

        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.error("close socket error:" + e);
            }
        }

        LOG.info(toString() + "connection close!");
        hasSendHandshake = false;
        isConnected = false;
    }

    /**
     * send and recv handshake
     * @return success true else false
     */
    private boolean sendAndRecvHandshake() {
        // if hasn't connected, just reconnect
        if (!isConnected) {
            try {
                connectToAgent();
            } catch (IOException e) {
                return false;
            }
        }

        boolean isSuccess = true;
        try {
            writer.print(HANDSHAKE_MSG);
            writer.flush();

            byte[] buffer = new byte[HANDSHAKE_SIZE];
            streamReader.read(buffer);
            String handshakeRet = new String(buffer);
            if (!handshakeRet.equals(HANDSHAKE_RETURN_MSG)) {
                LOG.warn("handshakeRet:" + handshakeRet + ", return msg error!");
                isSuccess = false;
            }
        } catch (IOException e) {
            LOG.error("io excpetion when sand handshake");
            isSuccess = false;
        }

        return isSuccess;
    }

    /**
     * construct bt-extened request message
     *
     * @param type bt plugin type
     * @return request message bytes
     */
    private byte[] constructRequestMessage(int type) {
        byte[] btType = intToBytesWithReverse(BT_TYPE_EXTEND, INT8_BYTE_SIZE);
        byte[] btPluginType = intToBytesWithReverse(type, INT8_BYTE_SIZE);
        byte[] msgLength = intToBytesWithReverse(2 * INT8_BYTE_SIZE + gkoPath.length(), INT32_BYTE_SIZE);
        byte[] sendBytes = new byte[2 * INT8_BYTE_SIZE + INT32_BYTE_SIZE + gkoPath.length()];
        System.arraycopy(msgLength, 0, sendBytes, 0, INT32_BYTE_SIZE);
        System.arraycopy(btType, 0, sendBytes, INT32_BYTE_SIZE, INT8_BYTE_SIZE);
        System.arraycopy(btPluginType, 0, sendBytes, INT32_BYTE_SIZE + INT8_BYTE_SIZE, INT8_BYTE_SIZE);
        System.arraycopy(gkoPath.getBytes(), 0, sendBytes, 2 * INT8_BYTE_SIZE + INT32_BYTE_SIZE, gkoPath.length());
        return sendBytes;
    }

    /**
     * int to reverse bytes
     * @param source    int source
     * @param arrayLen  int length
     * @return int bytes, if some errors occur, return null
     */
    private static byte[] intToBytesWithReverse(int source, int arrayLen) {
        if (arrayLen <= 0) {
            return null;
        }

        byte[] target = new byte[arrayLen];
        for (int i = 0; i < arrayLen; ++i) {
            target[arrayLen - 1 - i] = (byte) (source >> (8 * i) & 0xFF);
        }

        return target;
    }

    @Override
    public String toString() {
        return "URITorrentProcessor{"
                + "agentHost='" + agentHost + '\''
                + ", agentPort=" + agentPort
                + ", gkoPath=" + gkoPath
                + '}';
    }

}
