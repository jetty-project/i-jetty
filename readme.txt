How to do Port Forwarding
==========================
adb shell
# forward tcp:8888 tcp:8080


How to Add Files to removable storage device
============================================
(Debian instructions)
mksdcard 2147483648 ~/src/i-jetty/sdcard.img
sudo losetup /dev/loop0 ~/src/i-jetty/sdcard.img
mkdir ~/src/i-jetty/sdcard
sudo mount /dev/loop0 ~/src/i-jetty/sdcard
gksudo nautilus ~/src/i-jetty/sdcard

Load into emulator:
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





