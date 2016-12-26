#!/bin/sh

PROJECT=torrentClientTest

DATE=`date +%Y%m%d-%H%M`
#echo ${DATE} ${PROJECT}  "begin!"

WORK_DIR=`dirname $0`
WORK_DIR=`cd ${WORK_DIR}; cd ..; pwd`
#echo "Work dir:"${WORK_DIR}

cd ${WORK_DIR}

LOGS_DIR=${WORK_DIR}/client_log

unset _JAVA_OPTIONS

#echo "Setting up environment variable..."
JAVA_HOME=${WORK_DIR}/java6
LIB_DIR=${WORK_DIR}/lib
CONF_DIR=${WORK_DIR}/conf

if [ ! -d ${LOGS_DIR} ];then
    mkdir -p ${LOGS_DIR}
fi

MAIN_CLASS="com.gko3.torrentprovider.client.TestTorrentProviderClient"
#echo "Main class:"${MAIN_CLASS}

CLASSPATH=.:${WORK_DIR}/target/torrentProvider.jar.jar:${CONF_DIR}:${LIB_DIR}/*
JAVA_ARGS=" -XX:SurvivorRatio=8 -XX:PermSize=512m -XX:MaxPermSize=512m -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSClassUnloadingEnabled -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=80 -XX:+UseCMSCompactAtFullCollection -XX:CMSFullGCsBeforeCompaction=0 -XX:-CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:MaxTenuringThreshold=7 -Xloggc:${LOGS_DIR}/${PROJECT}-gc.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintHeapAtGC -cp ${CLASSPATH}"

if [ -f ${LOGS_DIR}/gc.log ];then
    mv ${LOGS_DIR}/gc.log ${LOGS_DIR}/gc.log.${DATE}
fi

PROGRAM_PARAMS="hdfs://user:password@test.hadoop.com:54310/test/path/to/file localhost 9099 fcaea518a8099df0563c4d11524dd1304f62f178"
${JAVA_HOME}/bin/java ${JAVA_ARGS} ${MAIN_CLASS} ${PROGRAM_PARAMS} >/dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "torrent_provider_monitor:0"
else
    echo "torrent_provider_monitor:1"
fi

exit 0
