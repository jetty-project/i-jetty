#!/bin/sh
#
# Quick script that does a clean compile and install.
# To be run from project root.
#
# Environment variables:
#   $BUILD_ONLY - give a value to supress running the emulator
#   $SDCARD - location to sdcard image
#   $ANDROID_HOME - location to Android SDK
#   $CLEAN - set to 'clean' to clean then build, empty otherwise.

set -e

VERSION="1.2"

if [ ! $ANDROID_HOME ]; then ANDROID_HOME=/home/alex/Sources/android-sdk-linux_x86-1.0_r1 ; fi
if [ ! $SDCARD ]; then SDCARD=sdcard.img ; fi
if [ ! $CLEAN ]; then CLEAN="clean"; else CLEAN=""; fi

if [ ! -d $ANDROID_HOME ]; then
    echo "Error: Bad Android home directory; does not exist, or not directory."
    echo "Override with ANDROID_HOME environment variable. Currently: $ANDROID_HOME"
    exit 1
fi

# Create sdcard if it doesn't exist
if [ ! -f $SDCARD ]; then
    ANDROID_HOME=$ANDROID_HOME sh scripts/create-sdcard.sh $SDCARD
fi

# Build it
echo "i-jetty $VERSION is compiling. http://xkcd.com/303/"; sleep 1
mvn $CLEAN install -Dandroid.home=$ANDROID_HOME

if [ ! $BUILD_ONLY ]; then
    # Run the emulator
    $ANDROID_HOME/tools/emulator -sdcard $SDCARD &

    # Wait a bit, then upload the package
    sleep 30
    $ANDROID_HOME/tools/adb install -r modules/i-jetty/target/i-jetty-debug-$VERSION.apk

    # Forward ports
    $ANDROID_HOME/tools/adb forward tcp:8888 tcp:8080
fi
