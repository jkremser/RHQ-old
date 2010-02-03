#!/bin/sh

if [ -z $1 ]; then
    echo "Usage: $0 xml-file-name "
    echo "example: $0 /tmp/rhel-i386-server-vt-5-package-metadata.xml"
    exit 1
fi

mvn -e exec:java -Dexec.mainClass="org.rhq.enterprise.server.plugins.rhnhosted.xmlrpc.ParsePackageMetadataTool" -Drhn.xml_file_name=$1 -Dexec.args="" -Dlog4j.configuration="file://${PWD}/src/test/resources/test-log4j.xml"
