package com.gko3.torrentprovider.common;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/**
 * this class is used to get an property has the order as user input
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class OrderedProperties extends Properties {

    private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    public Enumeration<Object> keys() {
        return Collections.<Object>enumeration(keys);
    }

    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }

    public Set<Object> keySet() {
        return keys;
    }

    public Set<String> stringPropertyNames() {
        Set<String> set = new LinkedHashSet<String>();
        for (Object key : this.keys) {
            set.add((String) key);
        }
        return set;
    }

    @Override
    public String toString() {
        String result = "";
        for (String key : stringPropertyNames()) {
            result = result + key + " : " + get(key) + "\n";
        }
        return result;
    }
}
