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
mkdir -v ../sdcard-layout/jetty/console
cp target/console-*.war ../sdcard-layout/jetty/console/

# Unzip for i-jetty.
# XXX: Do we need this? TEST FIRST!
cd ../sdcard-layout/jetty/console/
unzip console-*.war
rm console-*.war
