#!/bin/bash
#
# relase torrent-provider @hechaobin01
#

set -eu
set errexit
set unset

Prompt(){
	echo "[$(date '+%D %H:%M:%S')] $@"
}

WORK_DIR=$(cd $(dirname $0); pwd)
Prompt "Work dir: $WORK_DIR"
cd $WORK_DIR

Prompt "Generate thrift java files ..."
chmod +x ${WORK_DIR}/src/main/java/protocol/thrift
sh ${WORK_DIR}/src/main/java/protocol/generateProtocol.sh

Prompt "Setting up environment variables ..."
mkdir -p opbin

pushd /tmp/
# install java 8u144
if [ ! -e /tmp/java ]
then
    curl -L -C - -b "oraclelicense=accept-securebackup-cookie" -O http://download.oracle.com/otn-pub/java/jdk/8u144-b01/090f390dda5b47b9b721c7dfaa008135/jdk-8u144-linux-x64.tar.gz
    tar xzf jdk-8u144-linux-x64.tar.gz
    ln -sf jdk1.8.0_144 java
fi
popd

export JAVA_HOME=/tmp/java
export PATH=/usr/local/apache-maven-3.2.5/bin:$JAVA_HOME/bin:$PATH

rm -rf $WORK_DIR/target/*
rm -rf $WORK_DIR/gko3-provider
mkdir -p $WORK_DIR/gko3-provider/lib

Prompt "Clean and Package"
mvn clean assembly:assembly -e -Dmaven.test.skip=true || exit $?

Prompt "Copy files ..."
mkdir -p $WORK_DIR/gko3-provider/opbin
mkdir -p $WORK_DIR/gko3-provider/log
mkdir -p $WORK_DIR/gko3-provider/data
mkdir -p $WORK_DIR/gko3-provider/leveldb
cp -r $WORK_DIR/bin $WORK_DIR/gko3-provider
cp $WORK_DIR/gko3-provider/bin/clientTest.sh $WORK_DIR/gko3-provider/opbin/service_monitor.sh

chmod +x $WORK_DIR/gko3-provider/bin/*
chmod +x $WORK_DIR/gko3-provider/opbin/*

TARGET_OUTPUT=$WORK_DIR/target/torrentProvider-all/output/
cp -r $TARGET_OUTPUT/* $WORK_DIR/gko3-provider

Prompt "Generate verion and timestamp ..."
echo $(date -d  today +%Y%m%d%H%M%S) > $WORK_DIR/gko3-provider/version

cd $WORK_DIR/ && tar -czf gko3-provider.tgz gko3-provider

Prompt "Build success ..."
exit $?
