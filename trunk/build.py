#!/usr/bin/env python
# build.py
# Cross-platform build helper for Android applications.
#
# Copyright 2009 Mort Bay Consulting Pty. Ltd.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at 
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import os
from optparse import OptionParser
import ConfigParser

try:
    import hashlib
except ImportERROR:
    import md5

# md5sums of android.jar
# for future use of version checking applications before build
android_releases = {
    "1.0_r2" : "8cce78264a998882faece5ff8618803e",
    "1.1_r1" : "95c651eb8f5689776987e06d26c58a79"
}

parser = OptionParser (usage="%prog [options]", version="%prog 1.0", description="Helps with building Android applications.")
parser.add_option('--configure',
                    dest='configmode',
                    action='store_true',
                    default=False,
                    help="enter config mode")

parser.add_option('-s', '--spec',
                    dest='specfile',
                    default="product.spec",
                    help="product specification [default: %default]")

parser.add_option('-c', '--config',
                    dest='buildconfig',
                    default="default.build",
                    help="build config file to read/save [default: %default]")

parser.add_option('-v', '--verbose',
                    dest='verbose',
                    action='store_true',
                    default=False,
                    help="spew lots of text [default: %default]")

(options, args) = parser.parse_args()

# Handy utilities
def md5file(filename):
    """Return the hex digest of a file without loading it all into memory"""
    fh = open(filename)
    try:
        digest = hashlib.md5()
    except:
        digest = md5.new()
    while 1:
        buf = fh.read(4096)
        if buf == "":
            break
        digest.update(buf)
    fh.close()
    return digest.hexdigest()

def pretty_fail(msg, exception, return_code=1):
    print "ERROR: %s" % msg
    print "Exception:"
    print exception
    sys.exit(return_code)

def ask(msg, default=None, path=False, allow_empty=False, options=None):
    val = ""
    asking = True
    promptstr = msg
    if options is not None:
        promptstr += " %s" % str(options)
    
    promptstr += ": "
    
    if default is not None:
        promptstr += "[%s] " % default
        allow_empty = True
    
    while asking:
        val = raw_input(promptstr).strip()
        # First, check length
        if not allow_empty and len(val) != 0:
            asking = False
        elif allow_empty:
            asking = False
        
        # Now, check if it's a valid path and that's what we're entering
        if not asking and not allow_empty and path:
            val = os.path.expanduser(val)
            asking = not os.path.isdir(val)
        
        if not asking and not allow_empty and options:
            val = val.lower()
            
            # FIXME: Better way than using private functions?
            asking = not options.__contains__(val)
        
        if asking:
          print "Bad input. Try again."
    
    if len(val) == 0:
        return default
    else:
        return val

def ask_bool(msg, default_yes=True):
    if default_yes:
        promptstr = "%s: [Y/n] " % msg
    else:
        promptstr = "%s: [y/N] " % msg
    
    val = raw_input(promptstr).strip().lower()
    if len(val) == 0:
        if default_yes:
            return "yes"
        else:
            return "no"
    else:
        if val.startswith("y"):
            return "yes"
        else:
            return "no"

