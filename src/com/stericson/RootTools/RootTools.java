package com.stericson.RootTools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class RootTools {

    /*
     *This class is the gateway to every functionality within the RootTools library.
     *The developer should only have access to this class and this class only.
     *This means that this class should be the only one to be public.
     *The rest of the classes within this library must not have the public modifier.
     *
     *All methods and Variables that the developer may need to have access to should be here.
     *
     *If a method, or a specific functionality, requires a fair amount of code, or work to be done,
     *then that functionality should probably be moved to its own class and the call to it done here.
     *For examples of this being done, look at the remount functionality.
     */

    //--------------------
    //# Public Variables #
    //--------------------

    public static boolean debugMode = false;
    public static List<String> lastFoundBinaryPaths = new ArrayList<String>();
    public static int lastExitCode;
    public static String utilPath;

    //---------------------------
    //# Public Variable Getters #
    //---------------------------

    //------------------
    //# Public Methods #
    //------------------

    /**
     * This will return to you a string to be used in your shell commands which will represent the valid
     * working toolbox with correct permissions. For instance, if Busybox is available it will return
     * "busybox", if busybox is not available but toolbox is then it will return "toolbox"
     * 
     *@return String that indicates the available toolbox to use for accessing applets.
     */
    public static String getWorkingToolbox()
    {
    	if (RootTools.checkUtil("busybox"))
    	{
    		return "busybox";
    	}
    	else if (RootTools.checkUtil("toolbox"))
    	{
    		return "toolbox";
    	}
    	else
    	{
    		return "";
    	}
    }

    /**
     * This will check an array of binaries, determine if they exist and determine that it has
     * either ther permissions 755, 775, or 777. If an applet is not setup correctly
     * it will try and fix it. (This is for Busybox applets or Toolbox applets)
     * 
     *@param String Name of the utility to check.
     *
     *@throws Exception if the operation cannot be completed.
     *
     *@return boolean to indicate whether the operation completed. 
     *Note that this is not indicative of whether the problem was fixed, just that the method did not
     *encounter any exceptions.
     */
	public static boolean checkUtils(String[] utils) throws Exception {
		
		for (String util : utils)
		{
			if (!checkUtil(util))
			{
				if (checkUtil("busybox"))
				{
					fixUtil(util, utilPath);
				}
				else if (checkUtil("toolbox"))
				{
					fixUtil(util, utilPath);
				}
				else
				{
					return false;
				}
			}
		}
		
		return true;
	}

    /**
     * This will check a given binary, determine if it exists and determine that it has
     * either the permissions 755, 775, or 777.
     * 
     *
     *@param String Name of the utility to check.
     *
     *@return boolean to indicate whether the binary is installed and has appropriate permissions.
     */
	public static boolean checkUtil(String util)
	{
		if (RootTools.findBinary(util))
		{
			for (String path : RootTools.lastFoundBinaryPaths)
			{
				Permissions permissions = RootTools.getFilePermissions(path + "/" + util);
				
				if (permissions != null)
				{
					int permission = permissions.getPermissions();
					
					if (permission == 755 || permission == 777 || permission == 775)
					{
						utilPath = path + "/" + util;
						return true;
					}
				}
			}
		}
		
		return false;
	}


    /**
     * This will try and fix a given binary. (This is for Busybox applets or Toolbox applets)
     * By "fix", I mean it will try and symlink the binary from either toolbox or Busybox
     * and fix the permissions if the permissions are not correct.
     *
     *@param String Name of the utility to fix.
     *@param String path to the toolbox that provides ln, rm, and chmod.
     *This can be a blank string, a path to a binary that will provide these, or you can use RootTools.getWorkingToolbox()
     */
	public static void fixUtil(String util, String utilPath)
	{
		try
		{
			RootTools.remount("/system", "rw");
			
			if (RootTools.findBinary(util))
			{
				for (String path : RootTools.lastFoundBinaryPaths)
					RootTools.sendShell(utilPath + " rm " + path + "/" + util, InternalVariables.timeout);
				
				RootTools.sendShell(new String[] {	utilPath + " ln -s " + utilPath + " /system/bin/" + util,
													utilPath + " chmod 0755 /system/bin/" + util}, 10, InternalVariables.timeout);
			}
			
			RootTools.remount("/system", "ro");
		}
		catch (Exception e) {}
	}	
	
    /**
     * This will return the environment variable $PATH
     *
     * @return <code>Set<String></code> A Set of Strings representing the environment variable $PATH
     * @throws Exception if we cannot return the $PATH variable
     */
    public static Set<String> getPath() throws Exception {
        if (InternalVariables.path != null) {
            return InternalVariables.path;
        } else {
            if (new InternalMethods().returnPath()) {
                return InternalVariables.path;
            } else {
                throw new Exception();
            }
        }
    }

    /**
     * This will return an ArrayList of the class Mount.
     * The class mount contains the following property's:
     * device
     * mountPoint
     * type
     * flags
     * <p/>
     * These will provide you with any information you need to work with the mount points.
     *
     * @return <code>ArrayList<Mount></code> an ArrayList of the class Mount.
     * @throws Exception if we cannot return the mount points.
     */
    public static ArrayList<Mount> getMounts() throws Exception {
        InternalVariables.mounts = new InternalMethods().getMounts();
        if (InternalVariables.mounts != null) {
            return InternalVariables.mounts;
        } else {
            throw new Exception();
        }
    }

    /**
     * This will return an ArrayList of the class Symlink.
     * The class Symlink contains the following property's:
     * path
     * SymplinkPath
     * <p/>
     * These will provide you with any Symlinks in the given path.
     * 
     * @param The path to search for Symlinks.
     *
     * @return <code>ArrayList<Symlink></code> an ArrayList of the class Symlink.
     * @throws Exception if we cannot return the Symlinks.
     */
    public static ArrayList<Symlink> getSymlinks(String path) throws Exception {
    	new InternalMethods().doExec(new String[] { "find " + path + " -type l -exec ls -l {} \\; > /data/local/symlinks.txt;"}, -1);
        InternalVariables.symlinks = new InternalMethods().getSymLinks();
        if (InternalVariables.symlinks != null) {
            return InternalVariables.symlinks;
        } else {
            throw new Exception();
        }
    }

    /**
     * This will return a String that represent the symlink for a specified file.
     * <p/>
     * 
     * @param The file to get the Symlink for. (must have absolute path)
     *
     * @return <code>String</code> a String that represent the symlink for a specified file or
     * an empty string if no symlink exists.
     */
    public static String getSymlink(File file) {
    	RootTools.log("Looking for Symlink for " + file.toString());
    	if (file.exists())
    	{
    		RootTools.log("File exists");
    		
    		try 
    		{
				List<String> results = sendShell("ls -l " + file, InternalVariables.timeout);
				String[] symlink = results.get(0).split(" ");
				if (symlink[symlink.length - 2].equals("->"))
				{
					RootTools.log("Symlink found.");
					return symlink[symlink.length - 1];
				}
			}
    		catch (Exception e) {}
    	}

    	RootTools.log("Symlink not found");
    	return "";
    }
    
    /**
     * This will launch the Android market looking for BusyBox
     *
     * @param activity pass in your Activity
     */
    public static void offerBusyBox(Activity activity) {
        RootTools.log(InternalVariables.TAG, "Launching Market for BusyBox");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox"));
        activity.startActivity(i);
    }

    /**
     * This will launch the Android market looking for BusyBox,
     * but will return the intent fired and starts the activity with startActivityForResult
     *
     * @param activity    pass in your Activity
     * @param requestCode pass in the request code
     * @return intent fired
     */
    public static Intent offerBusyBox(Activity activity, int requestCode) {
        RootTools.log(InternalVariables.TAG, "Launching Market for BusyBox");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=stericson.busybox"));
        activity.startActivityForResult(i, requestCode);
        return i;
    }

    /**
     * This will launch the Android market looking for SuperUser
     *
     * @param activity pass in your Activity
     */
    public static void offerSuperUser(Activity activity) {
        RootTools.log(InternalVariables.TAG, "Launching Market for SuperUser");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=com.noshufou.android.su"));
        activity.startActivity(i);
    }

    /**
     * This will launch the Android market looking for SuperUser,
     * but will return the intent fired and starts the activity with startActivityForResult
     *
     * @param activity    pass in your Activity
     * @param requestCode pass in the request code
     * @return intent fired
     */
    public static Intent offerSuperUser(Activity activity, int requestCode) {
        RootTools.log(InternalVariables.TAG, "Launching Market for SuperUser");
        Intent i = new Intent(
                Intent.ACTION_VIEW, Uri.parse("market://details?id=com.noshufou.android.su"));
        activity.startActivityForResult(i, requestCode);
        return i;
    }

    /**
     * @return  <code>true</code> if su was found.
     */
    public static boolean isRootAvailable() {
        return findBinary("su");
    }

    /**
     * @return  <code>true</code> if BusyBox was found.
     */
    public static boolean isBusyboxAvailable() {
        return findBinary("busybox");
    }

    /**
     * 
     * @param binaryName String that represent the binary to find.
     * 
     * @return  <code>true</code> if the specified binary was found.
     * Also, the path the binary was found at can be retrieved via the 
     * variable lastFoundBinaryPath, if the binary was found in more than
     * one location this will contain all of these locations.
     * 
     */
    public static boolean findBinary(String binaryName) {
    	
    	boolean found = false;
    	lastFoundBinaryPaths.clear();
        
    	RootTools.log(InternalVariables.TAG, "Checking for " + binaryName);
        try 
        {
            for(String paths : getPath()) {
                File file = new File(paths + "/" + binaryName);
                if (file.exists()) {
                    log(binaryName + " was found here: " + paths);
                    lastFoundBinaryPaths.add(paths);
                    found = true;
                }
                else
                {
                	log(binaryName + " was NOT found here: " + paths);
                }
            }
        } 
        catch (TimeoutException ex)
        {
            RootTools.log(InternalVariables.TAG, "TimeoutException!!!");        	
        }
        catch (Exception e)
        {
            RootTools.log(InternalVariables.TAG, binaryName + " was not found, more information MAY be available with Debugging on.");
        }
        
        if (!found)
        {
	        RootTools.log(InternalVariables.TAG, "Trying second method");
	        RootTools.log(InternalVariables.TAG, "Checking for " + binaryName);
	        String[] places = { "/sbin/", "/system/bin/", "/system/xbin/",
	                "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
	        for (String where : places) {
	            File file = new File(where + binaryName);
	            if (file.exists()) {
	                log(binaryName + " was found here: " + where);
	                lastFoundBinaryPaths.add(where);
	                found = true;
	            }
	            else
	            {
	            	log(binaryName + " was NOT found here: " + where);
	            }
	        }
        }
        
        return found;
    }

    
    /**
     * 
     * @param file String that represent the file, including the full
     * path to the file and its name.
     * 
     * @return An instance of the class permissions from which you can get the permissions of the file
     * or if the file could not be found or permissions couldn't be determined then permissions will be null.
     * 
     */
    public static Permissions getFilePermissions(String file) {
        RootTools.log(InternalVariables.TAG, "Checking permissions for " + file);
        File f = new File(file);
        if (f.exists()) 
        {
        	Permissions permissions;
            log(file + " was found." );
            try 
            {
            	for (String line : sendShell(new String[] {"ls -l " + file, "busybox ls -l " + file, "/system/bin/failsafe/toolbox ls -l " + file, "toolbox ls -l " + file}, 0, InternalVariables.timeout))
                {
                	RootTools.log("Line " + line);
                	try
                	{
                		permissions = new InternalMethods().getPermissions(line);
                		if (permissions != null)
                			return new InternalMethods().getPermissions(line);
                	}
                	catch (Exception e) {
                		RootTools.log(e.getMessage());
                	}
                }
            } 
            catch (Exception e) 
            {
            	RootTools.log(e.getMessage());
                return null;
            }            
        }
        
        return null;
    }

    /**
     * @return BusyBox version is found, "" if not found.
     */
    public static String getBusyBoxVersion() {
        RootTools.log(InternalVariables.TAG, "Getting BusyBox Version");
        InternalVariables.busyboxVersion = null;
        try {
            new InternalMethods().doExec(new String[]{"busybox"}, InternalVariables.timeout);
        } catch (TimeoutException ex) {
            RootTools.log(InternalVariables.TAG, "TimeoutException!!!");        	
        } catch (Exception e) {
            RootTools.log(InternalVariables.TAG, "BusyBox was not found, more information MAY be available with Debugging on.");
            return "";
        }
        return InternalVariables.busyboxVersion;
    }

    /**
     * This will return an List of Strings.
     * Each string represents an applet available from BusyBox.
     * <p/>
     *
     * @return <code>List<String></code> a List of strings representing the applets available from Busybox.
     * @throws Exception if we cannot return the applets available.
     */
    public static List<String> getBusyBoxApplets() throws Exception {
    	List<String> commands = sendShell("busybox --list", InternalVariables.timeout);
        if (commands != null) {
            return commands;
        } else {
            throw new Exception();
        }
    }
    
    /**
     * This will let you know if an applet is available from BusyBox
     * <p/>
     *
     * @param <code>String</code> The applet to check for.
     * 
     * @return <code>true</code> if applet is available, false otherwise.
     */
    public static boolean isAppletAvailable(String Applet) {
    	try
    	{
	    	for(String applet : getBusyBoxApplets())
	    	{
	    		if (applet.equals(Applet))
	    		{
	    			return true;
	    		}
	    	}
	    	return false;
    	}
    	catch (Exception e)
    	{
    		RootTools.log(e.toString());
    		return false;
    	}
    }

    /**
     * @return <code>true</code> if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    public static boolean isAccessGiven() throws TimeoutException {
        RootTools.log(InternalVariables.TAG, "Checking for Root access");
        InternalVariables.accessGiven = false;
        new InternalMethods().doExec(new String[]{"id"}, InternalVariables.timeout);

        if (InternalVariables.accessGiven) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNativeToolsReady(int nativeToolsId, Context context) {
        RootTools.log(InternalVariables.TAG, "Preparing Native Tools");
        InternalVariables.nativeToolsReady = false;

        Installer installer;
        try {
            installer = new Installer(context);
        } catch (IOException ex) {
            if (debugMode) {
                ex.printStackTrace();
            }
            return false;
        }

        if(installer.isBinaryInstalled("nativetools")) {
            InternalVariables.nativeToolsReady = true;
        }
        else {
            InternalVariables.nativeToolsReady = installer.installBinary(nativeToolsId, "nativetools", "700");
        }
        return InternalVariables.nativeToolsReady;
    }

    /**
     * Checks if there is enough Space on SDCard
     *
     * @param updateSize size to Check (long)
     * @return <code>true</code> if the Update will fit on SDCard,
     *         <code>false</code> if not enough space on SDCard.
     *         Will also return <code>false</code>,
     *         if the SDCard is not mounted as read/write
     */
    public static boolean hasEnoughSpaceOnSdCard(long updateSize) {
        RootTools.log(InternalVariables.TAG, "Checking SDcard size and that it is mounted as RW");
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
     * This will take a path, which can contain the file name as well,
     * and attempt to remount the underlying partition.
     * <p/>
     * For example, passing in the following string:
     * "/system/bin/some/directory/that/really/would/never/exist"
     * will result in /system ultimately being remounted.
     * However, keep in mind that the longer the path you supply, the more work this has to do,
     * and the slower it will run.
     *
     * @param file      file path
     * @param mountType mount type: pass in RO (Read only) or RW (Read Write)
     * @return a <code>boolean</code> which indicates whether or not the partition
     *         has been remounted as specified.
     */

    public static boolean remount(String file, String mountType) {
        //Recieved a request, get an instance of Remounter
        Remounter remounter = new Remounter();
        //send the request.
        return (remounter.remount(file, mountType));
    }

    /**
     * This method can be used to unpack a binary from the raw resources folder and store it in
     * /data/data/app.package/files/
     * This is typically useful if you provide your own C- or C++-based binary.
     * This binary can then be executed using sendShell() and its full path.
     *
     * @param context  the current activity's <code>Context</code>
     * @param sourceId resource id; typically <code>R.raw.id</code>
     * @param destName destination file name; appended to /data/data/app.package/files/
     * @param mode     chmod value for this file
     * @return a <code>boolean</code> which indicates whether or not we were
     *         able to create the new file.
     */
    public static boolean installBinary(Context context, int sourceId, String destName, String mode) {
        Installer installer;

        try {
            installer = new Installer(context);
        } catch (IOException ex) {
            if (debugMode) {
                ex.printStackTrace();
            }
            return false;
        }

        return (installer.installBinary(sourceId, destName, mode));
    }

    /**
     * This method can be used to unpack a binary from the raw resources folder and store it in
     * /data/data/app.package/files/
     * This is typically useful if you provide your own C- or C++-based binary.
     * This binary can then be executed using sendShell() and its full path.
     *
     * @param context  the current activity's <code>Context</code>
     * @param sourceId resource id; typically <code>R.raw.id</code>
     * @param binaryName destination file name; appended to /data/data/app.package/files/
     * @return a <code>boolean</code> which indicates whether or not we were
     *         able to create the new file.
     */
    public static boolean installBinary(Context context, int sourceId, String binaryName) {
        return installBinary(context, sourceId, binaryName, "700");
    }
    
    /**
     * Executes binary in a separated process. Before using this method, the binary has to be installed
     * in /data/data/app.package/files/ using the installBinary method.
     * 
     * @param context the current activity's <code>Context</code>
     * @param binaryName name of installed binary
     * @param parameter parameter to append to binary like "-vxf"
     */
    public static void runBinary(Context context, String binaryName, String parameter) {
        // executes binary as separated thread
        Runner runner = new Runner(context, binaryName, parameter);
        runner.start();
    }
    
    /**
     * This method can be used to kill a running process
     * 
     * @param processName name of process to kill
     * @return <code>true</code> if process was found and killed successfully
     */
    public static boolean killProcess(String processName) {
        RootTools.log(InternalVariables.TAG, "Killing process " + processName);
        InternalVariables.pid = null;
        try {
			new InternalMethods().doExec(new String[]{"busybox pidof " + processName}, InternalVariables.timeout);
		} catch (TimeoutException e) {
			return false;
		}

        if (InternalVariables.pid != null) {
            try {
				new InternalMethods().doExec(new String[]{"busybox kill -9 " + InternalVariables.pid}, InternalVariables.timeout);
			} catch (TimeoutException e) {
				return false;
			}

            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This method can be used to to check if a process is running
     * 
     * @param processName name of process to check
     * @return <code>true</code> if process was found
     * @throws TimeoutException (Could not determine if the process is running)
     */
    public static boolean isProcessRunning(String processName) throws TimeoutException {
        RootTools.log(InternalVariables.TAG, "Checks if process is running: " + processName);
        InternalVariables.pid = null;
        new InternalMethods().doExec(new String[]{"busybox pidof " + processName}, InternalVariables.timeout);

        if (InternalVariables.pid != null) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This restarts only Android OS without rebooting the whole device.
     * This does NOT work on all devices.
     * This is done by killing the main init process named zygote. Zygote is restarted
     * automatically by Android after killing it.
     * @throws TimeoutException 
     */
    public static void restartAndroid() throws TimeoutException {
        RootTools.log(InternalVariables.TAG, "Restart Android");
        new InternalMethods().doExec(new String[]{"busybox killall -9 zygote"}, InternalVariables.timeout);
    }

    /**
     * Sends several shell command as su (attempts to)
     *
     * @param commands  array of commands to send to the shell
     * @param sleepTime time to sleep between each command, delay.
     * @param result    injected result object that implements the Result class
     * @param timeout   How long to wait before throwing TimeoutException, sometimes
     * 					when running root commands on certain devices or roms
     * 					ANR's may occur because a process never returns or readline never returns.
     * 					This allows you to protect your application from throwing an ANR.
     * 					
     * 					if you pass -1, then the default timeout is 5 minutes.
     * 
     * @return a <code>LinkedList</code> containing each line that was returned
     *         by the shell after executing or while trying to execute the given commands.
     *         You must iterate over this list, it does not allow random access,
     *         so no specifying an index of an item you want,
     *         not like you're going to know that anyways.
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException 
     */
    public static List<String> sendShell(String[] commands, int sleepTime, Result result, int timeout)
            throws IOException, RootToolsException, TimeoutException {
    	return sendShell(commands, sleepTime, result, true, timeout);
    }
    
    /**
     * Sends several shell command as su (attempts to) if useRoot is true;
     * as the current user (app_xxx) otherwise.
     *
     * @param commands  array of commands to send to the shell
     * @param sleepTime time to sleep between each command, delay.
     * @param result    injected result object that implements the Result class
     * @param useRoot   whether to use root or not when issuing these commands.
     * @param timeout   How long to wait before throwing TimeoutException, sometimes
     * 					when running root commands on certain devices or roms
     * 					ANR's may occur because a process never returns or readline never returns.
     * 					This allows you to protect your application from throwing an ANR.
     * 					
     * 					if you pass -1, then the default timeout is 5 minutes.
     * 
     * @return a <code>LinkedList</code> containing each line that was returned
     *         by the shell after executing or while trying to execute the given commands.
     *         You must iterate over this list, it does not allow random access,
     *         so no specifying an index of an item you want,
     *         not like you're going to know that anyways.
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException 
     */
    public static List<String> sendShell(String[] commands, int sleepTime, Result result, boolean useRoot, int timeout)
            throws IOException, RootToolsException, TimeoutException {
        return (new Executer().sendShell(commands, sleepTime, result, useRoot, timeout));
    }


    /**
     * Sends several shell command as su (attempts to)
     *
     * @param commands  array of commands to send to the shell
     * @param sleepTime time to sleep between each command, delay.
     * @param timeout   How long to wait before throwing TimeoutException, sometimes
     * 					when running root commands on certain devices or roms
     * 					ANR's may occur because a process never returns or readline never returns.
     * 					This allows you to protect your application from throwing an ANR.
     * 					
     * 					if you pass -1, then the default timeout is 5 minutes.
     *
     * @return a LinkedList containing each line that was returned by the shell
     *         after executing or while trying to execute the given commands.
     *         You must iterate over this list, it does not allow random access,
     *         so no specifying an index of an item you want,
     *         not like you're going to know that anyways.

     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException 
     */
    public static List<String> sendShell(String[] commands, int sleepTime, int timeout)
            throws IOException, RootToolsException, TimeoutException {
        return sendShell(commands, sleepTime, null, timeout);
    }

    /**
     * Sends one shell command as su (attempts to)
     *
     * @param command command to send to the shell
     * @param result  injected result object that implements the Result class
     * @param timeout   How long to wait before throwing TimeoutException, sometimes
     * 					when running root commands on certain devices or roms
     * 					ANR's may occur because a process never returns or readline never returns.
     * 					This allows you to protect your application from throwing an ANR.
     * 					
     * 					if you pass -1, then the default timeout is 5 minutes.
     * 
     * @return a <code>LinkedList</code> containing each line that was returned
     *         by the shell after executing or while trying to execute the given commands.
     *         You must iterate over this list, it does not allow random access,
     *         so no specifying an index of an item you want,
     *         not like you're going to know that anyways.
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws RootToolsException
     * @throws TimeoutException 
     */
    public static List<String> sendShell(String command, Result result, int timeout)
            throws IOException, RootToolsException, TimeoutException {
        return sendShell(new String[]{command}, 0, result, timeout);
    }

    /**
     * Sends one shell command as su (attempts to)
     *
     * @param command command to send to the shell
     * @param timeout   How long to wait before throwing TimeoutException, sometimes
     * 					when running root commands on certain devices or roms
     * 					ANR's may occur because a process never returns or readline never returns.
     * 					This allows you to protect your application from throwing an ANR.
     * 					
     * 					if you pass -1, then the default timeout is 5 minutes.
     * 
     * @return a LinkedList containing each line that was returned by the shell
     *         after executing or while trying to execute the given commands.
     *         You must iterate over this list, it does not allow random access,
     *         so no specifying an index of an item you want,
     *         not like you're going to know that anyways.
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException 
     */
    public static List<String> sendShell(String command, int timeout)
            throws IOException, RootToolsException, TimeoutException {
        return sendShell(command, null, timeout);
    }

    /**
     * Get the space for a desired partition.
     *
     * @param path The partition to find the space for.
     * @return the amount if space found within the desired partition.
     *         If the space was not found then the value is -1
     * @throws TimeoutException 
     */
    public static long getSpace(String path) {
        InternalVariables.getSpaceFor = path;
        boolean found = false;
        String[] commands = {"df " + path};
        try {
			new InternalMethods().doExec(commands, -1);
		} catch (TimeoutException e) {}

        RootTools.log("Looking for Space");

        if (InternalVariables.space != null) {
            RootTools.log("First Method");

            for (String spaceSearch : InternalVariables.space) {

                RootTools.log(spaceSearch);

                if (found) {
                    return new InternalMethods().getConvertedSpace(spaceSearch);
                } else if (spaceSearch.equals("used,")) {
                    found = true;
                }
            }

            //Try this way
            int count = 0,
                    targetCount = 3;

            RootTools.log("Second Method");

            if (!InternalVariables.space[0].startsWith(path)) {
                targetCount = 2;
            }

            for (String spaceSearch : InternalVariables.space) {

                RootTools.log(spaceSearch);
                if (spaceSearch.length() > 0) {
                    RootTools.log(spaceSearch + ("Valid"));
                    if (count == targetCount) {
                        return new InternalMethods().getConvertedSpace(spaceSearch);
                    }
                    count++;
                }
            }
        }
        RootTools.log("Returning -1, space could not be determined.");
        return -1;
    }

    /**
     * This method allows you to output debug messages only when debugging is on.
     * This will allow you to add a debug option to your app, which by default can be
     * left off for performance. However, when you need debugging information, a simple
     * switch can enable it and provide you with detailed logging.
     * <p/>
     * This method handles whether or not to log the information you pass it depending
     * whether or not RootTools.debugMode is on. So you can use this and not have to
     * worry about handling it yourself.
     *
     * @param TAG Optional parameter to define the tag that the Log will use.
     * @param msg The message to output.
     */
    public static void log(String msg) {
        log(null, msg);
    }

    public static void log(String TAG, String msg) {
        if (debugMode) {
            if (TAG != null) {
                Log.d(TAG, msg);
            } else {
                Log.d(InternalVariables.TAG, msg);
            }
        }
    }

    public static abstract class Result implements IResult {
        private Process process = null;
        private Serializable data = null;
        private int error = 0;

        public abstract void process(String line) throws Exception;

        public abstract void processError(String line) throws Exception;

        public abstract void onFailure(Exception ex);

        public abstract void onComplete(int diag);

        public Result setProcess(Process process) {
            this.process = process;
            return this;
        }

        public Process getProcess() {
            return process;
        }

        public Result setData(Serializable data) {
            this.data = data;
            return this;
        }

        public Serializable getData() {
            return data;
        }

        public Result setError(int error) {
            this.error = error;
            return this;
        }

        public int getError() {
            return error;
        }
    }
}
