VERSION:      1.1
RELEASE DATE: 22 October 2008
SVN:          https://i-jetty.googlecode.com/svn/tags/i-jetty-1.1


Notes
=====

These instructions refer to the release bundle structure and NOT
to the structure found in svn.


Installation
============

x. Ensure that you have the Android tools directory in your path.

x. Make an sdcard image:
  $ mksdcard 60M sdcard.img

x. Start the Android emulator if you didn't do it in the previous step:
   $ emulator -sdcard sdcard.img

   You will need to wait a few moments for the emulator to start. 
   When you see the android icon start to pulsate on the screen
   you are ready to proceed to the next step.

x. Copy the i-jetty application bundle to the Android emulator using the adb utility:

  $ adb install i-jetty-debug.apk

x. Ensure the emulator's unzipped classes cache is writeable:
   $ adb shell
   > chmod 777 /data/dalvik-cache

x. Link the i-jetty port from the emulator to your desktop environment
   using the adb utility. The i-jetty port by default is 8080. Here's
   how to link it to the desktop machine's port 8888:

   $ adb forward tcp:8888 tcp:8080

i-jetty should now be installed on the phone. You can now start the i-jetty application.


Using i-jetty
================

On the phone, select the i-jetty application icon and click on it to
activate.

There are a number of options:

  + Start Jetty
    This button will start i-jetty.

  + Stop Jetty
    This button stops i-jetty.

  + Configure
    Change the setup for i-jetty.

  + Download
    Download a war and install it.


Start Jetty
-----------

Clicking the Start button will start an i-jetty service.
i-jetty will be available on the port that you selected
with the Configure button. The default port is 8080.


Stop Jetty
----------

The Stop Jetty button kills the i-jetty service. 
You will need to stop and then restart i-jetty for
newly downloaded webapps to be deployed.

Configure
---------

Currently supported settings are:

 + Use NIO [true|false]
   The default is false. Select true if you want
   i-jetty to use NIO style connectors. Note that
   there may be issues on the dalvik jvm with some
   of the NIO libraries.

 + Port [8080]
   The port on which i-jetty will listen for requests.

 + Password for console [admin]
   The password which allows access to the console
   web application.


Download
--------

Click on the Download button and then enter the http url of a 
android-enabled webapp, and enter the context path at which
you want the web application to be deployed. The webapp will b
e downloaded and installed to i-jetty. If i-jetty is already
running, you will need to stop and restart.


Console
-------
Available on the phone browser at http://localhost:8080/console
(or from your desktop on http://localhost:8888/console if you
configured the port forwarding). This webapp makes available the
on-phone content such as Contacts, Call Logs, System Settings via
the browser. To access the console, you need to supply the following
authentication information:

     login:    admin
     password: admin

Note that you can use i-jetty's Configure button to change the
password.


Android Enabled Webapps
========================

Some pre-packaged demonstration webapps are available on
the i-jetty website http://code.google.com/p/i-jetty/downloads/list.
They can be installed onto the phone using the i-jetty Download
button:

  x. Hello webapp
     This is a very simple webapp that demonstrates the use of a Servlet.

  x. Chat webapp
     This webapp deployes a cometd ajax chatroom application based on the
     Bayeux protocol.

     

Adding Photos to Contacts
=========================

x. Start the emulator if you haven't already:

   $ emulator -sdcard sdcard.img 

x. Using the emulator, start the Contacts activity and add a few people you know.

x. To upload photos to associate with your contacts, the easiest way is to
   use the adb utility to push your images onto the emulator into the directory
   where the images from the Camera are stored and which form part of the
   phot gallery. You can then use the Contacts application to pick the image 
   to suit the Contact. Here's an example:

   $ adb push /my/local/pic.jpg /sdcard/Camera/dcim/pic.jpg


Jetty Configuration on the sdcard
=================================

When i-jetty is started, it will automatically create the
following structure on the sdcard:

   /sdcard
       /jetty
           /etc
               realm.properties
               webdefaults.xml
           /contexts
           /webapps
               /console


