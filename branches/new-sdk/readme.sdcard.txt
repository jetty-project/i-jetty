********************************************************************************
    Note: it's generally easier if you get the deploy and helper scripts to
    manage the sdcard image for you (see readme.txt).
********************************************************************************

How to Add Files to removable storage device
============================================
(Debian instructions)
mksdcard 2147483648 ~/src/i-jetty/sdcard.img
sudo losetup /dev/loop0 ~/src/i-jetty/sdcard.img
mkdir ~/src/i-jetty/sdcard
sudo mount /dev/loop0 ~/src/i-jetty/sdcard
gksudo nautilus ~/src/i-jetty/sdcard

How to Start Emulator with removable storage device
===================================================
emulator -sdcard ~src/i-jetty/sdcard.img
