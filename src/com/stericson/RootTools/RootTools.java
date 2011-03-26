package com.stericson.RootTools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class RootTools {

    //--------------------
    //# Public Variables #
    //--------------------


    //----------------------
    //# Internal Variables #
    //----------------------

    protected static String TAG = "RootTools";
    protected static boolean accessGiven = false;

    //------------------
    //# Public Methods #
    //------------------

    /**
     * This will launch the Android market looking for BusyBox
     * 
     * @param activity  pass in your Activity
     */
    public static void offerBusyBox(Activity activity) {
        Log.i(TAG, "Launching Market for BusyBox");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox"));
        activity.startActivity(i);
    }

    /**
     * This will launch the Android market looking for BusyBox,
     * but will return the intent fired and starts the activity with startActivityForResult
     * 
     * @param activity      pass in your Activity
     * 
     * @param requestCode   pass in the request code
     * 
     * @return              intent fired
     */
    public static Intent offerBusyBox(Activity activity, int requestCode) {
        Log.i(TAG, "Launching Market for BusyBox");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox"));
        activity.startActivityForResult(i, requestCode);
        return i;
    }

    /**
     * This will launch the Android market looking for SuperUser
     * 
     * @param activity  pass in your Activity
     */
    public static void offerSuperUser(Activity activity) {
        Log.i(TAG, "Launching Market for SuperUser");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=com.noshufou.android.su"));
        activity.startActivity(i);
    }

    /**
     * This will launch the Android market looking for SuperUser,
     * but will return the intent fired and starts the activity with startActivityForResult
     * 
     * @param activity      pass in your Activity
     * 
     * @param requestCode   pass in the request code
     * 
     * @return              intent fired
     */
    public static Intent offerSuperUser(Activity activity, int requestCode) {
        Log.i(TAG, "Launching Market for SuperUser");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=com.noshufou.android.su"));
        activity.startActivityForResult(i, requestCode);
        return i;
    }

    /**
     * @return  <code>true</code> if su was found.
     */
    public static boolean isRootAvailable() {
        Log.i(TAG, "Checking for Root binary");
        String[] places = { "/system/bin/", "/system/xbin/",
                "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/" };
        for (String where : places) {
            File file = new File(where + "su");
            if (file.exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return  <code>true</code> if BusyBox was found.
     */
    public static boolean isBusyboxAvailable() {
        Log.i(TAG, "Checking for BusyBox");
        File tmpDir = new File("/data/local/tmp");
        if (!tmpDir.exists()) {
            doExec(new String[] { "mkdir /data/local/tmp" });
        }
        Set<String> tmpSet = new HashSet<String>();
        //Try to read from the file.
        LineNumberReader lnr = null;
        try {
            doExec(new String[] { "cp /init.rc /data/local/tmp",
                    "chmod 0777 /data/local/tmp/init.rc"});
            lnr = new LineNumberReader( new FileReader( "/data/local/tmp/init.rc" ) );
            String line;
            while( (line = lnr.readLine()) != null ){
                if (line.contains("export PATH")) {
                    int tmp = line.indexOf("/");
                    tmpSet = new HashSet<String>(Arrays.asList(line.substring(tmp).split(":")));
                    for(String paths : tmpSet) {
                        File file = new File(paths + "/busybox");
                        if (file.exists()) {
                            Log.i(TAG, "Found BusyBox!");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "BusyBox was not found, some error happened!");
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * @return  <code>true</code> if your app has been given root access.
     */
    public static boolean isAccessGiven() {
        Log.i(TAG, "Checking for Root access");
        accessGiven = false;
        doExec(new String[] { "id" });

        if (accessGiven) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if there is enough Space on SDCard
     * 
     * @param updateSize    size to Check (long)
     *
     * @return              <code>true</code> if the Update will fit on SDCard,
     *                      <code>false</code> if not enough space on SDCard.
     *		                Will also return <code>false</code>,
     *		                if the SDCard is not mounted as read/write
     */
    public static boolean hasEnoughSpaceOnSdCard(long updateSize) {
        Log.i(TAG, "Checking SDcard size and that it is mounted as RW");
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return (updateSize < availableBlocks * blockSize);
    }

    /**
     * Sends one shell command as su (attempts to)
     * 
     * @param command   command to send to the shell
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
     *
     * @throws RootToolsException
     */
    public static List<String> sendShell(String command, Result result)
            throws IOException, InterruptedException, RootToolsException {
        return sendShell(new String[] { command}, 0, result);
    }

    /**
     * Sends one shell command as su (attempts to)
     *
     * @param command   command to send to the shell
     *
     * @return          a LinkedList containing each line that was returned by the shell
     *                  after executing or while trying to execute the given commands.
     *                  You must iterate over this list, it does not allow random access,
     *                  so no specifying an index of an item you want,
     *                  not like you're going to know that anyways.
     *
     * @throws InterruptedException
     *
     * @throws IOException
     */
    public static List<String> sendShell(String command)
            throws IOException, InterruptedException, RootToolsException {
        return sendShell(command, null);
    }

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
    public static List<String> sendShell(String[] commands, int sleepTime, Result result)
            throws IOException, InterruptedException, RootToolsException {
        Log.i(TAG, "Sending " + commands.length + " shell command" + (commands.length>1?"s":""));
        List<String> response = null;
        if(null == result) {
            response = new LinkedList<String>();
        }

        Process process = null;
        DataOutputStream os = null;
        InputStreamReader osRes = null;

        try {
            process = Runtime.getRuntime().exec("su");
            if(null != result) {
                result.setProcess(process);
            }
            os = new DataOutputStream(process.getOutputStream());
            osRes = new InputStreamReader(process.getInputStream());
            BufferedReader reader = new BufferedReader(osRes);
            // Doing Stuff ;)
            for (String single : commands) {
                os.writeBytes(single + "\n");
                os.flush();
                Thread.sleep(sleepTime);
            }

            os.writeBytes("exit \n");
            os.flush();

            String line = reader.readLine();

            while (line != null) {
                if(null == result) {
                    response.add(line);
                } else {
                    result.process(line);
                }
                line = reader.readLine();
            }
        }
        catch (Exception ex) {
            if(null != result) {
                result.onFailure(ex);
            }
        }
        finally {
            int diag = process.waitFor();
            if(null != result) {
                result.onComplete(diag);
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
                //return what we have
                return response;
            }
            return response;
        }
    }

    /**
     * Sends several shell command as su (attempts to)
     *
     * @param commands  array of commands to send to the shell
     *
     * @param sleepTime time to sleep between each command, delay.
     *
     * @return          a LinkedList containing each line that was returned by the shell
     *                  after executing or while trying to execute the given commands.
     *                  You must iterate over this list, it does not allow random access,
     *                  so no specifying an index of an item you want,
     *                  not like you're going to know that anyways.
     *
     * @throws InterruptedException
     *
     * @throws IOException
     */
    public static List<String> sendShell(String[] commands, int sleepTime)
            throws IOException, InterruptedException, RootToolsException {
        return sendShell(commands, sleepTime,  null);
    }

    //-------------
    //# Remounter #
    //-------------

    /**
     * This will take a path, which can contain the file name as well,
     * and attempt to remount the underlying partition.
     * 
     * For example, passing in the following string:
     * "/system/bin/some/directory/that/really/would/never/exist"
     * will result in /system ultimately being remounted.
     * However, keep in mind that the longer the path you supply, the more work this has to do,
     * and the slower it will run.
     * 
     * @param file      file path
     * 
     * @param mountType mount type: pass in RO (Read only) or RW (Read Write)
     * 
     * @return          a <code>boolean</code> which indicates whether or not the partition
     *                  has been remounted as specified.
     */

    public static boolean remount(String file, String mountType) {
        //if the path has a trailing slash get rid of it.
        if (file.endsWith("/")) {
            file = file.substring(0, file.lastIndexOf("/"));
        }
        //Make sure that what we are trying to remount is in the mount list.
        boolean foundMount = false;
        while (!foundMount) {
            try {
                for (Mount mount : getMounts()) {
                    if (file.equals(mount.mountPoint.toString())) {
                        foundMount = true;
                        break;
                    }
                }
            }
            catch (Exception e) {
                return false;
            }
            if (!foundMount) {
                try {
                    file = (new File(file).getParent()).toString();
                }
                catch (Exception e) {
                    return false;
                }
            }
        }
        Mount mountPoint = findMountPointRecursive(file);

        Log.i(TAG, "Remounting " + mountPoint.mountPoint.getAbsolutePath() + " as " + mountType);
        final boolean isMountMode = mountPoint.flags.contains(mountType);

        if ( isMountMode ) {
            doExec(new String[] {
                    String.format(
                            "mount -o remount,%s %s %s",
                            mountType,
                            mountPoint.device.getAbsolutePath(),
                            mountPoint.mountPoint.getAbsolutePath() )
                    }
            );
            mountPoint = findMountPointRecursive(file);
        }

        if ( mountPoint.flags.contains(mountType) ) {
            return false;
        } else {
            return false;
        }
    }

    protected static class Mount {
        final File device;
        final File mountPoint;
        final String type;
        final Set<String> flags;

        Mount(File device, File path, String type, String flagsStr) {
            this.device = device;
            this.mountPoint = path;
            this.type = type;
            this.flags = new HashSet<String>( Arrays.asList(flagsStr.split(",")));
        }

        @Override
        public String toString() {
            return String.format( "%s on %s type %s %s", device, mountPoint, type, flags );
        }
    }

    protected static Mount findMountPointRecursive(String file) {
        try {
            ArrayList<Mount> mounts = getMounts();
            for( File path = new File(file); path != null; ) {
                for(Mount mount : mounts ) {
                    if ( mount.mountPoint.equals( path )) {
                        return mount;
                    }
                }
            }
            return null;
        }
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    protected static ArrayList<Mount> getMounts() throws FileNotFoundException, IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader( new FileReader( "/proc/mounts" ) );
            String line;
            ArrayList<Mount> mounts = new ArrayList<Mount>();
            while( (line = lnr.readLine()) != null ){
                String[] fields = line.split(" ");
                mounts.add( new Mount(
                        new File(fields[0]), // device
                        new File(fields[1]), // mountPoint
                        fields[2], // fstype
                        fields[3] // flags
                ) );
            }
            return mounts;
        }
        finally {
            //no need to do anything here.
        }
    }

    //--------------------
    //# Internal methods #
    //--------------------

    protected static void doExec(String[] commands) {
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
                            accessGiven = true;
                            Log.i(TAG, "Access Given");
                            break;
                        }
                    }
                    if (!accessGiven) {
                        Log.i(TAG, "Access Denied?");
                    }
                }
                line = reader.readLine();
            }

            process.waitFor();

        } catch (Exception e) {
            Log.d(TAG,
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
                Log.d(TAG,
                        "Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static abstract class Result {
        private Process         process = null;
        private Serializable    data    = null;
        private int             error   = 0;

        public abstract void process(String line) throws Exception;
        public abstract void onFailure(Exception ex);
        public abstract void onComplete(int diag);

        protected Result    setProcess(Process process) { this.process = process; return this; }
        public Process      getProcess()                { return process; }
        protected Result    setData(Serializable data)  { this.data = data; return this; }
        public Serializable getData()                   { return data; }
        protected Result    setError(int error)         { this.error = error; return this; }
        public int          getError()                  { return error; }
    }

    public static class RootToolsException extends Exception {
        public RootToolsException(Throwable th) {
            super(th);
        }
    }
}
