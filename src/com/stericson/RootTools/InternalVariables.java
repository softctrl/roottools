/* 
 * This file is part of the RootTools Project: http://code.google.com/p/roottools/
 *  
 * Copyright (c) 2012 Stephen Ericson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
 *  
 * This code is dual-licensed under the terms of the Apache License Version 2.0 and
 * the terms of the General Public License (GPL) Version 2.
 * You may use this code according to either of these licenses as is most appropriate
 * for your project on a case-by-case basis.
 * 
 * The terms of each license can be found in the root directory of this project's repository as well as at:
 * 
 * * http://www.apache.org/licenses/LICENSE-2.0
 * * http://www.gnu.org/licenses/gpl-2.0.txt
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under these Licenses is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See each License for the specific language governing permissions and
 * limitations under that License.
 */

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
