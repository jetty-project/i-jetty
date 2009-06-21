package org.mortbay.ijetty;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.deployer.ConfigurationManager;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.util.AttributesMap;
import org.mortbay.util.Scanner;
import org.mortbay.xml.XmlConfiguration;

/**
 * AndroidContextDeployer
 * TODO this class should be able to extend ContextDeployer rather than
 * reimplement it entirely.
 *
 */
public class AndroidContextDeployer extends AbstractLifeCycle
{
    private AttributesMap _attributes = null;
    public final static String NAME="ConfiguredDeployer";
    private int _scanInterval=10;
    private Scanner _scanner;
    private ScannerListener _scannerListener;
    private Resource _configurationDir;
    private Map _currentDeployments=new HashMap();
    private ContextHandlerCollection _contexts;
    private ConfigurationManager _configMgr;
    private boolean _recursive = false;

    /* ------------------------------------------------------------ */
    protected class ScannerListener implements Scanner.DiscreteListener
    {
        /**
         * Handle a new deployment
         * 
         * @see org.mortbay.util.Scanner.FileAddedListener#fileAdded(java.lang.String)
         */
        public void fileAdded(String filename) throws Exception
        {
            deploy(filename);
        }

        /**
         * Handle a change to an existing deployment. Undeploy then redeploy.
         * 
         * @see org.mortbay.util.Scanner.FileChangedListener#fileChanged(java.lang.String)
         */
        public void fileChanged(String filename) throws Exception
        {
            redeploy(filename);
        }

        /**
         * Handle an undeploy.
         * 
         * @see org.mortbay.util.Scanner.FileRemovedListener#fileRemoved(java.lang.String)
         */
        public void fileRemoved(String filename) throws Exception
        {
            undeploy(filename);
        }
        public String toString()
        {
            return "ContextDeployer$Scanner";
        }
    }


    public AndroidContextDeployer() throws Exception
    {
        super();
        _scanner = new Scanner();
        _attributes = new AttributesMap();
    }
    
    public void setAttribute (String name, Object value)
    {
        _attributes.setAttribute(name, value);
    }

    public Object getAttribute (String name)
    {
        return _attributes.getAttribute(name);
    }
    
  
  
    /* ------------------------------------------------------------ */
    /**
     * @return the ContextHandlerColletion to which to deploy the contexts
     */
    public ContextHandlerCollection getContexts()
    {
        return _contexts;
    }

