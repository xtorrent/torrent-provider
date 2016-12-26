#!/bin/sh
cd `dirname $0` || exit 1
pwd
./thrift -r -gen java -out ../ TorrentProvider.thrift
