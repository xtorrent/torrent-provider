package com.gko3.torrentprovider.core;

import com.gko3.torrentprovider.bean.URITorrentBaseInfo;
import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.gko3.torrentprovider.common.SerializeUtil;

/**
 * Unit test for URITorrent
 */
public class URITorrentTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(URITorrentTest.class);
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGenerateTorrentSuccess() {
        URITorrent torrent = new URITorrent();

        // test hdfs torrent
        torrent.setUri("hdfs://user:pass@test.hdfs.com:54310/test/hechaobin01");
        GeneralStatus status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_OK);

        assertNotNull(torrent.getUriLastModifyTime());
        assertNotNull(torrent.getTorrentZipCode());

        // test gko torrent
        torrent.setUri("gko3://db-oped-dev01.db01:4460/home/users/hechaobin01/tmp/dt-agent.proto");
        status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_OK);

        assertNotNull(torrent.getUriLastModifyTime());
        assertNotNull(torrent.getTorrentZipCode());
    }

    public void testGenerateTorrentFailed() {
        // hdfs
        URITorrent torrent = new URITorrent();
        torrent.setUri("hdfs://user:pass@test.hdfs.com:54310/test/not_exist");
        GeneralStatus status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_FILE_NOT_EXIST);
        assertTrue(torrent.getUriLastModifyTime().isEmpty());

        torrent.setUri("hdfs://user:pass@test.hdfs.com:5431/test/not_exist");
        status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_ERROR);


        // gko
        torrent.setUri("gko3://db-oped-dev01.db01:4460/home/xxx");
        status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_FILE_NOT_EXIST);
        assertTrue(torrent.getUriLastModifyTime().isEmpty());

        torrent.setUri("gko3://db-oped-dev01.db01:3333/home/xxx");
        status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_ERROR);
    }

    public void testSerialize() {
        URITorrent torrent = new URITorrent();
        torrent.setUri("hdfs://user:pass@test.hdfs.com:54310/test/hechaobin01");
        GeneralStatus status = torrent.generateTorrentCode();
        assertEquals(status, GeneralStatus.STATUS_OK);

        URITorrentBaseInfo base = torrent.getBaseInfo();
        byte[] serializeData = SerializeUtil.serialize(base);
        assertTrue(serializeData.length > 0);
        URITorrent anotherTorrent =
            URITorrent.getTorrentFromBaseInfo((URITorrentBaseInfo) SerializeUtil.unserialize(serializeData));
        LOG.info("uri is " + anotherTorrent.getUri());
        LOG.info("org byte size is " + torrent.getTorrentZipCode().length
                + ", new byte size is " + anotherTorrent.getTorrentZipCode().length);
        LOG.info("org uptime is " + torrent.getUpdateTimestamp()
                + ", new uptime is " + anotherTorrent.getUpdateTimestamp());
        assertEquals(torrent.getLastModifyTime(), anotherTorrent.getLastModifyTime());
    }
}
