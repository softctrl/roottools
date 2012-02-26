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

package com.stericson.RootTools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools.Result;

class Executer {

	protected Process process = null;
	protected DataOutputStream os = null;
	protected InputStreamReader osRes = null;
	protected InputStreamReader osErr = null;
	protected Result result = null;
	
	// ------------
	// # Executer #
	// ------------

	public Executer()
	{
		InternalVariables.executer = this;
	}
	
	/**
	 * Sends several shell command as su (attempts to)
	 * 
	 * @param commands
	 *            array of commands to send to the shell
	 * 
	 * @param sleepTime
	 *            time to sleep between each command, delay.
	 * 
	 * @param result
	 *            injected result object that implements the Result class
	 * 
	 * @return a <code>LinkedList</code> containing each line that was returned
	 *         by the shell after executing or while trying to execute the given
	 *         commands. You must iterate over this list, it does not allow
	 *         random access, so no specifying an index of an item you want, not
	 *         like you're going to know that anyways.
	 * 
	 * @throws InterruptedException
	 * 
	 * @throws IOException
	 * @throws TimeoutException 
	 */
	List<String> sendShell(String[] commands, int sleepTime,
			Result result, boolean useRoot, int timeout) throws IOException,
			RootToolsException, TimeoutException {

		RootTools.log(InternalVariables.TAG, "Sending " + commands.length
				+ " shell command" + (commands.length > 1 ? "s" : ""));

        Worker worker = new Worker(this, commands, sleepTime, result, useRoot);
        worker.start();
        
        try
        {
        	if (timeout == -1)
        	{
        		timeout = 300000;
        	}
        	
        	worker.join(timeout);
        	
        	//small pause, let things catch up
			Thread.sleep(RootTools.shellDelay);

            if (worker.exit != -911)
              return worker.finalResponse;
            else
              throw new TimeoutException();
        } 
        catch(InterruptedException ex) 
        {
            worker.interrupt();
            Thread.currentThread().interrupt();
            throw new TimeoutException();
        } 

	}

    protected static class Worker extends Thread 
    {
    	private String[] commands;
    	private int sleepTime;
    	private boolean useRoot;
    	public int exit = -911;
    	public List<String> finalResponse;
    	private Executer executer;
    	
	  
		private Worker(Executer executer, String[] commands, int sleepTime, Result result, boolean useRoot) 
		{
			this.commands = commands;
			this.sleepTime = sleepTime;
			executer.result = result;
			this.useRoot = useRoot;
		}
		public void run() 
		{
		    try 
		    {
		    	if (executer.process == null && executer.os == null && executer.osErr == null && executer.osRes == null)
		    	{
			    	Runtime.getRuntime().gc();
			    	executer.process = Runtime.getRuntime().exec(useRoot ? "su" : "sh");
					RootTools.log(useRoot ? "Using Root" : "Using sh");
					
					if (null != executer.result) {
						executer.result.setProcess(executer.process);
					}

					executer.os = new DataOutputStream(executer.process.getOutputStream());
					executer.osRes = new InputStreamReader(executer.process.getInputStream());
					executer.osErr = new InputStreamReader(executer.process.getErrorStream());

		    	}
		    	
				BufferedReader reader = new BufferedReader(executer.osRes);
				BufferedReader reader_error = new BufferedReader(executer.osErr);
				
				List<String> response = null;

				if (null == executer.result) {
					response = new LinkedList<String>();
				}

				try {
					
					// Doing Stuff ;)
					for (String single : commands) {
						RootTools.log("Shell command: " + single);
						executer.os.writeBytes(single + "\n");
						executer.os.flush();
						Thread.sleep(sleepTime);
					}

					if (!RootTools.keepShell)
					{
						executer.os.writeBytes("exit \n");
						executer.os.flush();
					}
					
					String line = reader.readLine();
					String line_error = reader_error.readLine();

					while (line != null) {
						if (null == executer.result) {
							response.add(line);
						} else {
							executer.result.process(line);
						}

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
		                if (commands[0].equals("busybox")) {
		                    if (line.startsWith("BusyBox")) {
		                        String[] temp = line.split(" ");
		                        InternalVariables.busyboxVersion = temp[1];
		                    }
		                }
		                if (commands[0].startsWith("df")) {
		                    if (line.contains(commands[0].substring(2, commands[0].length()).trim())) {
		                        InternalVariables.space = line.split(" ");
		                    }
		                }
		                
						RootTools.log("input stream" + line);
						line = reader.readLine();
					}

					while (line_error != null) {
						if (null == executer.result) {
							response.add(line_error);
						} else {
							executer.result.processError(line_error);
						}

						RootTools.log("error stream: " + line_error);
						line_error = reader_error.readLine();
					}

				} 
				catch (Exception ex) 
				{
					if (null != executer.result) 
					{
						executer.result.onFailure(ex);
					}
				} finally {
					if (executer.process != null) {
						finalResponse = response;
						exit = executer.process.waitFor();
						RootTools.lastExitCode = exit;
						
						if (null != executer.result) {
							executer.result.onComplete(exit);
						} else {
							response.add(Integer.toString(exit));
						}
					}
				}
		    }
		    catch (InterruptedException ignore) 
		    {
		    	return;
		    }
		    catch (Exception e) {
	            if (RootTools.debugMode) {
	                RootTools.log("Error: " + e.getMessage());
	            }
	        } finally {
				if (!RootTools.keepShell)
	        	{
	        		executer.closeShell();
	        	}
	        }
		}
    }
    
	public void closeShell()
	{
		if (this.process != null)
    	{
    		try {
    			//if this fails, ignore it and dont crash.
    			this.process.destroy();
    		} catch (Exception e) {}
    		this.process = null;
    	}
        
        try {
            if (this.os != null) {
            	this.os.writeBytes("exit \n");
    			this.os.flush();
    			this.os.close();
                this.os = null;
            }
            if (this.osRes != null) {
                this.osRes.close();
                this.osRes = null;
            }
            if (this.osErr != null) {
                this.osErr.close();
                this.osErr = null;
            }
            if (this.result != null)
            {
            	this.result = null;
            }
        } catch (Exception e) {
            if (RootTools.debugMode) {
                RootTools.log("Error: " + e.getMessage());
            }
        }
        
		InternalVariables.executer = null;
	}
}
