#!/bin/bash
#
# reset-sdcard-layout.sh
# Resets the sdcard-layout folder.

cd sdcard-layout
cd jetty
cd webapps
rm * -Rf
cd ..
cd etc

