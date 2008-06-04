//========================================================================
//$Id$
//Copyright 2008 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.ijetty;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ClassNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

import android.dalvik.DexFile;

/**
 * AndroidClassLoader
 *
 * Loads classes dynamically from dex files wrapped inside a jar.
 * 
 * 
 *   Example:
 *   
 *   <pre>
 *   try 
 *   {
 *          AndroidClassLoader classLoader = new AndroidClassLoader ();
 *          classLoader.addDexFile ("/sdcard/jetty.jar");   
 *          Class<Object> c = classLoader.findClass ("org.mortbay.jetty.Connector");
 *   }
 *   catch (Exception e)
 *   {
 *          Log.e ("Jetty", "Problem with AndroidClassLoader", e);
 *   }
 *   </pre>        
 */
public class AndroidClassLoader extends ClassLoader {   
    private Constructor<DexFile> dexFileClassConstructor = null;
    private Method dexFileClassLoadClass = null;
    
    private List<File> dexFiles;
    
    public AndroidClassLoader ()
    throws ClassNotFoundException, NoSuchMethodException  {
        Class<DexFile> dexFileClass =  (Class<DexFile>) Class.forName("android.dalvik.DexFile");
        dexFileClassConstructor = dexFileClass.getConstructor (new Class[] { java.io.File.class });
        dexFileClassLoadClass = dexFileClass.getMethod ("loadClass", new Class[] { String.class, ClassLoader.class });
        
        dexFiles = new ArrayList <File> ();
    }
    
    /**
     * Convenience constructor. Creates a new instance of the AndroidClassLoader,
     * and adds a path to the list of searchable DEX files. Note that this file
     * <strong>must</strong> exist, otherwise a FileNotFoundException will be thrown.
     * @param path Path to the DEX file.
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws FileNotFoundException
     */
    public AndroidClassLoader (String path)
    throws ClassNotFoundException, NoSuchMethodException, FileNotFoundException { 
        this ();
        if (!addDexFile (path)) {
            throw new FileNotFoundException ();
        }
    }
    
    /**
     * Adds a compiled DEX file to the list of files to be searched for classes.
     * @param path The path to the DEX file.
     * @return True if the file was added successfully, false otherwise.
     */
    public Boolean addDexFile (String path) {
        File file = new File (path);
        return addDexFile (file);
    }
    
    /**
     * Adds a compiled DEX file to the list of files to be searched for classes.
     * @param file The path to the DEX file.
     * @return True if the file was added successfully, false otherwise.
     */
    public Boolean addDexFile (File file) {
        if (file.exists()) {
            dexFiles.add(file);
            return true;
        } else {
            return false;
        }
    }
    
    public Class findClass (String name)
    throws ClassNotFoundException {
        if (dexFileClassConstructor == null || dexFileClassLoadClass == null) {
            return null;
        }
        
        Object dexFile = null;
        
        for (File file : dexFiles) {
            try {
                dexFile = dexFileClassConstructor.newInstance (new Object[] { file });
                
                Class c =  (Class) dexFileClassLoadClass.invoke (dexFile, 
                        new Object[] { name.replace('.','/'), getClass ().getClassLoader () });
                
                return c;
            } catch (Exception ex) {
                // Silenty ignore any exceptions, as not all DEX files might have
                // the class we're after - wait until we're done.
            }
        }
        
        throw new ClassNotFoundException ();
    }
}