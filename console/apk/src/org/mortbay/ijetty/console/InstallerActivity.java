package org.mortbay.ijetty.console;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;



public class InstallerActivity extends Activity
{
    private static final String TAG = "Console.Inst";
    
    
    public static final String WEBAPPS = "webapps";
    public static final String CONSOLE = "console";
    public static final String JETTY = "jetty";
    
    public static final int CONFIRM_DIALOG_ID = 1;
    public static final int ERROR_DIALOG_ID = 2;   
    public static final int PROGRESS_DIALOG_ID = 3;
    public static final int FINISH_DIALOG_ID = 4;
    
    private Button installButton;
    private TextView versionView;
    private ProgressDialog progressDialog;
    private String error;
    private boolean cleanInstall = false;
    
    
    class InstallerThread extends Thread
    { 
        private Handler handler;
        private boolean cleanInstall;


        public InstallerThread(Handler h) 
        {
            handler = h;
        }

        public void sendProgressUpdate (int prog)
        { 
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("prog", prog);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        public void run ()
        {       
            if (cleanInstall)
                delete(getWebApp());

            sendProgressUpdate(30);

            //extract war file from apk
            InputStream is = getResources().openRawResource(R.raw.console);
            long count = 0;
            try
            {
                int done = -1;
                do
                {
                    done = is.read();
                    if (done >= 0)
                        ++count;
                    
                }while (done >=0);
            }
            catch (IOException e)
            {
                Log.e(TAG, "IO error", e);
            }

            Log.i(TAG, "console.war size="+count);
            
            //TODO 
            sendProgressUpdate(60);

            //unpack war to jetty webapps dir
            //TODO
            sendProgressUpdate(100);
        }
    }

    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.installer);
        
        installButton = (Button)findViewById(R.id.install);
        versionView = (TextView)findViewById(R.id.version);
        progressDialog = null;
        
        //version info
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(),0);        
            versionView.setText(Html.fromHtml(String.format("Version %s (%s)",pi.versionName,pi.versionCode)));
        }
        catch (NameNotFoundException e)
        {
            versionView.setText("Version ? (?)");
        }
        
        
        //install button
        installButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                File jettyDir = getJettyInstallDir();
                if (jettyDir == null)
                    showError(getString(R.string.notInstalled));
                else
                {
                    //if the webapp does not already exist, go ahead and install it
                    File webapp = getWebApp();
                    if (webapp == null)
                        install();
                    else
                    {
                        //existing console webapp installed, check if the user wants to reinstall
                        showDialog(CONFIRM_DIALOG_ID);
                    }
                }
            }
        });
    }
    
    
    
    
    
    /** 
     * Set up an instance of a Dialog
     * 
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id)
        {
            case CONFIRM_DIALOG_ID:
            {
                ((AlertDialog)dialog).setMessage(getString(R.string.alreadyInstalled));
                break;
            }
            case ERROR_DIALOG_ID:
            {
                ((AlertDialog)dialog).setMessage((error == null?getString(R.string.defaultError):error));
                break;
            }
        }
    }





    /** 
     * Create an appropriate dialog.
     * 
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        
        switch (id)
        {
            case CONFIRM_DIALOG_ID:
            {
                //Dialog asking user to confirm to proceed with the installation after finding an already existing webapp
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Stuff");
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() 
                {
                           public void onClick(DialogInterface dialog, int id) 
                           {
                               cleanInstall = true;
                                install();
                           }
                       });
                builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() 
                {
                           public void onClick(DialogInterface dialog, int id) 
                           {
                                dialog.dismiss();
                           }
                       });
                dialog = builder.create();
                break;
            }
            case ERROR_DIALOG_ID:
            {
                //Dialog informing user a fatal error has occurred.
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("");
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() 
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        error = null;
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            }
            case PROGRESS_DIALOG_ID:
            {
                //Dialog showing user progress of installation.
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage(getString(R.string.installing));
                dialog = progressDialog;
                break;
            }
            case FINISH_DIALOG_ID:
            {
                //Dialog showing user install complete
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.success));
                builder.setCancelable(false);
                builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener()
                {
                    
                    public void onClick(DialogInterface dialog, int which)
                    {
                       dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            }
        }
        return dialog;
    }


    /**
     * Inform the user an error has occurred.
     * @param string
     */
    public void showError (String string)
    {
       error = ""+string;
       Log.e(TAG, error);
       showDialog(ERROR_DIALOG_ID);
    }
    
    
    
    /**
     * Check to see if jetty has been installed.
     * @return File of jetty install dir or null if not installed
     */
    public File getJettyInstallDir ()
    {    	
    	File jettyDir = new File(Environment.getExternalStorageDirectory(), JETTY); 
    	if (!jettyDir.exists())
    		return null;
    	return jettyDir;
    }
    
    
    
    /**
     * @return the File of the existing unpacked webapp or null
     */
    public File getWebApp ()
    {
        File jettyDir = getJettyInstallDir();
        if (jettyDir == null)
            return null;
        
        File webappsDir = new File(jettyDir, WEBAPPS);
        if (!webappsDir.exists())
            return null;
        
        File webapp = new File (webappsDir, CONSOLE);
        if (!webapp.exists())
            return null;
        
        return webapp;
    }
    
    
    /**
     * Delete the existing unpacked console webapp
     * @param webapp
     */
    public void delete (File webapp)
    {
        if (webapp.isDirectory())
        {
            File[] files = webapp.listFiles();
            for (File f:files)
            {
                delete(f);
            }
            webapp.delete();
        }
        else
            webapp.delete();
    }

    
    /**
     * Begin the installation
     */
    public void install ()
    {
        showDialog(PROGRESS_DIALOG_ID);
        InstallerThread thread = new InstallerThread(new Handler()
        {
            public void handleMessage(Message msg) 
            {
                int total = msg.getData().getInt("prog");
                progressDialog.setProgress(total);
                if (total >= 100)
                {
                    progressDialog.dismiss();                    
                    showDialog(FINISH_DIALOG_ID);
                }
            }; 
        });
        thread.start();
    }
}
