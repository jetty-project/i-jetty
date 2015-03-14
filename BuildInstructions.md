# Building Core i-jetty #

_Updated for i-jetty-3.0_

You might also be interested in info on [Building](BuildingConsoleWebApplication.md) the [Console Web Application](ConsoleWebApplication.md).

## From the Command Line ##
Building i-jetty at the command line is very straightforward.

You'll need these tools:

  * svn
  * [java jdk](http://java.sun.com/javase/) - 1.5 or above
  * [maven](http://maven.apache.org/) - 3.1 or above
  * [android sdk](http://developer.android.com/sdk/index.html) installation - 1.6 or above
  * [android platform definition for 1.6 (api version 4)](http://developer.android.com/sdk/adding-components.html)

To build:

  1. Checkout the code from svn:  `svn co http://i-jetty.googlecode.com/svn/trunk/`

Then, assuming you checked out the code to $I\_JETTY\_HOME, and you have the android sdk installed at $ANDROID\_HOME, you do:

```
 cd $I_JETTY_HOME/i-jetty
 mvn clean install
```

Voila! You will have an installable .apk bundle in your `$I_JETTY_HOME/i-jetty/i-jetty-ui/target` directory. Now follow the instructions over at the [Android Developers](http://developer.android.com/) site to upload it to your phone or emulator.

### Tweaking the Build Process ###
We use the [maven android plugin](http://code.google.com/p/maven-android-plugin/) to build i-jetty. If you need to tweak the default values used by the Android build tools, you will find the documentation for this plugin helpful.

### Making an SDCard Image ###
i-jetty requires an sd card. If you're running with the emulator, you'll need to use the [mksdcard](http://developer.android.com/guide/developing/tools/othertools.html#mksdcard) utility from the [android sdk](http://developer.android.com/). Here's an example of creating a file called "sdcard.img" which is a card with 60Mb storage:

```
mksdcard 60M ./sdcard.img
```

When you start the emulator, you can choose which android platform runtime to use, but remember you need to start the emulator with the scard option. Here's an example of starting the 1.6 runtime with the sdcard we created in the step above:
```
emulator -avd avd1.6 -sdcard ./sdcard.img
```

If you're using the emulator, you'll want to install the ijetty package:
```
adb install i-jetty/i-jetty-ui/target/i-jetty-[ijetty.version]-aligned.apk
```

Where ijetty.version is the version number of i-jetty, eg 2.1-SNAPSHOT.

If you want to make changes to i-jetty, you can remove the installed i-jetty with:
```
adb uninstall org.mortbay.ijetty
```


## Using the Eclipse Plugin ##

You can import the i-jetty project into Eclipse. We use the wonderful [m2 Eclipse plugin](http://m2eclipse.codehaus.org/) to make it easy.

Note: due to some massaging of the i-jetty-server component to comply with the Dalvik JVM running on Android, we advise that you do not have that project open in eclipse, as the [Eclipse Android Plugin](http://developer.android.com/sdk/eclipse-adt.html) cannot properly do what's needed to get it working on Android.  The Maven build is what creates the proper i-jetty-server component that i-jetty-ui (and Android) eventually use.

Please contact the i-jetty developers at **dev@jetty.codehaus.org** for help.