How to build
============
0)  Install the android.jar from your android install directory into your
    local maven repository:
      $ mvn install:install-file -DgroupId=android -DartifactId=android \
        -Dversion=[android version] -Dpackaging=jar \
        -Dfile=[path to android.jar]

1)  Check out the project from code.google.com:
    $ svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only
    
2) Build the example web applications:
        $ cd console
        $ mvn clean install -Dandroid.home=[path to your android installation]
        $ cd hello
        $ mvn clean install -Dandroid.home=[path to your android installation]
        $ cd chat
        $ mvn clean install -Dandroid.home=[path to your android installation]

3)  Build i-jetty. You will need to have maven installed:
        $ cd i-jetty-read-only
        $ mvn clean install -Dandroid.home=[path to your android installation]

4) Make an sdcard image if you do not already have one:
        $ scripts/create-sdcard.sh

5)  Run i-jetty:
        $ emulator -sdcard [path to the sdcard.img created in step 4]

6)  Install i-jetty:
        $ adb install target/i-jetty-debug.apk

7)  If you want to be able to access the webapps running on the phone from
    your desktop browser, then link up the i-jetty port on the phone to that
    of your desktop environment. For example, assuming i-jetty is running on
    it's default port of 8080, and you want to be able to access that on your
    desktop as port 8888, do:

        $ adb forward tcp:8888 tcp:8080

8)  To start i-jetty, simply open the "Manage Jetty" activity in the emulator
    and click "Start Jetty". You should be able to point your favorite browser
    at http://localhost:8888/ (or http://localhost:8080 on the phone) and check
    out Jetty from Android!

9) There is a console webapp accessible at http://localhost:8888/console (or 
   http://localhost:8080/console on the phone). You can use it to manage
   your on-phone data such as Contacts, Call Logs etc.



Using i-jetty
=============

Starting i-jetty
----------------
Navigate to the "Manage Jetty" application and click on it to activate. Click
on the "Start Jetty" button.

Stopping i-jetty
----------------
If you already have i-jetty running, you can click and drag down the i-jetty
icon in the navigation bar at the top of the screen to see the "Manage Jetty"
task. Click on the task.

Now click on the "Stop Jetty" button.


Configuring i-jetty
-------------------

Click on the "Configure" button to change the settings for i-jetty.

Currently supported settings are:

 + Use NIO [true|false]
 + Port [8080]
 + Password for console [admin]

Downloading new webapps
-----------------------

Click on the "Download" button and then enter the http url of a 
android-enabled webapp. The webapp will be downloaded and installed
to i-jetty.

You may need to restart i-jetty in order to start the newly installed
webapp. 



Troubleshooting
===============

If the webapps are not able to inflate their WEB-INF/lib/classes.dex files
to /data/dalvik-cache, you'll see log messages like:
  "Can't open dex cache '/data/dalvik-cache/sdcard@jetty@webapps@chat@WEB-INF@lib@classes.zip@classes.dex'" 
To fix that, you'll need to follow these steps:
   1. stop ijetty using the emulator application
   2. in a desktop window do:
      $ adb shell
      > chmod 777 /data/dalvik-cache
   3. restart ijetty using the emulator application



Adding photos to contacts
=========================

* Open a new emulator from your SDK directory (optionally, you can enable an
  SD card now - check out readme.sdcard.txt). You might want to run this as a
  background process, eg:
  
  $ ./emulator -sdcard ~/src/i-jetty/sdcard.img &

* On the emulator, start the Contacts activity and add a few people you know.

* To upload photos to associate with your contacts, the easiest way is to
  use the adb program to push your images onto the emulator into the directory
  where the images from the Camera are stored. You can then use the Contacts
  application to pick the image to suit the Contact. Here's an example:
  
  $ adb push /my/local/pic.jpg /sdcard/Camera/dcim

Scripts
=======
These are intended to be called from the main checkout, since they assume that 
path as the working directory:


    create-sdcard.sh        -   Creates a new sdcard.img file. Accepts optional
                                filename and image size arguments. See source 
                                header for more info.

