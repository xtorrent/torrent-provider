package com.gko3.torrentprovider.bean;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * Unit test for URIProcessQueue
 */
public class URIProcessQueueTest extends TestCase {
    private static final Logger LOG = Logger.getLogger(URIProcessQueueTest.class);
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAll() {
        URIProcessQueue queue = URIProcessQueue.getInstance();
        String key1 = "abc";
        String key2 = "abccdf";
        String key3 = "abcnnn";
        queue.put(key1);
        queue.put(key2);
        queue.put(key3);
        try {
            String result = queue.take();
            LOG.info("result is " + result + ", and key1 is " + key1);
            assertEquals(result, key1);
        } catch (InterruptedException e) {
            LOG.warn(e);
            assertTrue(false);
        }
    }
}
