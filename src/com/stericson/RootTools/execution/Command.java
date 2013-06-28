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

package com.stericson.RootTools.execution;

import java.io.IOException;

import com.stericson.RootTools.RootTools;

public abstract class Command {
    final String[] command;
    boolean finished = false;
    boolean terminated = false;
    int exitCode = -1;
    int id = 0;
    int timeout = 50000;

    public abstract void output(int id, String line);
    public abstract void commandTerminated(int id, String reason);
    public abstract void commandCompleted(int id, int exitCode);

    public Command(int id, String... command) {
        this.command = command;
        this.id = id;
    }

    public Command(int id, int timeout, String... command) {
        this.command = command;
        this.id = id;
        this.timeout = timeout;
    }

    public String getCommand() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.length; i++) {
            sb.append(command[i]);
            sb.append('\n');
        }
        RootTools.log("Sending command(s): " + sb.toString());
        return sb.toString();
    }

    public int getExitCode() {
        return this.exitCode;
    }

    protected void commandFinished() {
        if (!terminated) {
            synchronized (this) {
                RootTools.log("Command " + id + " finished.");
                finished = true;
                this.notifyAll();
                commandCompleted(id, exitCode);
            }
        }
    }

    protected void setExitCode(int code) {
        synchronized (this) {
            exitCode = code;
        }
    }

    protected void startExecution() {
        synchronized (this) {

            Thread t = new Thread() {
                public void run() {
                    while (!finished) {

                        synchronized (Command.this) {
                            try {
                                Command.this.wait(timeout);
                            } catch (InterruptedException e) {}
                        }

                        if (!finished) {
                            finished = true;
                            RootTools.log("Timeout Exception has occurred.");
                            terminate("Timeout Exception");
                        }
                    }
                }
            };

            t.start();
        }
    }

    public void terminate(String reason) {
        try {
            Shell.closeAll();
            RootTools.log("Terminating all shells.");
            terminated(reason);
        } catch (IOException e) {}
    }

    protected void terminated(String reason) {
        synchronized (Command.this) {
            setExitCode(-1);
            terminated = true;
            this.finished = true;
            this.notifyAll();
            RootTools.log("Command " + id + " did not finish because it was terminated. Termination reason: " + reason);
            commandTerminated(id, reason);
        }
    }

    /**
     * @deprecated This is a deprecated function and should not be used.
     * Extend Command and implement the methods commandCompleted and commandTerminated
     * to be notified when a command has completed.
     */
    public void waitForFinish() throws InterruptedException {
        synchronized (this) {
            while (!finished) {
                this.wait(timeout);
            }
        }
    }

    /**
     * @deprecated This is a deprecated function and should not be used.
     * Extend Command and implement the methods commandCompleted and commandTerminated
     * to be notified when a command has completed.
     */
    public int exitCode() throws InterruptedException {
        synchronized (this) {
            waitForFinish();
        }
        return exitCode;
    }
}