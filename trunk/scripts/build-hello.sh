#!/bin/bash
#
# build-hello.sh
# Builds the hello application, and copies data into the sdcard-layout folder.

cd hello
mvn -Dandroid.home=${ANDROID_SDK} clean install 
if (( $? )); then
    exit 1
fi

# Build seemed OK, copy stuff into sdcard-layout
rm -Rvf ../sdcard-layout/jetty/webapps/hello
mkdir -v ../sdcard-layout/jetty/webapps/hello
cp target/hello-*.war ../sdcard-layout/jetty/webapps/hello/

# Now, copy the dexified libs (in a jar) so we can load it!
mkdir -vp ../sdcard-layout/jetty/webapps/hello/WEB-INF/libs
cp target/hello.jar ../sdcard-layout/jetty/webapps/hello/WEB-INF/libs/

# Unzip for i-jetty (if card is READ-ONLY)
cd ../sdcard-layout/jetty/webapps/hello/
unzip hello-*.war
rm hello-*.war
