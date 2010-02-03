#!/bin/sh

echo "$# args passed"

if [ $# -lt 2 ]; then
    echo "Usage: $0 channel-name chunk-size [save-file-name]"
    exit 1
fi

CHUNK_SIZE=100
if [ -n $2 ]; then
    CHUNK_SIZE=$2
fi
echo "Setting CHUNK_SIZE to ${CHUNK_SIZE}"


if [ -n $3 ]; then
    SAVE_FILE=$3
    echo "Setting SAVE_FILE to ${SAVE_FILE}"
fi

mvn -e exec:java -Dexec.mainClass="org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.DownloadPackageMetadataTool" -Drhn.channel=$1 -Drhn.save.file.path=${SAVE_FILE} -Drhn.chunk.size=${CHUNK_SIZE} -Dexec.args="" -Dlog4j.configuration="file://${PWD}/src/test/resources/test-log4j.xml"
