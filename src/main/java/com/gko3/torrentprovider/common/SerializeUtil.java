package com.gko3.torrentprovider.common;

import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * serialize util for serializtion
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class SerializeUtil {
    private static final Logger LOG = Logger.getLogger(SerializeUtil.class);

    /**
     * serialize an object to bytes
     *
     * @param object object to serialize
     * @return serialize bytes of this object
     */
    public static byte[] serialize(java.lang.Object object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            oos.flush();
            byte[] bytes = baos.toByteArray();
            baos.close();
            oos.close();
            return bytes;
        } catch (Exception e) {
            LOG.warn("serialize error:" + e);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * unserialize an object from bytes
     *
     * @param bytes for unserialize
     * @return object for these bytes
     */
    public static java.lang.Object unserialize(byte[] bytes) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            java.lang.Object obj =  ois.readObject();
            bais.close();
            ois.close();
            return obj;
        } catch (Exception e) {
            LOG.warn("unserialize error:" + e);
        }
        return null;
    }
}
