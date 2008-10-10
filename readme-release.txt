VERSION:      1.0
RELEASE DATE: 10 October 2008
SVN:          https://i-jetty.googlecode.com/svn/tags/i-jetty-1.0


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


x. Copy the contents of the sdcard directory from the release onto the sdcard.img.

   $ adb push sdcard/jetty /sdcard/jetty


x. Copy the i-jetty application bundle to the Android emulator using the adb utility:

  $ adb install i-jetty-debug.apk


x. Ensure the emulator's unzipped classes cache is writeable:
   $ adb shell
   > chmod 777 /data/dalvik-cache


x. Link the i-jetty port from the emulator to your desktop environment
   using the adb utility. The i-jetty port by default is 8080. Here's
   how to link it to the desktop machine's port 8888:

   $ adb forward tcp:8888 tcp:8080


Now all you need to do is to start the i-jetty application.

Starting i-jetty
================

Using the emulator, select the i-jetty application icon, then push the "Start"
button.  i-jetty will start on port 8080.

A number of pre-packaged demonstration webapps are available:

  x. Hello webapp
     Available on the emulator browser at http://localhost:8080/hello
     or on your desktop browser at http://localhost:8888/hello. This
     is a simple webapp that demonstrates the use of a Servlet.

  x. Chat webapp
     Available on the emulator browser at http://localhost:8080/chat
     or http://localhost:8888/chat. This webapp deployes a cometd
     ajax chatroom application.

  x. Console webapp
     Available on the emulator browser at http://localhost:8080/console
     or http://localhost:8888/console. This webapp makes available the
     on-phone content such as Contacts, Call Logs, System Settings via
     the browser. To access the console, you need to supply the follwing
     authentication information:

     login:    admin
     password: admin
     

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

The sdcard.img is structured similarly to a standard Jetty layout:

   /sdcard
       /jetty
           /etc
               realm.properties
               webdefaults.xml
           /contexts
               hello.xml
               chat.xml
           /webapps
               /chat
               /hello
               /console


Known Issues
============

i-jetty 22: 
Using the Android browser, particularly the Linux version, with the 
chat webapp can cause the i-jetty process to die. This does not happen 
if desktop browsers are used to access the chat application. 