# Mode-specific functions
def do_config(config, filename, productSDK):
    config.add_section ("Paths")
    sdkpath = ask("Path to Android SDK", path=True)
    config.set ("Paths", 'sdkpath', sdkpath)
    
    androidjar = os.path.join (sdkpath, "android.jar")
    if not os.path.isfile (androidjar):
        print "ERROR: cannot find android.jar inside Android SDK path."
        sys.exit(1)
        
    md5sum = md5file (androidjar).lower()
    sdkver = None
    for release in android_releases:
        release_sum = android_releases[release].lower()
        if release_sum == md5sum:
            sdkver = release
    
    if sdkver is None:
        print "Could not guess SDK version."
        sdkver = ask("SDK version", "1.1_r1")
    else:
        print "Guessed SDK version %s" % sdkver
    
    if productSDK != sdkver:
        print
        print "WARNING: the SDK version you're using does not match"
        print "the one that the software has been built for. There may"
        print "be build errors due to changes in API."
        print
        print "Your API version is %s, while %s is required." % (sdkver, productSDK)
        print
        if ask_bool("Do you wish to continue?", False) == 'no':
            sys.exit(0)
    
    config.set ("Paths", 'sdkver', sdkver)
    
    config.add_section ("SD")
    enablesd = ask_bool("Enable SD card with emulator?")
    config.set ("SD", 'usesd', enablesd)
    if enablesd != "no":
        config.set ("SD", 'sdcard', ask("SD Card filename", "sdcard.img"))
        config.set ("SD", 'clean', ask_bool("Use fresh SD card every build?", False))
    
    config.add_section ("Build")
    config.set ("Build", 'build', ask_bool("Actually build the project?"))
    config.set ("Build", 'system', ask("Build system", "maven", options=('ant', 'make', 'maven')))
    config.set ("Build", 'alwaysclean', ask_bool("Always do make clean?"))
    
    emulator = ask_bool("Run emulator?")
    config.set ("Build", 'runemulator', emulator)
    if emulator != "no":
        config.set ("Build", 'wipedata', ask_bool("Always clear emulator data?"))
    
    config.add_section ("ADB")
    config.set ("ADB", 'usb', ask_bool("Using USB device?", False))
    
    forward = ask_bool("Forward ports?", False)
    config.set ("ADB", 'forwardports', forward)
    
    if forward != "no":
        config.set ("ADB", 'forwardsrc', ask("Source port to forward", "tcp:8888"))
        config.set ("ADB", 'forwarddest', ask("Destination port to forward", "tcp:8080"))
    
    print "Saving to '%s'..." % filename
    try:
        configfile = open (filename, 'wb')
        config.write (configfile)
        configfile.close ()
    except Exception, e:
        pretty_fail ("Failed to write configuration file: %s" % filename, e)
    
    print "Configuration complete!"

