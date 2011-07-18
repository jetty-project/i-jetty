i-jetty Instructions
====================

Building  from Source
=====================

Depedencies
-----------
* Subversion
* Java 1.6 
* Maven
* Google Android SDK

Steps
-----
These instructions are loosely adapted from
http://code.google.com/p/i-jetty/source/checkout
Please see this URL for more details (and ability to browse the repository)

1) Check out the project from code.google.com:
    $ svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only

2) Ensure your ANDROID_HOME environment variable is set and points to
   your local installation of the Android SDK. Also ensure that your PATH
   environment variable is set. For instructions on downloading and installing
   the Android SDK, see http://developer.android.com/sdk/installing.html


3) Go to your i-jetty-read-only directory and type "mvn clean install". This will
   produce an adroid bundle in i-jetty-ui/target/i-jetty-debug.apk. This apk
   file can then be installed to the phone or to an emulator. For help on installing
   apk bundles to the phone or the emulator, see http://developer.android.com



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

 Non-SSL Port Settings
   + Use NIO [true|false]
   + Non SSL Port [8080]
 SSL Port Settings
   + Use SSL [false|true]
   + Keystore Password [jetty default value]
   + Keystore Filename [/sdcard/etc/keystore]
   + Password [jetty default value]
   + Truststore Password [jetty default value]
   + Truststore Filename [/sdcard/etc/keystore]
  Console Settings 
   + Password for console [admin]


Non-SSL Settings
----------------
You can choose to use either an NIO (SslSelectChannelConnector) or 
a BIO (SocketConnector) based connector. For information on the
differences between these connectors, see 
http://docs.codehaus.org/display/JETTY/Configuring+Connectors


SSL Settings
------------
An SSL connector will not be started by default. If you wish to use
SSL, check the Use SSL checkbox. You will not need to configure anything
else, as i-jetty will use its own preconfigured keystore and password 
settings. If you wish to use your own keystore, then provide the location
of the keystore file and passwords as appropriate. You may find it helpful
to refer to the Jetty documentation on the SSL connector at 
http://docs.codehaus.org/display/JETTY/Ssl+Connector+Guide.


Downloading new webapps
-----------------------
Click on the "Download" button and then enter the http url of a 
android-enabled webapp. The webapp will be downloaded and installed
to i-jetty.

You may need to restart i-jetty in order to start the newly installed
webapp. 


Console Web Application
-----------------------
TODO TODO TODO TODO 
I-jetty comes preconfigured with a console-like web application that
makes available the information on your phone over the web. This means
that you can browse and control your phone from your desktop browser,
or any other device sharing the same LAN as the phone. 

Surf to http://localhost:8080/console/ to:

 + create, edit and delete your phone Contacts
 + see your Settings
 + see your Call Logs
 + upload and download media such as sounds, video and images

