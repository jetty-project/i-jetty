#!/bin/bash
#
# build-console.sh
# Builds the console application, and copies data into the sdcard-layout folder.

cd console
mvn -Dandroid.home=${ANDROID_SDK} clean install 
if (( $? )); then
    exit 1
fi

# Build seemed OK, copy stuff into sdcard-layout
rm -Rvf ../sdcard-layout/jetty/webapps/console
mkdir -v ../sdcard-layout/jetty/webapps/console
cp target/console-*.war ../sdcard-layout/jetty/webapps/console/

# Unzip for i-jetty (if card is READ-ONLY)
cd ../sdcard-layout/jetty/webapps/console/
unzip console-*.war
rm console-*.war
