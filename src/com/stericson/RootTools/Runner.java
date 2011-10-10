package com.stericson.RootTools;

import java.io.IOException;

import android.content.Context;
import android.util.Log;

class Runner extends Thread {

    private static final String LOG_TAG = "RootTools::Runner";

    Context context;
    String binaryName;
    String parameter;

    public Runner(Context context, String binaryName, String parameter) {
        this.context = context;
        this.binaryName = binaryName;
        this.parameter = parameter;
    }

    public void run() {
        String privateFilesPath = null;
        try {
            // /data/data/app.package/files/
            privateFilesPath = context.getFilesDir().getCanonicalPath();
        } catch (IOException e) {
            if (RootTools.debugMode) {
                Log.e(LOG_TAG, "Problem occured while trying to locate private files directory!");
            }
            e.printStackTrace();
        }
        if (privateFilesPath != null) {
            InternalMethods.instance().doExec(
                    new String[] { privateFilesPath + "/" + binaryName + " " + parameter });
        }
    }

}