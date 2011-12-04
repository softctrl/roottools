package com.stericson.RootTools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//no modifier, this is package-private which means that no one but the library can access it.
class InternalMethods {

    //--------------------
    //# Internal methods #
    //--------------------

    static private InternalMethods instance_;

    static protected InternalMethods instance() {
        if (null == instance_) {
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
        InputStreamReader osErr = null;

        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            osRes = new InputStreamReader(process.getInputStream());
            osErr = new InputStreamReader(process.getErrorStream());
            BufferedReader reader = new BufferedReader(osRes);
            BufferedReader reader_err = new BufferedReader(osErr);

            // Doing Stuff ;)
            for (String single : commands) {
                os.writeBytes(single + "\n");
                os.flush();
            }


            os.writeBytes("exit \n");
            os.flush();

            String line = reader.readLine();
            String line_err = reader_err.readLine();

            while (line != null) {
                if (commands[0].equals("id")) {
                    Set<String> ID = new HashSet<String>(Arrays.asList(line.split(" ")));
                    for (String id : ID) {
                        if (id.toLowerCase().contains("uid=0")) {
                            InternalVariables.accessGiven = true;
                            RootTools.log(InternalVariables.TAG, "Access Given");
                            break;
                        }
                    }
                    if (!InternalVariables.accessGiven) {
                        RootTools.log(InternalVariables.TAG, "Access Denied?");
                    }
                }
                if (commands[0].startsWith("df")) {
                    if (line.contains(commands[0].substring(2, commands[0].length()).trim())) {
                        InternalVariables.space = line.split(" ");
                    }
                }
                if (commands[0].equals("busybox")) {
                    if (line.startsWith("BusyBox")) {
                        String[] temp = line.split(" ");
                        InternalVariables.busyboxVersion = temp[1];
                    }
                }
                if (commands[0].startsWith("busybox pidof")) {
                    if (!line.equals("")) {
                        RootTools.log("PID: " + line);
                        InternalVariables.pid = line;
                    }
                }

                RootTools.log(line);

                line = reader.readLine();
            }

            while (line_err != null) {

                RootTools.log(line_err);

                line_err = reader_err.readLine();
            }

            process.waitFor();

        } catch (Exception e) {
            if (RootTools.debugMode) {
                RootTools.log("Error: " + e.getMessage());
                e.printStackTrace();
            }
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
                if (RootTools.debugMode) {
                    RootTools.log("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    protected boolean returnPath() {
        File tmpDir = new File("/data/local/tmp");
        if (!tmpDir.exists()) {
            doExec(new String[]{"mkdir /data/local/tmp"});
        }
        try {
            InternalVariables.path = new HashSet<String>();
            //Try to read from the file.
            LineNumberReader lnr = null;
            doExec(new String[]{"dd if=/init.rc of=/data/local/tmp/init.rc",
                    "chmod 0777 /data/local/tmp/init.rc"});
            lnr = new LineNumberReader(new FileReader("/data/local/tmp/init.rc"));
            String line;
            while ((line = lnr.readLine()) != null) {
                RootTools.log(line);
                if (line.contains("export PATH")) {
                    int tmp = line.indexOf("/");
                    InternalVariables.path = new HashSet<String>(Arrays.asList(line.substring(tmp).split(":")));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            if (RootTools.debugMode) {
                RootTools.log("Error: " + e.getMessage());
                e.printStackTrace();
            }
            return false;
        }
    }

    protected ArrayList<Mount> getMounts() throws FileNotFoundException, IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader("/proc/mounts"));
            String line;
            ArrayList<Mount> mounts = new ArrayList<Mount>();
            while ((line = lnr.readLine()) != null) {

                RootTools.log(line);

                String[] fields = line.split(" ");
                mounts.add(new Mount(
                        new File(fields[0]), // device
                        new File(fields[1]), // mountPoint
                        fields[2], // fstype
                        fields[3] // flags
                ));
            }
            return mounts;
        } finally {
            //no need to do anything here.
        }
    }

    protected ArrayList<Symlink> getSymLinks() throws FileNotFoundException, IOException {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader("/data/local/symlinks.txt"));
            String line;
            ArrayList<Symlink> symlink = new ArrayList<Symlink>();
            while ((line = lnr.readLine()) != null) {

                RootTools.log(line);

                String[] fields = line.split(" ");
                symlink.add(new Symlink(
                        new File(fields[fields.length - 3]), // file
                        new File(fields[fields.length - 1]) // SymlinkPath
                ));
            }
            return symlink;
        } finally {
            //no need to do anything here.
        }
    }

    protected Permissions getPermissions(String line) {

    	String[] lineArray = line.split(" ");
    	String rawPermissions = lineArray[0];
    	
    	RootTools.log(rawPermissions);
    	
    	Permissions permissions = new Permissions();
    	
    	permissions.setType(rawPermissions.substring(0));

    	RootTools.log(permissions.getType());

    	permissions.setUserPermissions(rawPermissions.substring(1, 4));
    	
    	RootTools.log(permissions.getUserPermissions());
    	
    	permissions.setGroupPermissions(rawPermissions.substring(4, 7));
    	
    	RootTools.log(permissions.getGroupPermissions());

    	permissions.setOtherPermissions(rawPermissions.substring(7, 10));
    	
    	RootTools.log(permissions.getOtherPermissions());

    	
    	String finalPermissions;
    	finalPermissions = Integer.toString(parsePermissions(permissions.getUserPermissions()));
    	finalPermissions += Integer.toString(parsePermissions(permissions.getGroupPermissions()));
    	finalPermissions += Integer.toString(parsePermissions(permissions.getOtherPermissions()));
    	
    	permissions.setPermissions(Integer.parseInt(finalPermissions));
    	
        return permissions;
    }
    
    protected int parsePermissions(String permission)
    {
    	int tmp;
    	if (permission.charAt(0) == 'r')
    		tmp = 4;
    	else
    		tmp = 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(0));
    	
    	if (permission.charAt(1) == 'w')
    		tmp = tmp + 2;
    	else
    		tmp = tmp + 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(1));

    	if (permission.charAt(2) == 'x')
    		tmp = tmp + 1;
    	else
    		tmp = tmp + 0;

    	RootTools.log("permission " + tmp);
    	RootTools.log("character " + permission.charAt(2));

    	return tmp;
    }

    /*
     * @return long Size, converted to kilobytes (from xxx or xxxm or xxxk etc.)
     */
    protected long getConvertedSpace(String spaceStr) {
    	try {
	        double multiplier = 1.0;
	        char c;
	        StringBuffer sb = new StringBuffer();
	        for (int i = 0; i < spaceStr.length(); i++) {
	            c = spaceStr.charAt(i);
	            if (!Character.isDigit(c) && c != '.') {
	                if (c == 'm' || c == 'M') {
	                    multiplier = 1024.0;
	                } else if (c == 'g' || c == 'G') {
	                    multiplier = 1024.0 * 1024.0;
	                }
	                break;
	            }
	            sb.append(spaceStr.charAt(i));
	        }
	        return (long) Math.ceil(Double.valueOf(sb.toString()) * multiplier);
    	}
    	catch (Exception e) 
    	{
    		return -1;
    	}
    }
}
