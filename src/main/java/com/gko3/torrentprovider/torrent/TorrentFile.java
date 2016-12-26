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

import java.util.ArrayList;
import java.util.Date;

/**
 * Representation of a torrent file
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class TorrentFile {

    public String announceURL;
    public String comment;
    public String createdBy;
    public long creationDate;
    public String encoding;
    public String saveAs;
    public String parentPath;
    public int pieceLength;

    /* In case of multiple files torrent, saveAs is the name of a directory
     * and name contains the path of the file to be saved in this directory
     */
    public ArrayList name;
    public ArrayList path;
    public ArrayList length;
    public ArrayList pathMode;
    public boolean singleFileNeedSetPath;

    public byte[] infohashAsBinary;
    public String infohashAsHex;
    public String infohashAsUrl;
    public long totalLength;

    public ArrayList pieceHashValuesAsBinary;
    public ArrayList pieceHashValuesAsHex;
    public ArrayList pieceHashValuesAsUrl;

    /**
     * Create the TorrentFile object and initiate its instance variables
     */
    public TorrentFile() {
        super();
        announceURL = new String();
        comment = new String();
        createdBy = new String();
        encoding = new String();
        saveAs = new String();
        parentPath = new String();
        creationDate = -1;
        totalLength = -1;
        pieceLength = -1;

        path = new ArrayList();
        pathMode = new ArrayList();
        name = new ArrayList();
        length = new ArrayList();
        singleFileNeedSetPath = false;

        pieceHashValuesAsBinary = new ArrayList();
        pieceHashValuesAsUrl = new ArrayList();
        pieceHashValuesAsHex = new ArrayList();
        infohashAsBinary = new byte[20];
        infohashAsUrl = new String();
        infohashAsHex = new String();
    }
}
