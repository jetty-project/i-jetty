#!/bin/bash
#
# build-hello.sh
# Builds the hello application, and copies data into the sdcard-layout folder.

cd hello
mvn -Dandroid.home=${ANDROID_SDK} $BUILD_ARGS install 
if (( $? )); then
    exit 1
fi

# Build seemed OK, copy stuff into sdcard-layout
rm -Rf $VERBOSE_ARGS ../sdcard-intermediate/jetty/webapps/hello
mkdir $VERBOSE_ARGS ../sdcard-intermediate/jetty/webapps/hello
cp target/hello-*.war ../sdcard-intermediate/jetty/webapps/hello/

# Now, copy the dexified libs (in a jar) so we can load it!
#mkdir -vp ../sdcard-intermediate/jetty/webapps/hello/WEB-INF/libs
#cp target/hello.jar ../sdcard-intermediate/jetty/webapps/hello/WEB-INF/libs/

# Unzip for i-jetty (if card is READ-ONLY)
cd ../sdcard-intermediate/jetty/webapps/hello/
unzip $UNZIP_ARGS hello-*.war
rm hello-*.war
