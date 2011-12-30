package com.stericson.RootTools;

import java.util.ArrayList;
import java.util.Set;

//no modifier, this is package-private which means that no one but the library can access it.
//If we need public variables just create the class for it.
class InternalVariables {

    //----------------------
    //# Internal Variables #
    //----------------------

    //Version numbers should be maintained here.
    protected static String TAG = "RootTools v1.5.1";
    protected static boolean accessGiven = false;
    protected static boolean nativeToolsReady = false;
    protected static String[] space;
    protected static String getSpaceFor;
    protected static String busyboxVersion;
    protected static String pid;
    protected static Set<String> path;
    protected static ArrayList<Mount> mounts;
    protected static ArrayList<Symlink> symlinks;
    protected static int timeout = 10000;

}
