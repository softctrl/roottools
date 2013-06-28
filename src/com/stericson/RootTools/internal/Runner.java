/* 
 * This file is part of the RootTools Project: http://code.google.com/p/roottools/
 *  
 * Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
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

package com.stericson.RootTools.internal;

import java.io.IOException;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

import android.content.Context;
import android.util.Log;

public class Runner extends Thread {

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
            privateFilesPath = context.getFilesDir().getCanonicalPath();
        } catch (IOException e) {
            if (RootTools.debugMode) {
                Log.e(LOG_TAG, "Problem occured while trying to locate private files directory!");
            }
            e.printStackTrace();
        }
        if (privateFilesPath != null) {
            try {
                InternalCommand command = new InternalCommand(0, privateFilesPath + "/" + binaryName + " " + parameter);
                Shell.startRootShell().add(command);
                commandWait();

            } catch (Exception e) {
            }
        }
    }

    protected class InternalCommand extends Command {

        public InternalCommand(int id, String... command) {
            super(id, command);
        }

        @Override
        public void output(int id, String line) {
            //does nothing
        }

        @Override
        public void commandTerminated(int id, String reason) {
            synchronized (this) {
                this.notifyAll();
            }
        }

        @Override
        public void commandCompleted(int id, int exitCode) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    protected void commandWait() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {}
        }
    }
}
