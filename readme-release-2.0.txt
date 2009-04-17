i-jetty VERSION:  2.0
RELEASE DATE:      
SVN:              https://i-jetty.googlecode.com/svn/tags/i-jetty-2.0
ANDROID VERSION:  1.1_r1



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
you want the web application to be deployed. The webapp will be 
downloaded and installed to i-jetty. If i-jetty is already running, 
you will need to stop and restart.


Console
-------
Available on the phone browser at http://localhost:8080/console
(or from your desktop on http://localhost:8888/console if you
configured the port forwarding). This webapp makes available the
on-phone content such as Contacts, Call Logs, System Settings and
Media via a browser. To access the console, you need to supply 
the following authentication information:

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

NOTE: due to security restrictions on release 1.1_r1 of Android, it will 
not be possible to install webapps using the G1 handset. 


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


