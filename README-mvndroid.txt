I decided to experiment with one of the 2 maven plugins for android with this project.

Both plugins are not ready for prime time.
I prefer the mvndroid plugin at this time, as it follow the maven way.
  (It has packagings of apk, and splits all of the tools into separate mojos)

  http://code.google.com/p/mvndroid/

  I want to work with the lead developer (on my spare time) to enhance this plugin
  to be best of breed. :-)

Anyway, to use this plugin, there are 2 requirements.

1) You have a new repository that needs to be reached.
   http://mvndroid.googlecode.com/svn/maven/2

   So if you use a local maven repository manager like Archiva or Nexus you'll
   need to add this to your search / downstream / proxy setups (or whatever
   they might be called on your repo manager of choice)

2) You have to have an ANDROID_SDK environment variable setup.
   Currently, the Generation 3 SDK layouts confuse mvndroid (but the code in
   mvndroid trunk fixes this)

   That means you'll need to point to a specific Android version with this
   environment variable.

   I setup my environment variable as such.
   ANDROID_SDK=/home/joakim/java/android/android-sdk_r06-linux_86/platforms/android-4

That's what I've figured out (so far), still more to fix.

You can see where it is with the following command ...

  $ mvn --file pom-mvndroid.xml clean install

