package com.gko3.torrentprovider.torrent;

import com.gko3.torrentprovider.core.GeneralStatus;

/**
 * basic torrent processor interface
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since  JDK 1.6
 */
public interface BaseTorrentProcessor {
    /**
     * set basic config from uri
     * @param uri
     * @return operate status
     */
    abstract GeneralStatus setBasicConfig(String uri);

    /**
     * add directory to this processor
     * @return
     */
    abstract GeneralStatus addDirectory();

    /**
     * return directory modify time
     * @return
     */
    abstract String directoryModifyTime();

    /**
     * error message for this processor
     * @return
     */
    abstract String getErrorMessage();

    /**
     * generate torrent bytes info
     * @return torrent bytes
     */
    abstract byte[] generateTorrent();

    /**
     * release resource
     */
    abstract void release();
}
