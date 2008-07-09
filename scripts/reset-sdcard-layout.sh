#!/bin/bash
#
# reset-sdcard-layout.sh
# Resets the sdcard-layout folder.

# FIXME: Probably helps to make it clean a bit more (ie, check for other dirs)
cd sdcard-layout
cd jetty
cd webapps
rm * -Rf
cd ..
cd etc

