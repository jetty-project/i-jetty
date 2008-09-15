#!/bin/bash
#
# build.sh
# builds the i-jetty package and the sdcard image.
# Requires that the ANDROID_SDK environment variable be set, or that
# the SDK tools be in PATH.
#
# Usage: ./build.sh

# Setup any environment variables as required

if [ "$VERBOSE" = "on" ]; then
    export VERBOSE_ARGS="-v"
    export UNZIP_ARGS=""
    export BUILD_ARGS=""
else
    export VERBOSE_ARGS=""
    export UNZIP_ARGS="-qq"
    export BUILD_ARGS="-q"
fi
    

if [ -z "$ANDROID_SDK" ]; then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "ANDROID_SDK isn't set."
    echo "Please actually set one of the above correctly, then re-run"
    echo "the tool."
    echo "======================== [ ERROR ] ========================"
    exit 1
else
    # Make sure it ends with a slash.
    if [ ${ANDROID_SDK: -1} != "/" ]; then
        export ANDROID_SDK=${ANDROID_SDK}/
    fi
    
    export ANDROID_SDK_TOOLS=${ANDROID_SDK}tools/
fi

# Create sdcard.img if we don't already have one.
if [ ! -e "sdcard.img" ]; then
    echo -e "\033[1m******* Creating SD card image...\033[0m"
    scripts/create-sdcard.sh
fi

mkdir $VERBOSE_ARGS sdcard-intermediate/
cp sdcard-layout/* sdcard-intermediate/ -Rf
find sdcard-intermediate/ -name '.svn' -type d | xargs rm -r $VERBOSE_ARGS

mvn install:install-file -DgroupId=android -DartifactId=android \
  -Dversion=0.9_beta -Dpackaging=jar \
  -Dfile=$ANDROID_SDK/android.jar $BUILD_ARGS

echo -e "\033[1m******* Building i-jetty...\033[0m"
mvn -v > /dev/null
if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "Maven was not found to be installed on the system (or, at  "
    echo "least, it wasn't in the PATH). Please install it so I can  "
    echo "build i-jetty!"
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

mvn -Dandroid.home=${ANDROID_SDK} $BUILD_ARGS install 
if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "The Maven build failed! Please check the output and try to "
    echo "rectify the problem."
    echo 
    echo "You may want to try running with verbose mode enabled; you "
    echo "do that like so: $ VERBOSE=on ./build.sh"
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

# Heck, why not - build the console!
echo -e "\033[1m******* Building console application...\033[0m"
scripts/build-console.sh
if (( $? )); then
    echo
    echo "======================== [ WARN ] ========================"
    echo "The Maven build for the console application failed! Please"
    echo "check the output and try to rectify the problem."
    echo 
    echo "You may want to try running with verbose mode enabled; you "
    echo "do that like so: $ VERBOSE=on ./deploy.sh"
    echo "======================== [ WARN ] ========================"
    echo 
fi

echo -e "\033[1m******* Building hello application...\033[0m"
scripts/build-hello.sh
if (( $? )); then
    echo
    echo "======================== [ WARN ] ========================"
    echo "The Maven build for the hello application failed! Please"
    echo "check the output and try to rectify the problem."
    echo 
    echo "You may want to try running with verbose mode enabled; you "
    echo "do that like so: $ VERBOSE=on ./deploy.sh"
    echo "======================== [ WARN ] ========================"
    echo 
fi

# Sync up the sdcard folder with the actual image
# (console should be in here already)
echo -e "\033[1m******* Syncing SD card against sdcard-layout directory...\033[0m"
scripts/sync-sdcard.sh

# We *must* unmount our SD card otherwise we crash poor ol' qemu
echo -e "\033[1m******* Unmounting image...\033[0m"
sudo umount sdcard-mount/
rm -Rf $VERBOSE_ARGS sdcard-mount/

# and detatch the loopback device - IMPORTANT!
echo -e "\033[1m******* Freeing loopback device...\033[0m"
sudo losetup -d /dev/loop0 || echo " FAILED!"

# Remove temp dir
rm -Rf $VERBOSE_ARGS sdcard-intermediate/
