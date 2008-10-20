#!/bin/bash
#
# sync-sdcard.sh
# Mounts sdcard.img, copies across a directory structure, and umounts.

if [ ! -e "sdcard.img" ]; then
    echo "sdcard.img does not exist!"
    echo "Please run ./create-sdcard.sh to generate it."
    exit 1
fi

if [ -d sdcard-intermediate ]; then
  echo "Dir sdcard-intermediate exists"
else
  mkdir sdcard-intermediate
  echo "Dir sdcard-intermediate created"
fi



if [ `mount -l | grep "sdcard-mount"` == ""]; then
    # Looks OK to mount.
    echo "SD image doesn't seem to have been mounted yet."
    echo "Mounting image..."

    
    if [ -d sdcard-mount ]; then
      echo "Dir sdcard-mount exists"
    else
      mkdir sdcard-mount
      echo "Dir sdcard-mount created"
    fi

    # NOTE: Requires root permissions! :(
    echo "This step requires root permissions to setup the"
    echo "loopback device and mount the image."
    
    sudo losetup /dev/loop0 sdcard.img
    if (( $? )); then
        echo "Failed to setup loopback device."
        # Not being able to setup loopback is not fatal.
        # It may mean we've already been setup.
    fi
    
    sudo mount /dev/loop0 sdcard-mount
    if (( $? )); then
        echo "Failed to mount image."
        exit 1
    fi
    
    echo "Syncing directories..."
else
    echo "SD image already mounted; only syncing directories..."
fi

# Nuke the entire SD card so we have a clean slate.
sudo rm sdcard-mount/* -Rf $VERBOSE_ARGS

# Copy it all in (again, root, eugh) minus .svn directories
sudo cp -Rf $VERBOSE_ARGS sdcard-intermediate/* sdcard-mount/

# We *must* unmount our SD card otherwise we crash poor ol' qemu
echo -e "\033[1m******* Unmounting image...\033[0m"
sudo umount sdcard-mount/
rm -Rf $VERBOSE_ARGS sdcard-mount/

# and detatch the loopback device - IMPORTANT!
echo -e "\033[1m******* Freeing loopback device...\033[0m"
sudo losetup -d /dev/loop0 || echo " FAILED!"
