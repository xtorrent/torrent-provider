package com.gko3.torrentprovider.bean;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * Unit test for ProcessKeeper
 */
public class ProcessKeeperTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(ProcessKeeperTest.class);
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAll() {
        ProcessKeeper keeper = ProcessKeeper.getInstance();
        String key1 = "abc";
        String key2 = "abccdf";
        String key3 = "abcnnn";
        keeper.addKey(key1);
        keeper.addKey(key2);
        keeper.addKey(key3);
        assertTrue(keeper.isExists(key1));
        assertTrue(keeper.isExists(key2));

        keeper.removeKey(key1);
        assertTrue(!keeper.isExists(key1));
    }
}
