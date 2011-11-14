package com.stericson.RootTools;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class Executer {

    //------------
    //# Executer #
    //------------

    /**
     * Sends several shell command as su (attempts to)
     *
     * @param commands  array of commands to send to the shell
     *
     * @param sleepTime time to sleep between each command, delay.
     *
     * @param result    injected result object that implements the Result class
     *
     * @return          a <code>LinkedList</code> containing each line that was returned
     *                  by the shell after executing or while trying to execute the given commands.
     *                  You must iterate over this list, it does not allow random access,
     *                  so no specifying an index of an item you want,
     *                  not like you're going to know that anyways.
     *
     * @throws InterruptedException
     *
     * @throws IOException
     */
    public List<String> sendShell(String[] commands, int sleepTime, IResult result)
            throws IOException, InterruptedException, RootToolsException {
        RootTools.log(InternalVariables.TAG, "Sending " + commands.length + " shell command" + (commands.length>1?"s":""));
        List<String> response = null;
        if(null == result) {
            response = new LinkedList<String>();
        }

        Process process = null;
        DataOutputStream os = null;
        InputStreamReader osRes = null,
        				  osErr = null;

        try {
            process = Runtime.getRuntime().exec("su");
            if(null != result) {
                result.setProcess(process);
            }
            os = new DataOutputStream(process.getOutputStream());
            osRes = new InputStreamReader(process.getInputStream());
            osErr = new InputStreamReader(process.getErrorStream());
            BufferedReader reader = new BufferedReader(osRes);
            BufferedReader reader_error = new BufferedReader(osErr);
            // Doing Stuff ;)
            for (String single : commands) {
                os.writeBytes(single + "\n");
                os.flush();
                Thread.sleep(sleepTime);
            }

            os.writeBytes("exit \n");
            os.flush();

            String line = reader.readLine();
            String line_error = reader_error.readLine();

            while (line != null) {
                if(null == result) {
                    response.add(line);
                } else {
                    result.process(line);
                }

                RootTools.log(line);
                line = reader.readLine();
            }
            
            while (line_error != null) {
                if(null == result) {
                    response.add(line_error);
                } else {
                    result.processError(line_error);
                }
                
                RootTools.log(line_error);
                line_error = reader_error.readLine();
            }

        }
        catch (Exception ex) {
            if(null != result) {
                result.onFailure(ex);
            }
        }
        finally {
            if (process != null) {
                int diag = process.waitFor();
                if(null != result) {
                    result.onComplete(diag);
                    response.add(Integer.toString(diag));
                    RootTools.lastExitCode = diag;
                }
            }

            try {
                if (os != null) {
                    os.close();
                }
                if (osRes != null) {
                    osRes.close();
                }
                process.destroy();
            } catch (Exception e) {
                Log.e(InternalVariables.TAG, "Catched Exception in finally block!");
                e.printStackTrace();
            }
        }
        
        return response;
    }
}
