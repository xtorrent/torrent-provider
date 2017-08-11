package com.gko3.torrentprovider.torrent;

import com.gko3.torrentprovider.common.TorrentProviderConfig;
import com.gko3.torrentprovider.core.GeneralStatus;
import org.apache.log4j.Logger;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import com.gko3.torrentprovider.common.SnappyTool;

/**
 * This class is responsible for hdfs torrent file
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class HdfsTorrentProcessor implements BaseTorrentProcessor {
    private static final Logger LOG = Logger.getLogger(HdfsTorrentProcessor.class);

    // gzip compress buffer size 1M
    private static final int COMPRES_BUFFER_SIZE = 1024;

    private TorrentFile torrent;

    // hdfs username
    private String username;
    private String hdfsUrl;
    private String hdfsHost;
    private String hdfsPath;

    // hdfs FileSystem
    private FileSystem hdfsFileSystem;

    // hdfs error message
    private String errorMessage = "OK";

    public String getErrorMessage() {
        return errorMessage;
    }

    public HdfsTorrentProcessor(TorrentFile torrent) {
        this.torrent = torrent;
    }

    public HdfsTorrentProcessor() {
        LOG.debug("call HdfsTorrentProcessor constructor");
        this.torrent = new TorrentFile();
        // TODO: should be modified available, default 8M
        setPieceLength(TorrentProviderConfig.defaultPieceSize() * 1024);
        LOG.debug("finish call HdfsTorrentProcessor constructor");
    }

    /**
     * set hdfs basic config from uri
     *
     * @param uri   the origin uri
     * @return HdfsStatus
     */
    public GeneralStatus setBasicConfig(String uri) {
        // uri format: hdfs://user@test.hdfs.com:54310/path/to/dir
        if (!uri.startsWith("hdfs://")) {
            this.errorMessage = "uri[" + uri + "] is invalid!";
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_INVALID_PARAM;
        } 
        uri = uri.substring(7);
        int lastAtPos = uri.lastIndexOf("@");
        if (lastAtPos == -1) {
            this.errorMessage = "uri[" + uri + "] is invalid!";
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_INVALID_PARAM;
        }

        this.username = uri.substring(0, lastAtPos);
        this.hdfsUrl = "hdfs://" + uri.substring(lastAtPos + 1);
        if (this.hdfsUrl.charAt(this.hdfsUrl.length() - 1) == '/') {
            this.hdfsUrl = this.hdfsUrl.substring(0, this.hdfsUrl.length() - 1);
        }

        // hdfs url is hdfs://test.hdfs.com:54310/path/to/dir/file
        // we need to get the last level name 
        String hdfsUrl = this.hdfsUrl;
        if (hdfsUrl.charAt(hdfsUrl.length() - 1) == '/') {
            hdfsUrl = hdfsUrl.substring(0, hdfsUrl.length() - 1);
        }
        int pos = hdfsUrl.lastIndexOf('/');
        this.torrent.saveAs = hdfsUrl.substring(pos + 1);

        // get hdfsHost and hdfsPath
        int posPortStart = hdfsUrl.lastIndexOf(':');
        int posPortEnd = hdfsUrl.indexOf('/', posPortStart);
        this.hdfsHost = hdfsUrl.substring(0, posPortEnd);
        this.hdfsPath = hdfsUrl; //.substring(posPortEnd);
        return initHdfsFileSystem();
    }

    /**
     * add directory to the torrent
     *
     * @return HdfsStatus
     */
    public GeneralStatus addDirectory() {
        // check if file exists first
        try {
            LOG.debug("call addDirectory()");
            FileStatus fileStatus = hdfsFileSystem.getFileStatus(new Path(this.hdfsPath));
            if (fileStatus.isDir()) {
                this.torrent.singleFileNeedSetPath = true;
            }
        } catch (FileNotFoundException e) {
            this.errorMessage = "directory[" + this.hdfsPath + "] not exist:" + e;
            LOG.error(this.errorMessage);
            return GeneralStatus.STATUS_FILE_NOT_EXIST;
        } catch (IOException e) {
            this.errorMessage = "add directory[" + hdfsPath + "] error:" + e;
            LOG.error(this.errorMessage);
            return GeneralStatus.STATUS_ERROR;
        }

        return addDirectory(this.hdfsPath, "");
    }

    /**
     * add directory inner function
     *
     * @param hdfsPath
     * @param dirName   Hdfs dir(subdir) name
     * @return HdfsStatus
     */
    private GeneralStatus addDirectory(String hdfsPath, String dirName) {
        try {
            LOG.debug("call addDirectory(hdfsPath, dirName)");
            FileStatus[] fileList = hdfsFileSystem.listStatus(new Path(hdfsPath));
            for (int i = 0; i < fileList.length; ++i) {
                if (fileList[i].isDir()) {
                    String subDirName;
                    if (!dirName.isEmpty()) {
                        subDirName = dirName + "/" + fileList[i].getPath().getName();
                    } else {
                        subDirName = fileList[i].getPath().getName();
                    }

                    GeneralStatus currentStatus = addDirectory(
                            hdfsPath + "/" + fileList[i].getPath().getName(),
                            subDirName);
                    if (currentStatus != GeneralStatus.STATUS_OK) {
                        return currentStatus;
                    }
                } else {
                    String subPathName;
                    if (!dirName.isEmpty()) {
                        subPathName = dirName + "/" + fileList[i].getPath().getName();
                    } else {
                        subPathName = fileList[i].getPath().getName();
                    }
                    LOG.debug("file is " + subPathName);
                    this.torrent.totalLength += fileList[i].getLen();
                    LOG.debug("dir name is " + hdfsPath + "/" + fileList[i].getPath().getName());
                    this.torrent.path.add(hdfsPath + "/" + fileList[i].getPath().getName());
                    this.torrent.name.add(subPathName);
                    this.torrent.length.add(new Long(fileList[i].getLen()).longValue());
                }
            }  // for int i = 0
        } catch (FileNotFoundException e) {
            this.errorMessage = "directory[" + hdfsPath + "]dirname[" + dirName + "]not found:" + e;
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_FILE_NOT_EXIST;
        } catch (IOException e) {
            this.errorMessage = "add directory[" + hdfsPath + "]dirname[" + dirName + "]error:" + e;
            LOG.warn(this.errorMessage);
            return GeneralStatus.STATUS_ERROR;
        }

        return GeneralStatus.STATUS_OK;
    }

    /**
     * get hdfs url directory last modification time
     *
     * @return modifyTime String
     */
    public String directoryModifyTime() {
        return this.directoryModifyTime(this.hdfsPath);
    }

    /**
     * get hdfs url directory last modification time
     *
     * @param hdfsUrl   the url want to get
     * @return modifyTime String
     */
    private String directoryModifyTime(String hdfsUrl) {
        String modifyTime = "";

        try {
            FileStatus fileStatus = hdfsFileSystem.getFileStatus(new Path(hdfsUrl));
            modifyTime = Long.toString(fileStatus.getModificationTime());
        } catch (IOException e) {
            this.errorMessage = "get directory modify time error:" + e;
            LOG.error(this.errorMessage);
        }
        return modifyTime;
    }

    /**
     * Generate the bytes for the current object TorrentFile
     *
     * @return byte[]
     */
    public byte[] generateTorrent() {
        return this.generateTorrent(this.torrent);
    }

    /**
     * Generate the bytes of the bencoded TorrentFile data
     *
     * @param torr TorrentFile
     * @return byte[]
     */
    private byte[] generateTorrent(TorrentFile torr) {
        SortedMap map = new TreeMap();
        map.put("announce", torr.announceURL);
        if (torr.comment.length() > 0) {
            map.put("comment", torr.comment);
        }

        if (torr.creationDate >= 0) {
            map.put("creation date", torr.creationDate);
        }

        if (torr.createdBy.length() > 0) {
            map.put("created by", torr.createdBy);
        }

        SortedMap info = new TreeMap();
        if (torr.name.size() == 1 && !torr.singleFileNeedSetPath) {
            info.put("length", torr.length.get(0));
            info.put("name", new File((String) torr.name.get(0)).getName());
        } else {
            if (!torr.saveAs.matches("")) {
                info.put("name", torr.saveAs);
            } else {
                info.put("name", " ");
            }
            ArrayList files = new ArrayList();
            for (int i = 0; i < torr.name.size(); i++) {
                SortedMap file = new TreeMap();
                file.put("length", torr.length.get(i));
                String[] path = ((String) torr.name.get(i)).split("\\\\");

                ArrayList pathList = new ArrayList(path.length);
                for (int j = (path.length > 1) ? 1 : 0; j < path.length; j++) {
                    pathList.add(path[j]);
                }
                file.put("path", pathList);
                files.add(file);
            }
            info.put("files", files);
        }
        info.put("piece length", torr.pieceLength);

        int piecesNumber = (int) (torr.totalLength / torr.pieceLength + 1);
        int totalNumbers = piecesNumber * 20;
        byte[] pieces = new byte[totalNumbers];
        for (int i = 0; i < totalNumbers; ++i) {
            pieces[i] = 0;
        }
        info.put("pieces", pieces);

        // some hadoop info
        info.put("HADOOP_PROTOCOL", "hdfs");

        info.put("HADOOP_USER", this.username);
        LOG.debug("HADOOP_USER: " + this.username);

        // hdfsUrl is hdfs://test.hdfs.com:54310/path/to/dir/file
        String hdfsUrl = this.hdfsUrl;
        if (hdfsUrl.startsWith("hdfs://")) {
            hdfsUrl = hdfsUrl.substring(7);
        }
        int pos = hdfsUrl.indexOf('/');
        if (pos != -1) {
            String[] hostPort = hdfsUrl.substring(0, pos).split(":");
            LOG.debug("raw host:port is " + hdfsUrl.substring(0, pos));
            if (hostPort.length == 2) {
                info.put("HADOOP_HOST", hostPort[0]);
                info.put("HADOOP_PORT", Integer.parseInt(hostPort[1]));
                LOG.debug("HADOOP_HOST:" + hostPort[0] + ", HADOOP_PORT:" + hostPort[1]);
            }

            // need check parent path, that is /path/to/dir need /path/to
            String fullDir = hdfsUrl.substring(pos);
            if (fullDir.length() > 1 && fullDir.charAt(fullDir.length() - 1) == '/') {
                fullDir = fullDir.substring(0, fullDir.length() - 1);
            }
            pos = fullDir.lastIndexOf('/');
            if (pos != -1) {
                info.put("dest_src", fullDir.substring(0, pos));
                LOG.debug("dest_src:" + fullDir.substring(0, pos));
            }
        }

        map.put("info", info);
        try {
            LOG.debug("encode map object with BEncoder");
            byte[] data = BEncoder.encode(map);
            return SnappyTool.compress(data);
        } catch (Exception e) {
            this.errorMessage = "compress failed:" + e;
            LOG.error(this.errorMessage);
        }
        return null;
    }

    public void release() {
        // do nothing
    }

    /**
     * Sets the pieceLength
     *
     * @param length int
     */
    public void setPieceLength(int length) {
        this.torrent.pieceLength = length * 1024;
    }

    /**
     * Sets the directory the files have to be saved in (in case of multiple files torrent)
     *
     * @param name String
     */
    public void setName(String name) {
        this.torrent.saveAs = name;
    }

    /**
     * Sets the comment about this torrent
     *
     * @param comment String
     */
    public void setComment(String comment) {
        this.torrent.comment = comment;
    }

    /**
     * Sets the creator of the torrent. This should be the client name and version
     *
     * @param creator String
     */
    public void setCreator(String creator) {
        this.torrent.createdBy = creator;
    }

    /**
     * Sets the time the torrent was created
     *
     * @param date long
     */
    public void setCreationDate(long date) {
        this.torrent.creationDate = date;
    }

    /**
     * Sets the encoding of the torrent
     *
     * @param encoding String
     */
    public void setEncoding(String encoding) {
        this.torrent.encoding = encoding;
    }

    /**
     * init hdfs filesystem, and set this.hdfsFileSystem
     *
     * @return HdfsStatus
     */
    private GeneralStatus initHdfsFileSystem() {
        if (this.hdfsUrl.isEmpty() || this.hdfsHost.isEmpty()) {
            return GeneralStatus.STATUS_INVALID_PARAM;
        }

        try {
            String uri = this.username + "@" + this.hdfsHost;
            hdfsFileSystem = HdfsConnectionManager.getHdfsConnectionManager().getFileSystem(uri);
            if (hdfsFileSystem == null) {
                this.errorMessage = "hdfsUrl:" + this.hdfsUrl + " is invalid";
                LOG.error(this.errorMessage);
                return GeneralStatus.STATUS_INVALID_PARAM;
            }
        } catch (UnknownHostException e) {
            this.errorMessage = this.hdfsHost + "unknown:" + e;
            LOG.error(this.errorMessage);
            return GeneralStatus.STATUS_UNKNOWN_HOST;
        } catch (IOException e) {
            this.errorMessage = "get " + this.hdfsHost + "file system error:" + e;
            LOG.error(this.errorMessage);
            return GeneralStatus.STATUS_ERROR;
        }

        return GeneralStatus.STATUS_OK;
    }

    /**
     * Returns the gzip compress data bytes
     *
     * @param data input data
     * @return byte[] gzip compress data
     */
    private byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        GZIPOutputStream gos = new GZIPOutputStream(baos);
        int count = -1;
        byte[] buffer = new byte[COMPRES_BUFFER_SIZE];
        while ((count = bais.read(buffer, 0, COMPRES_BUFFER_SIZE)) != -1) {
            gos.write(buffer, 0, count);
        }
        gos.finish();
        gos.flush();
        gos.close();

        byte[] output = baos.toByteArray();
        baos.flush();
        baos.close();
        bais.close();
        return output;
    }
}
