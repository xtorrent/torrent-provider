package com.gko3.torrentprovider.common;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.xerial.snappy.Snappy;

import com.gko3.torrentprovider.exception.SnappyException;

/**
 * this class is used to compress and uncompress bytes by snappy lib
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public final class SnappyTool {
    private static final Logger LOG = Logger.getLogger(SnappyTool.class);

    /**
     * compress bytes
     *
     * @param input uncompress bytes
     * @return compress bytes
     * @throws SnappyException when compress fail
     */
    public static byte[] compress(byte[] input) throws SnappyException {
        if (input == null) {
            return input;
        }

        try {
            byte[] output = Snappy.compress(input);
            LOG.debug("compress file from " + input.length + "-->"
                    + output.length + ",compressed:"
                    + ((double) 100 * (output.length - input.length)) / output.length);
            return output;
        } catch (IOException e) {
            LOG.error("snappy compress fail", e);
            throw new SnappyException(SnappyException.ErrorCode.SNAPPY_COMPRESS_FAIL);
        }
    }

    /**
     * uncompress bytes
     *
     * @param input compress bytes
     * @return uncompress bytes
     * @throws SnappyException when uncompress bytes fail
     */
    public static byte[] uncompress(byte[] input) throws SnappyException {
        try {
            byte[] output = Snappy.uncompress(input);
            LOG.debug("uncompress file from" + input.length + "-->"
                    + output.length + ",compressed:"
                    + ((double) 100 * (output.length - input.length)) / output.length);
            return output;
        } catch (IOException e) {
            LOG.error("snappy uncompress fail" + e);
            throw new SnappyException(SnappyException.ErrorCode.SNAPPY_COMPRESS_FAIL);
        }
    }

    /**
     * uncompress byte range
     *
     * @param input compress bytes
     * @param offset byte start
     * @param length bytes length
     * @return uncompress bytes
     * @throws SnappyException when uncompress bytes fail
     */
    public static byte[] uncompress(byte[] input, int offset, int length) throws SnappyException {
        try {
            byte[] output = new byte[Snappy.uncompressedLength(input, offset, length)];
            Snappy.uncompress(input, offset, length, output, 0);
            return output;
        } catch (IOException e) {
            LOG.error("snappy uncompress fail" + e);
            throw new SnappyException(SnappyException.ErrorCode.SNAPPY_COMPRESS_FAIL);
        }
    }

    /**
     * uncompress of byte buffer
     *
     * @param byteBuffer compress byte buffer
     * @return uncompress bytes
     * @throws SnappyException when uncompress bytes fail
     */
    public static byte[] uncompress(ByteBuffer byteBuffer) throws SnappyException {
        return uncompress(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit() - byteBuffer.position());
    }
}
