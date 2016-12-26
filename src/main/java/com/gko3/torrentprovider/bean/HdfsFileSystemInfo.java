package com.gko3.torrentprovider.bean;


import org.apache.hadoop.fs.FileSystem;

/**
 * hdfs fileSystem base info, include FileSystem and last access timestamp
 *
 * @author Chaobin He <hechaobin1988@163.com>
 * @since JDK1.6
 */
public class HdfsFileSystemInfo {
    private FileSystem fs;
    private long lastAccessTimestamp;

    public long getLastAccessTimestamp() {
        return this.lastAccessTimestamp;
    }

    public FileSystem getFileSystem() {
        setLastAccessTimestamp();
        return fs;
    }

    public void setLastAccessTimestamp() {
        this.lastAccessTimestamp = System.currentTimeMillis();
    }

    public void setFileSystem(FileSystem fs) {
        setLastAccessTimestamp();
        this.fs = fs;
    }

}
