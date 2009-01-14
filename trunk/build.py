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

# md5sums of android.jar
# for future use of version checking applications before build
android_releases = {
    "1.0r2" : "8cce78264a998882faece5ff8618803e"
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
def pretty_fail(msg, exception, return_code=1):
    print "Error: %s" % msg
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
        
        if not asking and options:
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
        return "yes"
    else:
        if val.startswith("y"):
            return "yes"
        else:
            return "no"

# Mode-specific functions
def do_config(config, filename):
    config.add_section ("Paths")
    config.set ("Paths", 'sdkpath', ask("Path to Android SDK", path=True))
    
    config.add_section ("SD")
    enablesd = ask_bool("Enable SD card with emulator?")
    config.set ("SD", 'usesd', enablesd)
    if enablesd:
        config.set ("SD", 'sdcard', ask("SD Card filename", "sdcard.img"))
        config.set ("SD", 'clean', ask_bool("Use fresh SD card every build?", False))
    
    config.add_section ("Build")
    config.set ("Build", 'build', ask_bool("Actually build the project?"))
    config.set ("Build", 'system', ask("Build system", "maven", options=('ant', 'make', 'maven')))
    config.set ("Build", 'alwaysclean', ask_bool("Always do make clean?"))
    config.set ("Build", 'runemulator', ask_bool("Run emulator?"))
    
    config.add_section ("ADB")
    config.set ("ADB", 'usb', ask_bool("Using USB device?", False))
    
    forward = ask_bool("Forward ports?", False)
    config.set ("ADB", 'forwardports', forward)
    
    if forward:
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
        config.getboolean ("Build", "build")
        
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
            print "Warning: SD card does not exist. Creating..."
            os.system (os.path.join (androidpath, "tools", "mksdcard") + (" %d %s" % (size, sdcard)))
        elif config.getboolean ("SD", "clean"):
            if os.path.exists (sdcard + ".lock"):
                print "Warning: cannot clean out SD card; locked."
            else:
                print "Cleaning SD card..."
                os.remove (sdcard)
                os.system (os.path.join (androidpath, "tools", "mksdcard") + (" %d %s" % (size, sdcard)))
    
    # Build section
    if build:
        print "Building..."
        system = config.get ("Build", "system").lower()
        if system == "make":
            print "Error: I don't know how to build with 'make' yet!"
            sys.exit(1)
        elif system == "ant":
            print "Error: I don't know how to build with 'ant' yet!"
            sys.exit(1)
        elif system == "maven":
            cmd = None
            
            if verbose: print "Building with Maven."
            if config.getboolean ("Build", "alwaysclean"):
                cmd = "mvn clean install -Dandroid.home=%s > build.log" % androidpath
            else:
                cmd = "mvn install -Dandroid.home=%s > build.log" % androidpath
            
            if verbose: print "Executing: %s" % cmd
            ret = os.system (cmd)
            if ret != 0:
                print "Error: Build failed. Check build.log for details."
                if verbose: print "Got return code %d" % ret
                sys.exit(2)
        else:
            print "Error: unknown build system '%s'."
            sys.exit(1)
        
    elif verbose:
        print "Config says we shouldn't build."
    
    # Emulator section
    adb = os.path.join (androidpath, "tools", "adb")
    if emulator:
        print "Launching emulator..."
        cmd = os.path.join (androidpath, "tools", "emulator")
        
        if usesd:
            cmd += " -sdcard %s" % sdcard
        
        if verbose: print "Running '%s' in background." % cmd
        if os.name == "nt":
            # FIXME: Test me under windows!
            os.spawnle(os.P_NOWAIT, cmd, os.environ)
        else:
            # Works in most unix shells
            os.system(cmd + " &")
        
        import time
        # Sleep for ten seconds before checking status
        if verbose: print "Sleeping for 20 seconds..."
        time.sleep (20)
        
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
        ret = os.system ("%s %s install -r %s" % (adb, device, pkg))
        if ret != 0:
            print "Error: failed to install!"
    
    if config.getboolean ("ADB", "forwardports"):
        print "Forwarding ports..."
        os.system ("%s %s forward %s %s" % (adb, device, config.get ("ADB", "forwardsrc"), config.get ("ADB", "forwarddest")))
    
    print "Done!"

# Main program
if __name__ == "__main__":
    config = ConfigParser.SafeConfigParser ()
    buildconfig = ConfigParser.SafeConfigParser ()
    
    if options.configmode:
        do_config (config, options.buildconfig)
    else:
        if not os.path.isfile (options.buildconfig):
            print "Error: Configuration file '%s' does not exist." % options.buildconfig
            print "*** Did you remember to run with --configure first? ***"
            sys.exit(1)
        try:
            config.read (options.buildconfig)
        except Exception, e:
            pretty_fail ("Failed to read configuration file: %s" % options.buildconfig, e)
        
        if not os.path.isfile (options.specfile):
            print "Error: Product spec file '%s' does not exist."
            print "Please create one before attempting to build!"
            sys.exit(1)
        
        buildconfig.read (options.specfile)
        
        print "Using build configuration '%s'." % options.buildconfig
        print "Building %s version %s" % (buildconfig.get ("Product", "Name"), buildconfig.get ("Product", "Version"))
        do_build (config, buildconfig, options.verbose, args)
