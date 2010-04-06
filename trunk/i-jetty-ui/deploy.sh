#!/bin/bash

DEVICEID=$1

if [ -z $DEVICEID ] ; then
    echo "ERROR: Must provide device id"
    echo "Usage: $0 <deviceid>"
    echo ""
    adb devices
    exit 1
fi

echo ""
echo "Uninstalling any existing ijetty ..."
adb -s $DEVICEID uninstall org.mortbay.ijetty

echo ""
echo "Installing new ijetty ..."
adb -s $DEVICEID install target/i-jetty-*.apk

echo ""
echo "Starting ijetty ..."
adb -s $DEVICEID shell am start -a android.intent.action.MAIN \
 -c android.intent.category.LAUNCHER \
 -n org.mortbay.ijetty.IJetty

