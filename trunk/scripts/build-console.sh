#!/bin/bash
#
# build-console.sh
# Builds the console application, and copies data into the sdcard-layout folder.

cd console
mvn -Dandroid.home=${ANDROID_SDK} $BUILD_ARGS install 
if (( $? )); then
    exit 1
fi

# Build seemed OK, copy stuff into sdcard-layout
rm -Rf $VERBOSE_ARGS ../sdcard-intermediate/jetty/webapps/console
mkdir $VERBOSE_ARGS ../sdcard-intermediate/jetty/webapps/console
cp target/console-*.war ../sdcard-intermediate/jetty/webapps/console/

# Unzip for i-jetty (if card is READ-ONLY)
cd ../sdcard-intermediate/jetty/webapps/console/
unzip $UNZIP_ARGS console-*.war
rm console-*.war