    /* ------------------------------------------------------------ */
    /**
     * Associate with a {@link ContextHandlerCollection}.
     * 
     * @param contexts
     *            the ContextHandlerColletion to which to deploy the contexts
     */
    public void setContexts(ContextHandlerCollection contexts)
    {
        if (isStarted()||isStarting())
            throw new IllegalStateException("Cannot set Contexts after deployer start");
        _contexts=contexts;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param seconds
     *            The period in second between scans for changed configuration
     *            files. A zero or negative interval disables hot deployment
     */
    public void setScanInterval(int seconds)
    {
        if (isStarted()||isStarting())
            throw new IllegalStateException("Cannot change scan interval after deployer start");
        _scanInterval=seconds;
    }

    /* ------------------------------------------------------------ */
    public int getScanInterval()
    {
        return _scanInterval;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param dir
     * @throws Exception
     */
    public void setConfigurationDir(String dir) throws Exception
    {
        setConfigurationDir(Resource.newResource(dir));
    }

    /* ------------------------------------------------------------ */
    /**
     * @param file
     * @throws Exception
     */
    public void setConfigurationDir(File file) throws Exception
    {
        setConfigurationDir(Resource.newResource(file.toURL()));
    }

    /* ------------------------------------------------------------ */
    /**
     * @param resource
     */
    public void setConfigurationDir(Resource resource)
    {
        if (isStarted()||isStarting())
            throw new IllegalStateException("Cannot change hot deploy dir after deployer start");
        _configurationDir=resource;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param directory
     */
    public void setDirectory(String directory) throws Exception
    {
                setConfigurationDir(directory);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getDirectory()
    {
        return getConfigurationDir().getName();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public Resource getConfigurationDir()
    {
        return _configurationDir;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param configMgr
     */
    public void setConfigurationManager(ConfigurationManager configMgr)
    {
        _configMgr=configMgr;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public ConfigurationManager getConfigurationManager()
    {
        return _configMgr;
    }

    
    public void setRecursive (boolean recursive)
    {
        _recursive=recursive;
    }
    
    public boolean getRecursive ()
    {
        return _recursive;
    }
    
    public boolean isRecursive()
    {
        return _recursive;
    }
    /* ------------------------------------------------------------ */
    public void deploy(String filename) throws Exception
    {
        ContextHandler context=createContext(filename);
        Log.info("Deploy "+filename+" -> "+ context);
        _contexts.addHandler(context);
        _currentDeployments.put(filename,context);
        if (_contexts.isStarted())
            context.start();
    }

    /* ------------------------------------------------------------ */
    public void undeploy(String filename) throws Exception
    {
        ContextHandler context=(ContextHandler)_currentDeployments.get(filename);
        Log.info("Undeploy "+filename+" -> "+context);
        if (context==null)
            return;
        context.stop();
        _contexts.removeHandler(context);
        _currentDeployments.remove(filename);
    }

    /* ------------------------------------------------------------ */
    public void redeploy(String filename) throws Exception
    {
        undeploy(filename);
        deploy(filename);
    }

    /* ------------------------------------------------------------ */
    /**
     * Start the hot deployer looking for webapps to deploy/undeploy
     * 
     * @see org.mortbay.component.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        if (_configurationDir==null)
            throw new IllegalStateException("No configuraition dir specified");

        if (_contexts==null)
            throw new IllegalStateException("No context handler collection specified for deployer");

        _scanner.setScanDir(_configurationDir.getFile());
        _scanner.setScanInterval(getScanInterval());
        _scanner.setRecursive(_recursive); //only look in the top level for deployment files?
        // Accept changes only in files that could be a deployment descriptor
        _scanner.setFilenameFilter(new FilenameFilter()
        {
            public boolean accept(File dir, String name)
            {
                try
                {
                    if (name.endsWith(".xml")&&dir.equals(getConfigurationDir().getFile()))
                        return true;
                    return false;
                }
                catch (IOException e)
                {
                    Log.warn(e);
                    return false;
                }
            }
        });
        _scannerListener=new ScannerListener();
        _scanner.addListener(_scannerListener);
        _scanner.scan();
        _scanner.start();
        _contexts.getServer().getContainer().addBean(_scanner);
    }

    /* ------------------------------------------------------------ */
    /**
     * Stop the hot deployer.
     * 
     * @see org.mortbay.component.AbstractLifeCycle#doStop()
     */
    protected void doStop() throws Exception
    {
        _scanner.removeListener(_scannerListener);
        _scanner.stop();
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a WebAppContext for the webapp being hot deployed, then apply the
     * xml config file to it to configure it.
     * 
     * @param filename
     *            the config file found in the hot deploy directory
     * @return
     * @throws Exception
     */
    protected ContextHandler createContext(String filename) throws Exception
    {
        // The config file can call any method on WebAppContext to configure
        // the webapp being deployed.
        Resource resource = Resource.newResource(filename);        
        if (!resource.exists())
            return null;

        XmlConfiguration xmlConfiguration=new XmlConfiguration(resource.getURL());
        HashMap properties = new HashMap();
        properties.put("Server", _contexts.getServer());
        if (_configMgr!=null)
            properties.putAll(_configMgr.getProperties());
           
        xmlConfiguration.setProperties(properties);
        ContextHandler context=(ContextHandler)xmlConfiguration.configure();
        
        Enumeration names = _attributes.getAttributeNames();
        while (names.hasMoreElements())
        {
            String name = (String)names.nextElement();
            context.setAttribute(name, _attributes.getAttribute(name));
        }
      
        return context;
    }    
}
