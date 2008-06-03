How to build
============
Check out the project from code.google.com:
 svn checkout http://i-jetty.googlecode.com/svn/trunk/ i-jetty-read-only

Do an ant build FIRST, to download some jetty classes that need to be
compiled for dalvik vm:
 cd i-jetty-read-only; ant

Use the eclipse plugin or apk tool to build the project and deploy
to the emulator.

Then, follow the steps below:

Adding photos to contacts
=========================

* Close the emulator started by Jetty after deployment.
* Open a new emulator from your SDK directory (optionally, you can enable an
  SD card now - check out readme.sdcard.txt). You might want to run this as a
  background process, eg:
  
  $ ./emulator &

* On the emulator, start the Contacts activity and add a few people you know.
* Still inside the SDK directory, open a shell to the Android device by using
  adb, and exectute the following commands:
  
  $ ./adb shell
  # mkdir /data/data/com.google.android.providers.contacts/photos
  # sqlite3 /data/data/com.google.android.providers.contacts/databases/contacts.db 
  
* From within the SQLite client, run the following query:
  
  SELECT _id, name FROM people;
  
  That should give you a list of people you added in the third step, along with
  their IDs. Keep this for later. Type ".q" without quotes to quit the SQLite
  client, and 'exit' to leave the device shell.

* Now, we need to upload some images of our contacts! Find a few smallish PNG
  files that will represent the icons for our contacts. Put them in a
  directory, say, ~/friends.

* Make sure you're still in the SDK directory, and run the following:

  $ ./adb push ~/friends/$(PERSON).png /data/data/com.google.android.providers.contacts/photos/$(ID).png
  
  where $(PERSON) is the image on the local filesystem, and $(ID) is the value
  of the ID column that represents the person's image we're uploading - we found
  this out in step 5 when we ran a SELECT query on the database.
  
  Do this for each contact you have. Note that the images *must* be PNG files.

* Open a shell connection to the device again, and run the following:

  $ ./adb shell
  # sqlite3 /data/data/com.google.android.providers.contacts/databases/contacts.db
  
  and execute the following SQL query which will set the images to their
  respective contact:
  
  UPDATE people SET photo = '/data/data/com.google.android.providers.contacts/photos/' || _id || '.png';

Finally, follow the steps below:

Accessing the web server
============================

Execute the following command on in the SDK's tools directory, with
the emulator running:

$ ./adb forward tcp:8888 tcp:8080

Open the "Manage Jetty" activity in the Android emulator and click "Start Jetty"

Now, you should be able to point your favorite browser at
http://localhost:8888/ and check out Jetty from Android!
