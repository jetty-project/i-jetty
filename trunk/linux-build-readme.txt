Depedencies
===========

* Subversion
* Java 1.5 (or 1.6, but only report bugs if they occur with 1.5)
* Maven
* Python

How to build
============
   
0) Check out the project from code.google.com:
    $ svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only

1) Run build.py --configure
   Default are fine, except for port forwarding, which you will probably
   want to enable, so you can access Jetty from your desktop webbrowser as
   well as on the phone.

2) Run build.py with no arguments to setup your SD card, build i-jetty, run
   the emulator and upload the Android package.

3)  To start i-jetty, simply open the "Manage Jetty" activity in the emulator
    and click "Start Jetty". You should be able to point your favorite browser
    at http://localhost:8888/ (or http://localhost:8080 on the phone) and check
    out Jetty from Android!

4) There is a console webapp accessible at http://localhost:8888/console (or 
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
   1. stop i-jetty application
   2. in a desktop window do:
      $ adb shell
      > chmod 777 /data/dalvik-cache
   3. restart i-jetty application


Adding photos to contacts
=========================

* On the emulator, start the Contacts activity and add a few people you know.

* To upload photos to associate with your contacts, the easiest way is to
  use the adb program to push your images onto the emulator into the directory
  where the images from the Camera are stored. You can then use the Contacts
  application to pick the image to suit the Contact. Here's an example:
  
  $ adb push /my/local/pic.jpg /sdcard/Camera/dcim
