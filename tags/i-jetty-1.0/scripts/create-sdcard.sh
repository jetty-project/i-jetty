#!/bin/bash
#
# create-sdcard.sh
# Creates a new small sdcard.img file, for use as an SD card.
#
# Usage: ./create-sdcard.sh [image-size-in-MB]

DEFAULT_IMAGE_SIZE_MB=60
DEFAULT_IMAGE_SIZE=$(($DEFAULT_IMAGE_SIZE_MB * 1024 * 1024))

IMAGE_SIZE=0

if [ -e "sdcard.img" ]; then
    echo "sdcard.img already exists!"
    echo "Delete it if you wish to create a new one."
    exit 1
fi


if [ -z "$1" ]; then
    echo "Defaulting image size to ${DEFAULT_IMAGE_SIZE_MB} MB."
    IMAGE_SIZE=${DEFAULT_IMAGE_SIZE}
else
    IMAGE_SIZE=$(($1 * 1024 * 1024))
fi

${ANDROID_SDK_TOOLS}mksdcard $IMAGE_SIZE sdcard.img
