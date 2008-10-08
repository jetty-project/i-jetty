#!/bin/bash
#
# deploy.sh
# Deploys the i-jetty package to the emulator.
# Requires that the ANDROID_SDK environment variable be set, or that
# the SDK tools be in PATH.
#
# Usage: ./deploy.sh

# Setup any environment variables as required

if [ "$VERBOSE" = "on" ]; then
    export VERBOSE_ARGS="-v"
    export UNZIP_ARGS=""
else
    export VERBOSE_ARGS=""
    export UNZIP_ARGS="-qq"
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


# Start ze emulator! (in the background)
echo -e "\033[1m******* Starting emulator...\033[0m"
${ANDROID_SDK_TOOLS}emulator ${1}  -sdcard sdcard.img &

if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "Failed to start emulator. "
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

exit

# Wait for it to start
${ANDROID_SDK_TOOLS}adb wait-for-bootloader

# Upload the package to the emulator when it's started
echo -e "\033[1m******* Uploading i-jetty to emulator...\033[0m"
${ANDROID_SDK_TOOLS}adb install target/i-jetty-debug.apk

if (( $? )); then
    echo
    echo "======================== [ ERROR ] ========================"
    echo "Failed to upload i-jetty to the device."
    echo "======================== [ ERROR ] ========================"
    exit 1
fi

# Setup port forwarding so we can check in our browser instead of only the phone
echo -e "\033[1m******* Setting up port forwarding...\033[0m"
${ANDROID_SDK_TOOLS}adb forward tcp:8888 tcp:8080

unset $VERBOSE_ARGS UNZIP_ARGS
unset $VERBOSE_ARGS VERBOSE_ARGS

echo -e "\033[1m******* Done!\033[0m"
