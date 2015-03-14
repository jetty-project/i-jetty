# Build the Console Web Application #

After [checking out](http://code.google.com/p/i-jetty/source/checkout) the i-jetty source, you will have the console application in $IJETTY\_HOME/console, assuming $IJETTY\_HOME represents where you checked out the code.

The console application consists of 2 maven subprojects:

  1. console/webapp
  1. console/apk

The former is the web application itself, specially built for Android. The latter is an Android installer application. This is for use by the i-jetty developers to allow us to release the webapp separately from core i-jetty so it can be conveniently downloaded from the Android Marketplace. You won't need to build that, although you can of course look at the app if you want.

## Prerequisites ##

Are similar for core i-jetty:

  1. [java](http://java.sun.com/javase)
  1. [maven](http://maven.apache.org)
  1. [Android SDK](http://http://developer.android.com/sdk/index.html) 1.6 or higher

However, in addition you must set an environment variable which indicates where the Android SDK is installed on your machine. For example, if I have installed the SDK at `/opt/android/android-sdk-linux_86` then I would set:

`export ANDROID_HOME=/opt/android/android-sdk-linux_86`

Note: you need to set this environment variable appropriately for your operating system.

## Building ##

```
cd $IJETTY_HOME/console
mvn clean install
```

This will produce a war file in $IJETTY\_HOME/console/webapp/target, and also the apk in $IJETTY\_HOME/console/apk/target (although as we mentioned, you're probably not interested in that).

## Building with Eclipse ##

To build with Eclipse, you will first need to install the M2Eclipse plugin:

http://eclipse.org/m2e/

Then, use the `Import->Existing Maven Projects` menu option to import the console parent project with both its child projects. You will then have these Eclipse projects:

  1. console (the webapp)
  1. i-jetty-console-installer (the installer apk)
  1. console-parent (the parent project)

You can use the normal Eclipse mechanisms to build the console webapp.

**NOTE:**  Ensure that you have your $ANDROID\_HOME environment variable set when you launch Eclipse. If you are using a launcher to start the Eclipse executable on Ubuntu, depending on how you set up your environment variables you may need to do some extra configuration to ensure your environment vars are set. For example, you could write a small shell script to ensure that ANDROID\_HOME is set before invoking Eclipse.