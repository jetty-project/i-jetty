i-jetty VERSION:  2.1
RELEASE DATE:     16 November 2009
SVN:              https://i-jetty.googlecode.com/svn/tags/i-jetty-2.1
ANDROID VERSION:  1.6_r1


Changes
=======
+ Support for sdk 1.6
+ Console webapp classes refactored back into console webapp 
+ Console webapp provides media upload and download
+ Console webapp uses JSON and XHR
+ Addition of SSL connector and configuration
+ Upgrade to jetty-6.1.21


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

Non SSL Connector Configuration
 + Use NIO [true|false]
   The default is false. Select true if you want
   i-jetty to use NIO style connectors. Note that
   there may be issues on the dalvik jvm with some
   of the NIO libraries.

 + Port [8080]
   The port on which i-jetty will listen for requests.

SSL Connector Configuration
 + Use SSL [false|true]
   The default is false. Select true if you want 
   i-jetty to ALSO start an SSL connector.

 + Keystore Password 
   Enter a password ONLY if you are providing your own keystore.
   This is the password that is passed to the KeyStoreManagerFactory.init()
   method.
   
 + Keystore Filename [/sdcard/etc/keystore]
   If you have a different keystore file, specify it here.

 + Password
   Enter a password ONLY if you are providing your own keystore.
   This is the password that is passed to KeyStore.load() method.

 + Truststore Password 
   Enter a password ONLY if you are providing your own keystore.

 + Truststore Filename [/sdcard/etc/keystore]
   If you are not using the i-jetty default, then specify the
   location of your truststore here.

Console Configuration
 + Password for console [admin]
   The password which allows access to the console
   web application.


Download
--------

Click on the Download button and then enter the http url of a 
android-enabled webapp, and enter the context path at which
you want the web application to be deployed. The webapp will be 
downloaded and installed to i-jetty. If i-jetty is already running, 
you will need to stop and restart.


Console
-------
Available on the phone browser at:
   http://localhost:8080/console
or
   https://localhost:8443/console (if you are using an SSL connector)

Or from your desktop on:
    http://[LAN IP address of phone]:8080/console
or
    https://[LAN IP address of phone]:8443/console (if using SSL)
or
    http://localhost:8888/console (if you use port forwarding) 

This webapp makes available the on-phone content such as Contacts, 
Call Logs, System Settings and Media via a browser. To access the console, 
you need to supply the following authentication information:

     login:    admin
     password: admin

Note that you can use i-jetty's Configure button to change the password.


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


Jetty Configuration on the sdcard
=================================

When i-jetty is started, it will automatically create the
following structure on the sdcard:

   /sdcard
       /jetty
           /etc
               realm.properties
               webdefaults.xml
               keystore
           /contexts
           /webapps
               /console


