#!/bin/bash
#
# create-sdcard.sh
# Creates a new small sdcard.img file, for use as an SD card.
#
# Usage: ./create-sdcard.sh filename.img [image-size-in-MB]

DEFAULT_IMAGE_SIZE_MB=60
DEFAULT_IMAGE_SIZE=$(($DEFAULT_IMAGE_SIZE_MB * 1024 * 1024))

IMAGE_SIZE=0

if [ -e "$1" ]; then
    echo "$1 already exists!"
    echo "Delete it if you wish to create a new one."
    exit 1
fi


if [ -z "$2" ]; then
    echo "Defaulting image size to ${DEFAULT_IMAGE_SIZE_MB} MB."
    IMAGE_SIZE=${DEFAULT_IMAGE_SIZE}
else
    IMAGE_SIZE=$(($2 * 1024 * 1024))
fi

$ANDROID_HOME/tools/mksdcard $IMAGE_SIZE $1
