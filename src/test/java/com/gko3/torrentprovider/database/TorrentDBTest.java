package com.gko3.torrentprovider.database;

import com.gko3.torrentprovider.core.TorrentInfo;
import com.gko3.torrentprovider.thrift.GeneralResponse;
import com.gko3.torrentprovider.thrift.InfohashTorrent;
import com.gko3.torrentprovider.thrift.TorrentStatus;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * Unit test for TorrentDB
 */
public class TorrentDBTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(TorrentDBTest.class);

    protected void setUp() throws Exception {
        TorrentDB.getInstance().init();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testUploadTorrent() {
        InfohashTorrent info = new InfohashTorrent();
        info.setInfohash("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        info.setSource("gko3://db-oped-dev01.db01/home/users/hechaobin01/tmp/dt-agent.proto");
        byte[] code = new byte[10];
        for (int i = 0; i < 10; ++i) {
            code[i] = 'a';
        }
        info.setTorrentZipCode(code);

        GeneralResponse response = TorrentDB.getInstance().uploadTorrent(info);
        assertEquals(0, response.getRetCode());
        assertEquals("OK", response.getMessage());

        info.setInfohash("invalid_infohash");
        response = TorrentDB.getInstance().uploadTorrent(info);
        assertEquals(1, response.getRetCode());
    }

    public void testGetTorrentByInfohash() {
        String infohash = new String("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        try {
            TorrentInfo info = TorrentDB.getInstance().getTorrentByInfohash(infohash);
            assertEquals(infohash, info.getInfohash());
            assertEquals(info.getSource(), "gko3://db-oped-dev01.db01/home/users/hechaobin01/tmp/dt-agent.proto");
            assertNotNull(info.getTorrentCode());
            assertEquals(TorrentStatus.STATUS_OK, info.getTorrentStatus());
            assertEquals(10, info.getTorrentCode().length);

            info = TorrentDB.getInstance().getTorrentByInfohash("invalid_infohash");
            assertEquals(TorrentStatus.STATUS_ERROR, info.getTorrentStatus());
        } catch (SQLException e) {
            assertTrue(false);
        }
    }

}
