#!/bin/bash
#
# cleanup.sh
# Cleans up the checkout. Useful for testing before release.

# FIXME: Probably helps to make it clean a bit more (ie, check for other dirs)

mvn clean
cd console
mvn clean
cd ..
cd hello
mvn clean
cd ..

cd sdcard-layout
cd jetty
cd webapps
rm * -Rf
cd ..
cd etc

