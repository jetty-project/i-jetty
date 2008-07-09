#!/bin/bash
#
# deploy.sh
# Deploys the i-jetty package to the emulator.
# Requires that the ANDROID_SDK environment variable be set, or that
# the SDK tools be in PATH.
#
# Usage: ./deploy.sh

# Setup any environment variables as required
if [ -z "$ANDROID_SDK" ]; then
    aapt v
    if (( $? )); then
        echo
        echo "======================== [ ERROR ] ========================"
        echo "SDK not found in PATH, and ANDROID_SDK isn't set."
        echo "Please actually set one of the above correctly, then re-run"
        echo "the tool."
        echo "======================== [ ERROR ] ========================"
        exit 1
    else
        export ANDROID_SDK=""
    fi
else
    # Make sure it ends with a slash.
    if [ ${ANDROID_SDK: -1} != "/" ]; then
        export ANDROID_SDK=${ANDROID_SDK}/
    fi
    
    export ANDROID_SDK_TOOLS=${ANDROID_SDK}tools/
fi

# Create sdcard.img if we don't already have one.
if [ ! -a "sdcard.img" ]; then
    echo "Creating SD card image..."
    ./create-sdcard.sh
fi

echo "Buidling i-jetty..."
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

mvn -Dandroid.home=${ANDROID_SDK} clean install 
if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "The Maven build failed! Please check the output and try to "
    echo "rectify the problem."
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

# Heck, why not - build the console!
./build-console.sh
if (( $? )); then
    echo
    echo "======================== [ WARN ] ========================"
    echo "The Maven build for the console application failed! Please"
    echo "check the output and try to rectify the problem."
    echo "======================== [ WARN ] ========================"
    echo 
fi

# Sync up the sdcard folder with the actual image
# (console should be in here already)
echo "Syncing SD card against sdcard-layout directory..."
./sync-sdcard.sh

# We *must* unmount our SD card otherwise we crash poor ol' qemu
sudo umount sdcard-mount

# and free the loopback device - IMPORTANT!
sudo losetup -f /dev/loop1

# Start ze emulator! (in the background)
echo "Starting emulator..."
${ANDROID_SDK_TOOLS}emulator -sdcard sdcard.img &

if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "Failed to start emulator. "
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

# Wait for it to start
${ANDROID_SDK_TOOLS}adb wait-for-device

# Upload the package to the emulator when it's started
echo "Uploading i-jetty..."
${ANDROID_SDK_TOOLS}adb install target/i-jetty.apk

if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "Failed to upload i-jetty to the device."
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

echo "Done!"
