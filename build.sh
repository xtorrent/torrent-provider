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

JAVA_BASE_PATH=/opt/usr/local

Prompt "Generate thrift java files ..."
chmod +x ${WORK_DIR}/src/main/java/protocol/thrift
sh ${WORK_DIR}/src/main/java/protocol/generateProtocol.sh

Prompt "Setting up environment variables ..."
mkdir -p opbin

export JAVA_HOME=$JAVA_BASE_PATH/jdk1.6.0_45/
export PATH=$JAVA_BASE_PATH/apache-maven-3.2.5/bin:$JAVA_HOME/bin:$PATH

rm -rf $WORK_DIR/target/*
rm -rf $WORK_DIR/output
mkdir -p $WORK_DIR/output/lib

Prompt "Clean and Package"
mvn clean package -e -Dmaven.test.skip=true || exit $?
#mvn clean package || exit $?

Prompt "Copy files ..."
mkdir -p $WORK_DIR/output/opbin
mkdir -p $WORK_DIR/output/log
mkdir -p $WORK_DIR/output/data
cp -r $JAVA_HOME/jre $WORK_DIR/output/java6
cp -r $WORK_DIR/bin $WORK_DIR/output
cp $WORK_DIR/output/bin/clientTest.sh $WORK_DIR/output/opbin/service_monitor.sh

chmod +x $WORK_DIR/output/bin/*
chmod +x $WORK_DIR/output/opbin/*

TARGET_OUTPUT=$WORK_DIR/target/torrentProvider-all/output/
cp -r $TARGET_OUTPUT/* $WORK_DIR/output
cp -r $WORK_DIR/lib/org/fusesource/leveldbjni/leveldbjni-all/1.7.1/leveldbjni-all-1.7.1.jar $WORK_DIR/output/lib/

Prompt "Generate verion and timestamp ..."
echo $(date -d  today +%Y%m%d%H%M%S) > $WORK_DIR/output/version

cd $WORK_DIR/output && tar -czf output.tar.gz *

Prompt "Build success ..."
exit $?
