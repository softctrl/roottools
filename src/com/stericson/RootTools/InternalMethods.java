package com.stericson.RootTools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import android.util.Log;

//no modifier, this is package-private which means that no one but the library can access it.
class InternalMethods {
	
    //--------------------
    //# Internal methods #
    //--------------------

    static private InternalMethods instance_;

    static protected InternalMethods instance() {
        if(null == instance_) {
            instance_ = new InternalMethods();
        }
        return instance_;
    }

    private InternalMethods() {
        super();
    }

    protected void doExec(String[] commands) {
        Process process = null;
        DataOutputStream os = null;
        InputStreamReader osRes = null;

        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            osRes = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(osRes);

            // Doing Stuff ;)
            for (String single : commands) {
                os.writeBytes(single + "\n");
                os.flush();
            }


            os.writeBytes("exit \n");
            os.flush();

            String line = reader.readLine();

            while (line != null) {
                if (commands[0].equals("id")) {
                    Set<String> ID = new HashSet<String>(Arrays.asList(line.split(" ")));
                    for (String id : ID) {
                        if (id.toLowerCase().contains("uid=") && id.toLowerCase().contains("root")) {
                            InternalVariables.accessGiven = true;
                            Log.i(InternalVariables.TAG, "Access GInternalVariablesen");
                            break;
                        }
                    }
                    if (!InternalVariables.accessGiven) {
                        Log.i(InternalVariables.TAG, "Access Denied?");
                    }
                }
                line = reader.readLine();
            }

            process.waitFor();

        } catch (Exception e) {
            Log.d(InternalVariables.TAG,
                    "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (osRes != null) {
                    osRes.close();
                }
                process.destroy();
            } catch (Exception e) {
                Log.d(InternalVariables.TAG,
                        "Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
