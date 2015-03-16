i-jetty Instructions
====================

New versions of i-jetty can be downloaded from the Android Marketplace.

From version 3.0 onwards, a web application that exposes your phone information
via the network - called the "Console WebApp" - is also available for
download via the Android Marketplace.

Building  from Source
=====================

Depedencies
-----------
* git
* Java 1.6 
* Maven


Checkout source
---------------
Check out the project from github.com:

    $ git clone https://github.com/jetty-project/i-jetty.git


Source structure
----------------

The checkout will produce a directory structure like so:

 + i-jetty
    + i-jetty-server      : adaptation of Jetty to Android 
    + i-jetty-ui          : Android app bundle for Jetty

 + console
    + webapp              : webapp for controlling phone remotely
    + apk                 : Android app bundle for installing webapp

 + example-webapps        : example webapps integrated with Android APIs


Building
--------

1) cd i-jetty-read-only/i-jetty 
2) mvn clean install


This produces an adroid app bundle in i-jetty-ui/target/i-jetty-xxxxx.apk. This apk
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

 HTTP Connector Settings
   + Use NIO [true|false]
   + Non SSL Port [8080]
 HTTPS Connector Settings
   + Use SSL [false|true]
   + Use NIO [true|false]                  *** only on Android 2.2 and greater
   + Keystore Password [jetty default value]
   + Keystore Filename [/sdcard/etc/keystore]
   + Password [jetty default value]
   + Truststore Password [jetty default value]
   + Truststore Filename [/sdcard/etc/keystore]


HTTP Connector Settings
----------------
You can choose to use either an NIO (SslSelectChannelConnector) or 
a BIO (SocketConnector) based connector. For information on the
differences between these connectors, see 
http://docs.codehaus.org/display/JETTY/Configuring+Connectors

NIO is the Default.


HTTPS Connector Settings
------------
An SSL connector will not be started by default. If you wish to use
SSL, check the Use SSL checkbox. You will not need to configure anything
else, as i-jetty will use its own preconfigured keystore and password 
settings. If you wish to use your own keystore, then provide the location
of the keystore file and passwords as appropriate. You may find it helpful
to refer to the Jetty documentation on the SSL connector at 
http://docs.codehaus.org/display/JETTY/Ssl+Connector+Guide.

If you are running on Android 2.2 or greater, you also have the option to use
an NIO SSL connector. Prior releases use a BIO SSL connector.


Downloading new webapps
-----------------------
Click on the "Download" button and then enter the http url of a 
android-enabled webapp. The webapp will be downloaded and installed
to i-jetty.

You may need to restart i-jetty in order to start the newly installed
webapp. 


