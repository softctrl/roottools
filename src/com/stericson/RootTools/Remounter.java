package com.stericson.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import android.util.Log;

//no modifier, this means it is package-private. Only our internal classes can use this.
class Remounter {

    //-------------
    //# Remounter #
    //-------------

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

    protected boolean remount(String file, String mountType) {
    	String util = "";
    	
    	if (RootTools.checkUtil("busybox"))
    	{
    		util = RootTools.utilPath;
    	}
    	else if (RootTools.checkUtil("toolbox"))
    	{
    		util = RootTools.utilPath;    		
    	}
    	
        //if the path has a trailing slash get rid of it.
        if (file.endsWith("/")) {
            file = file.substring(0, file.lastIndexOf("/"));
        }
        //Make sure that what we are trying to remount is in the mount list.
        boolean foundMount = false;
        while (!foundMount) {
            try {
                for (Mount mount : RootTools.getMounts()) {
                    RootTools.log(mount.getMountPoint().toString());

                    if (file.equals(mount.getMountPoint().toString())) {
                        foundMount = true;
                        break;
                    }
                }
            } catch (Exception e) {
                if (RootTools.debugMode) {
                    e.printStackTrace();
                }
                return false;
            }
            if (!foundMount) {
                try {
                    file = (new File(file).getParent()).toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        Mount mountPoint = findMountPointRecursive(file);

        Log.i(InternalVariables.TAG, "Remounting " + mountPoint.getMountPoint().getAbsolutePath() + " as " + mountType.toLowerCase());
        final boolean isMountMode = mountPoint.getFlags().contains(mountType.toLowerCase());

        if (!isMountMode) {
            //grab an instance of the internal class
            try {
            	new InternalMethods().doExec(new String[]{
				        String.format(
				                util + " mount -o remount,%s %s %s",
				                mountType.toLowerCase(),
				                mountPoint.getDevice().getAbsolutePath(),
				                mountPoint.getMountPoint().getAbsolutePath()),
				        String.format(
				        		"mount -o remount,%s %s %s",
				                mountType.toLowerCase(),
				                mountPoint.getDevice().getAbsolutePath(),
				                mountPoint.getMountPoint().getAbsolutePath()),
				        String.format(
				        		"/system/bin/toolbox mount -o remount,%s %s %s",
				                mountType.toLowerCase(),
				                mountPoint.getDevice().getAbsolutePath(),
				                mountPoint.getMountPoint().getAbsolutePath())
				}, -1);
			} catch (TimeoutException e) {}
            mountPoint = findMountPointRecursive(file);
        }

        Log.i(InternalVariables.TAG, mountPoint.getFlags() + " AND " + mountType.toLowerCase());
        if (mountPoint.getFlags().contains(mountType.toLowerCase())) {
            RootTools.log(mountPoint.getFlags().toString());
            return true;
        } else {
            RootTools.log(mountPoint.getFlags().toString());
            return false;
        }
    }

    private Mount findMountPointRecursive(String file) {
        try {
            ArrayList<Mount> mounts = RootTools.getMounts();
            for (File path = new File(file); path != null; ) {
                for (Mount mount : mounts) {
                    if (mount.getMountPoint().equals(path)) {
                        return mount;
                    }
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            if (RootTools.debugMode) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
