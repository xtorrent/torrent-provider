/*
 * Java Bittorrent API as its name indicates is a JAVA API that implements the Bittorrent Protocol
 * This project contains two packages:
 * 1. jBittorrentAPI is the "client" part, i.e. it implements all classes needed to publish
 *    files, share them and download them.
 *    This package also contains example classes on how a developer could create new applications.
 * 2. trackerBT is the "tracker" part, i.e. it implements a all classes needed to run
 *    a Bittorrent tracker that coordinates peers exchanges. *
 *
 * Copyright (C) 2007 Baptiste Dubuis, Artificial Intelligence Laboratory, EPFL
 *
 * This file is part of jbittorrentapi-v1.0.zip
 *
 * Java Bittorrent API is free software and a free user study set-up;
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Java Bittorrent API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Bittorrent API; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @version 1.0
 * @author Baptiste Dubuis
 * To contact the author:
 * email: baptiste.dubuis@gmail.com
 *
 * More information about Java Bittorrent API:
 *    http://sourceforge.net/projects/bitext/
 */

package com.gko3.torrentprovider.torrent;

import java.nio.charset.Charset;

/**
 * Some useful (or not...) constants used (or not yet...) throughout the program
 */
public class Constants {
    public static final String DEFAULT_ENCODING = "UTF8";
    public static final String BYTE_ENCODING = "ISO-8859-1";
    public static Charset BYTE_CHARSET;
    public static Charset DEFAULT_CHARSET;

    static {
        try {

            BYTE_CHARSET = Charset.forName(Constants.BYTE_ENCODING);
            DEFAULT_CHARSET = Charset.forName(Constants.DEFAULT_ENCODING);

        } catch (Throwable e) {

            e.printStackTrace();
        }
    }

    public static final String CLIENT = "jBittorrentAPI 1.0";
    public static String SAVEPATH = "downloads/";

    public static final String OS_NAME = System.getProperty("os.name");

    public static final boolean IS_OSX = OS_NAME.toLowerCase().startsWith(
            "mac os");
    public static final boolean IS_LINUX = OS_NAME.equalsIgnoreCase("Linux");
    public static final boolean IS_SOLARIS = OS_NAME.equalsIgnoreCase("SunOS");
    public static final boolean IS_FREEBSD = OS_NAME.equalsIgnoreCase("FreeBSD");
    public static final boolean IS_WINDOWSXP = OS_NAME.equalsIgnoreCase(
            "Windows XP");
    public static final boolean IS_WINDOWS95 = OS_NAME.equalsIgnoreCase(
            "Windows 95");
    public static final boolean IS_WINDOWS98 = OS_NAME.equalsIgnoreCase(
            "Windows 98");
    public static final boolean IS_WINDOWSME = OS_NAME.equalsIgnoreCase(
            "Windows ME");
    public static final boolean IS_WINDOWS9598ME = IS_WINDOWS95 || IS_WINDOWS98 || IS_WINDOWSME;
    public static final boolean IS_WINDOWS = OS_NAME.toLowerCase().startsWith(
            "windows");
    public static final String JAVA_VERSION = System.getProperty("java.version");
}
