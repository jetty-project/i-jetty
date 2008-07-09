#!/bin/bash
#
# sync-sdcard.sh
# Mounts sdcard.img, copies across a directory structure, and umounts.

if [ ! -e "sdcard.img" ]; then
    echo "sdcard.img does not exist!"
    echo "Please run ./create-sdcard.sh to generate it."
    exit 1
fi

if [ `mount -l | grep "sdcard-mount"` == ""]; then
    # Looks OK to mount.
    echo "SD image doesn't seem to have been mounted yet."
    echo "Mounting image..."
    
    # NOTE: Requires root permissions! :(
    echo "This step requires root permissions to setup the"
    echo "loopback device and mount the image."
    
    sudo losetup /dev/loop1 sdcard.img
    if (( $? )); then
        echo "Failed to setup loopback device."
        # Not being able to setup loopback is not fatal.
        # It may mean we've already been setup.
    fi
    
    mkdir sdcard-mount
    sudo mount /dev/loop1 sdcard-mount
    if (( $? )); then
        echo "Failed to mount image."
        exit 1
    fi
    
    echo "Syncing directories..."
else
    echo "SD image already mounted; only syncing directories..."
fi

# Nuke the entire SD card so we have a clean slate.
sudo rm sdcard-mount/* -Rf

# Copy it all in (again, root, eugh)
sudo cp -Rvf sdcard-layout/* sdcard-mount/