def do_build (config, buildconfig, verbose=False, args=None):
    # Work out arguments and stuff
    if args.count ("nobuild") > 0:
        build = False
    elif args.count ("build") > 0:
        build = True
    else:
        build = config.getboolean ("Build", "build")
        
    if args.count ("noemulator") > 0:
        emulator = False
    elif args.count ("emulator") > 0:
        emulator = True
    else:
        emulator = config.getboolean ("Build", "runemulator")
    
    if args.count ("noupload") > 0:
        upload = False
    else:
        upload = True
    
    androidpath = config.get ("Paths", "sdkpath")
    
    # Make sure SD card is setup
    usesd = config.getboolean ("SD", "usesd")
    sdcard = None
    if usesd:
        size = 60 * 1024 * 1024 # 60 MB
        sdcard = config.get ("SD", "sdcard")
        if not os.path.isfile (sdcard):
            print "WARNING: SD card does not exist. Creating..."
            os.system (os.path.join (androidpath, "tools", "mksdcard") + (" %d %s" % (size, sdcard)))
        elif config.getboolean ("SD", "clean"):
            if os.path.exists (sdcard + ".lock"):
                print "WARNING: cannot clean out SD card; locked."
            else:
                print "Cleaning SD card..."
                os.remove (sdcard)
                os.system (os.path.join (androidpath, "tools", "mksdcard") + (" %d %s" % (size, sdcard)))
    
    # Build section
    if build:
        print "Building..."
        system = config.get ("Build", "system").lower()
        if system == "make":
            print "ERROR: I don't know how to build with 'make' yet!"
            sys.exit(1)
        elif system == "ant":
            print "ERROR: I don't know how to build with 'ant' yet!"
            sys.exit(1)
        elif system == "maven":
            cmd = None
            installcmd = "mvn install:install-file -DgroupId=android -DartifactId=android -Dversion=%s -Dpackaging=jar -Dfile=%s > build.log" % (config.get ("Paths", "sdkver"), os.path.join (androidpath, "android.jar"))
            
            if verbose: print "Installing artifact..."
            ret = os.system (installcmd)
            if ret != 0:
                print "ERROR: installing artifact failed. Check build.log for details."
                if verbose: print "Got return code %d" % ret
                sys.exit(2)
            
            if verbose: print "Building with Maven."
            if config.getboolean ("Build", "alwaysclean"):
                cmd = "mvn clean install -Dandroid.home=%s -Dandroid-version=%s > build.log" % (androidpath, config.get ("Paths", "sdkver"))
            else:
                cmd = "mvn install -Dandroid.home=%s -Dandroid-version > build.log" % (androidpath, config.get ("Paths", "sdkver"))
            
            if verbose: print "Executing: %s" % cmd
            ret = os.system (cmd)
            if ret != 0:
                print "ERROR: Build failed. Check build.log for details."
                if verbose: print "Got return code %d" % ret
                sys.exit(2)
        else:
            print "ERROR: unknown build system '%s'."
            sys.exit(1)
        
    elif verbose:
        print "Config says we shouldn't build."
    
    # Emulator section
    adb = os.path.join (androidpath, "tools", "adb")
    if emulator:
        wipedata = config.getboolean ("Build", "wipedata")
        
        print "Launching emulator..."
        cmd = os.path.join (androidpath, "tools", "emulator")
        
        if usesd:
            cmd += " -sdcard %s" % sdcard
        
        if wipedata:
            cmd += " -wipe-data"
        
        if verbose: print "Running '%s' in background." % cmd
        if os.name == "nt":
            # FIXME: Test me under windows!
            os.spawnle(os.P_NOWAIT, cmd, os.environ)
        else:
            # Works in most unix shells
            os.system(cmd + " &")
        
        import time
        # Sleep for ten seconds before checking status
        if verbose: print "Sleeping for 40 seconds..."
        time.sleep (40)
        
        # Wait for emulator to start (only works on recent emlators 1.0+;
        # previous were buggy)
        if verbose: print "Running '%s' in foreground; waiting for emulator" % adb
        os.system (adb + " wait-for-device")
        if verbose: print "We're back!"
    elif verbose:
        print "Config says we're not using an emulator."
    
    # ADB/upload section
    if upload:
        device = ""
        if config.getboolean ("ADB", "usb"): device = "-d"
        
        #print "Uninstalling..."
        #print "(ignore any errors for this stage if you haven't yet installed)"
        #os.system ("%s %s uninstall org.mortbay.ijetty" % (adb, device))
        
        print "Installing..."
        pkg = buildconfig.get ("Upload", "Package").replace ("_VERSION_", buildconfig.get ("Product", "Version"))
        install_cmd = "%s %s install -r %s" % (adb, device, pkg)
        ret = os.system (install_cmd)
        if ret != 0:
            print "ERROR: failed to install!"
            print "Ran with: '%s'" % install_cmd
    
    if config.getboolean ("ADB", "forwardports"):
        print "Forwarding ports..."
        os.system ("%s %s forward %s %s" % (adb, device, config.get ("ADB", "forwardsrc"), config.get ("ADB", "forwarddest")))
    
    print "Done!"

# Main program
if __name__ == "__main__":
    config = ConfigParser.SafeConfigParser ()
    buildconfig = ConfigParser.SafeConfigParser ()
    
    if options.configmode:
        if not os.path.isfile (options.specfile):
            print "ERROR: Product spec file '%s' does not exist."
            print "Please create one before attempting to build!"
            sys.exit(1)
        
        buildconfig.read (options.specfile)
        do_config (config, options.buildconfig, buildconfig.get ("Product", "SDK"))
    else:
        if not os.path.isfile (options.buildconfig):
            print "ERROR: Configuration file '%s' does not exist." % options.buildconfig
            print "*** Did you remember to run with --configure first? ***"
            sys.exit(1)
        try:
            config.read (options.buildconfig)
        except Exception, e:
            pretty_fail ("Failed to read configuration file: %s" % options.buildconfig, e)
        
        if not os.path.isfile (options.specfile):
            print "ERROR: Product spec file '%s' does not exist."
            print "Please create one before attempting to build!"
            sys.exit(1)
        
        buildconfig.read (options.specfile)
        
        print "Using build configuration '%s'" % options.buildconfig
        print "Product: %s" % buildconfig.get ("Product", "Name")
        print "Version: %s" % buildconfig.get ("Product", "Version")
        print
        do_build (config, buildconfig, options.verbose, args)
