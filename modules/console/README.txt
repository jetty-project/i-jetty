Console
=======

Build Instructions
------------------

mvn clean install


Installation
------------

If you modify the console web application, you need to rebuild it
and then rebuild then reinstall onto android the  i-jetty main 
application, as the i-jetty bundle incorporates the console webapp.

For Developers
--------------
If you don't wish to rebuild and reinstall the i-jetty application
after making changes to the console webapp, you should be able to
simply unpack the console war file in the target directory to a 
temporary location, then use the "adb push" command to copy that
to the jetty file structure on the sdcard. For example:

  cd target
  mkdir -p tmp/console
  cd tmp/console
  jar xvf console-1.0-SNAPSHOT.war 
  adb push ../console /sdcard/jetty/webapps/console
