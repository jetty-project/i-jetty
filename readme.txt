How to Build
============
Check out the project from code.google.com:
 svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only

Do an ant build FIRST, to download some jetty classes that need to be
compiled for dalvik vm:
 cd i-jetty-read-only; ant

Use the eclipse plugin or apk tool to build the project and deploy
to the emulator.


How to do Port Forwarding
==========================
adb forward tcp:8888 tcp:8080


How to Add Files to removable storage device
============================================
(Debian instructions)
mksdcard 2147483648 ~/src/i-jetty/sdcard.img
sudo losetup /dev/loop0 ~/src/i-jetty/sdcard.img
mkdir ~/src/i-jetty/sdcard
sudo mount /dev/loop0 ~/src/i-jetty/sdcard
gksudo nautilus ~/src/i-jetty/sdcard

Load into emulator:

How to Start Emulator with removable storage device
===================================================
emulator -sdcard ~src/i-jetty/sdcard.img


How to Access Onboard Database
==============================
adb shell
# sqlite3 /data/data/com.google.android.providers.contacts/databases/contacts.db 
# select _id,name from people;
# mkdir /data/data/com.google.android.providers.contacts/photos
adb push <hostfile> <remotefile> (use _id as name of each file)
adb shell
# sqlite3 /data/data/com.google.android.providers.contacts/databases/contacts.db
# UPDATE people SET photo = '/data/data/com.google.android.providers.contacts/photos/' || _id || '.png';





