How to build
============
0)  Install the android.jar from your android install directory into your
    local maven repository:
      $ mvn install:install-file -DgroupId=android -DartifactId=android \
        -Dversion=[android version] -Dpackaging=jar \
        -Dfile=[path to android.jar]

1)  Check out the project from code.google.com:
    $ svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only
    
2)  Build i-jetty. You will need to have maven installed:
        $ cd i-jetty-read-only
        $ mvn clean install -Dandroid.home=[path to your android installation]

3) Build two example web applications:
        $ cd console
        $ mvn clean install -Dandroid.home=[path to your android installation]
        $ cd hello
        $ mvn clean install -Dandroid.home=[path to your android installation]

4) Make an sdcard image if you do not already have one:
        $ scripts/create-sdcard.sh

5) Copy the webapps and i-jetty onto the sdcard.img:
        $ scripts/sync-sdcard.sh

4)  Run i-jetty:
        $ export ANDROID_SDK=[path to your android installation]
        $ scripts/deploy.sh

    The script will start the emulator for you. Once it's done, the 
    emulator should pop up and boot into Android.

3)  To start i-jetty, simply open the "Manage Jetty" activity in the emulator
    and click "Start Jetty". You should be able to point your favorite browser
    at http://localhost:8888/ (or http://localhost:8080 on the phone) and check
    out Jetty from Android!

Troubleshooting
===============
If you encounter any errors, you can run the deploy script with verbose output
enabled, like so:
$ VERBOSE=on ./deploy.sh

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
  
  adb push /my/local/pic.jpg /sdcard/Camera/dcim

Scripts
=======

The one script that may of be use to regular users is cleanup.sh. Its purpose
is to clean any Maven builds, and make sure the sdcard-layout directory is
pristine. You might like to use it before doing a stable release, or for testing
before commit.

Except for cleanup.sh, the scripts/ directory contains a number of utility
scripts used by the main deploy.sh shell script. They are intended to be called
from the main checkout, since they assume that path as the working directory:


    cleanup.sh              -   Cleans builds and sdcard-layout. See first
                                paragraph for more information.

    create-sdcard.sh        -   Creates a new sdcard.img file. Should be run as
                                root. Accepts optional filename and image size
                                arguments. See source header for more info.

    sync-sdcard.sh          -   Mounts the sdcard image on a loopback device
                                (requires root permissions, will prompt), cleans
                                the SD card, and copies the sdcard-intermediate
                                directory onto the card image. Note that it does
                                *NOT* unmount the image. This is done back in 
                                deploy.sh.

Most of these scripts (except cleanup.sh) depend on one or more of following
environment variables be available to them:

    ANDROID_SDK_TOOLS       -   Path to SDK tools/ directory. Use ANDROID_SDK
                                if you're after the root SDK path.

    VERBOSE_ARGS            -   Flags to pass to programs if they should be
                                verbose. "-v" if verbose, empty otherwise.

    UNZIP_ARGS              -   Flags to pass to the 'unzip' program. Empty if
                                verbose, "-qq" otherwise.

    BUILD_ARGS              -   Flags to pass to Maven while building. Empty if
                                verbose, "-q" otherwise.
