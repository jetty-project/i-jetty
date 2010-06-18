package org.googlecode.ijetty.debug.contacts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Contacts;
import android.util.Log;
import android.widget.TextView;

public class ContactsDebugActivity extends Activity {
    private static final String TAG = "ContactsDebug";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        StringBuilder str = new StringBuilder();
        str.append("\nBuild.BOARD=").append(Build.BOARD);
        str.append("\nBuild.BRAND=").append(Build.BRAND);
        str.append("\nBuild.CPU_ABI=").append(Build.CPU_ABI);
        str.append("\nBuild.DEVICE=").append(Build.DEVICE);
        str.append("\nBuild.FINGERPRINT=").append(Build.FINGERPRINT);
        str.append("\nBuild.HOST=").append(Build.HOST);
        str.append("\nBuild.ID=").append(Build.ID);
        str.append("\nBuild.MANUFACTURER=").append(Build.MANUFACTURER);
        str.append("\nBuild.MODEL=").append(Build.MODEL);
        str.append("\nBuild.PRODUCT=").append(Build.PRODUCT);
        str.append("\nBuild.TAGS=").append(Build.TAGS);
        str.append("\nBuild.TYPE=").append(Build.TYPE);
        str.append("\nBuild.USER=").append(Build.USER);
        str.append("\nBuild.VERSION.RELEASE=").append(Build.VERSION.RELEASE);
        str.append("\nBuild.VERSION.SDK_INT=").append(Build.VERSION.SDK_INT);
        str.append("\n");
        // Do not share this value with strangers
        // str.append("\n.getUniqueID=").append(AndroidInfo.getUniqueDeviceID(this));
        str.append("\n.getDeviceModel=").append(AndroidInfo.getDeviceModel());
        str.append("\n.getOSVersion=").append(AndroidInfo.getOSVersion());
        str.append("\n");
        str.append("\n.getApplicationLabel=").append(AndroidInfo.getApplicationLabel(this));
        str.append("\n");

        str.append("\n--Contacts ContentProvider--");
        Cursor c = null;
        try {
            c = getContentResolver().query(Contacts.People.CONTENT_URI, null, null, null, null);

            c.moveToFirst();

            str.append("\nRows of Data=").append(c.getCount());
            int columnCount = c.getColumnCount();
            str.append("\nColumn.count=").append(columnCount);

            Set<String> colNames = new TreeSet<String>();
            for (int i = 0; i < columnCount; i++) {
                colNames.add(c.getColumnName(i));
            }

            int i = 0;
            for (String name : colNames) {
                str.append("\n[").append(i++).append("] ").append(name);
            }
        } catch (Throwable t) {
            str.append("\nERROR: " + t);
            Log.e(TAG, "Error identifying Contacts structure: " + t, t);
            if (c != null) {
                c.close();
            }
        }

        TextView text = (TextView) findViewById(R.id.info);
        text.setText(str);

        File file = writeToSdCard(str);
        text.append("\n Wrote output to file: " + file);
    }

    private File writeToSdCard(StringBuilder str) {
        File outputFile = new File(Environment.getExternalStorageDirectory(), "contacts-debug.log");

        FileWriter out = null;
        try {
            out = new FileWriter(outputFile);
            out.write(str.toString());
            out.flush();
        } catch (IOException e) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                    /* ignore */
                }
            }
        }

        return outputFile;
    }
}