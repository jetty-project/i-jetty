#!/bin/sh
#
# Deploys the i-jetty package to the emulator.
# Requires that the ANDROID_SDK environment variable be set, or that
# the SDK tools be in PATH.

if [ -z "$ANDROID_SDK" ]; then
    adb install target/i-jetty.apk
    if (( $? )); then
        echo "SDK not found in PATH, and ANDROID_SDK isn't set."
    fi
else
    ${ANDROID_SDK}/tools/adb install target/i-jetty.apk
fi
