#!/bin/bash
#
#
# deploy.sh
#  
# A convenience script to install the i-jetty apk to
# connected devices.
#

DEVICEID=$1

if [ -z $DEVICEID ] ; then
    echo "ERROR: Must provide device id"
    echo "Usage: $0 <deviceid>"
    echo ""
    adb devices
    exit 1
fi

echo ""
echo "Looking for installed ijetty ..."
adb -s $DEVICEID shell pm list packages | grep ijetty
if [ $? == 0 ] ; then
  echo "Uninstalling existing ijetty ..."
  adb -s $DEVICEID uninstall org.mortbay.ijetty
fi

echo ""
echo "Installing new ijetty ..."
APKFILE=`ls -1 target/i-jetty-*.apk | head -1`
adb -s $DEVICEID install $APKFILE

echo ""
echo "Starting ijetty ..."
adb -s $DEVICEID shell am start -a android.intent.action.MAIN \
 -c android.intent.category.LAUNCHER \
 -n org.mortbay.ijetty/.IJetty

