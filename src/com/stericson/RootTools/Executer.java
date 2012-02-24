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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.stericson.RootTools.RootTools.Result;

class Executer {

	private Process process = null;
	private DataOutputStream os = null;
	private InputStreamReader osRes = null;
	private InputStreamReader osErr = null;
	private Result result = null;
	
	public void openShell(boolean useRoot, Result result) throws IOException
	{
    	Runtime.getRuntime().gc();
		this.process = Runtime.getRuntime().exec(useRoot ? "su" : "sh");
		RootTools.log(useRoot ? "Using Root" : "Using sh");

		this.os = new DataOutputStream(process.getOutputStream());
		this.osRes = new InputStreamReader(process.getInputStream());
		this.osErr = new InputStreamReader(process.getErrorStream());
		result.setProcess(process);
	}
	
	public void executeCommand(String command) throws Exception
	{
		BufferedReader reader = new BufferedReader(osRes);
		BufferedReader reader_error = new BufferedReader(osErr);

		RootTools.log("Shell command: " + command);
		os.writeBytes(command + "\n");
		os.flush();

		os.writeBytes("exit \n");
		os.flush();

		String line = reader.readLine();
		String line_error = reader_error.readLine();

		while (line != null) {
			result.process(line);
			
			RootTools.log("input stream" + line);
			line = reader.readLine();
		}

		while (line_error != null) {
			result.processError(line);

			RootTools.log("error stream: " + line_error);
			line_error = reader_error.readLine();
		}
		
        if (reader != null)
        {
        	reader.close();
        	reader = null;
        }
        if (reader_error != null)
        {
        	reader_error.close();
        	reader_error = null;
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
	}
	
	// ------------
	// # Executer #
	// ------------

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
			IResult result, boolean useRoot, int timeout) throws IOException,
			RootToolsException, TimeoutException {

		RootTools.log(InternalVariables.TAG, "Sending " + commands.length
				+ " shell command" + (commands.length > 1 ? "s" : ""));

        Worker worker = new Worker(commands, sleepTime, result, useRoot);
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

    private static class Worker extends Thread 
    {
    	private String[] commands;
    	private int sleepTime;
    	private IResult result;
    	private boolean useRoot;
    	public int exit = -911;
    	public List<String> finalResponse;
	  
		private Worker(String[] commands, int sleepTime, IResult result, boolean useRoot) 
		{
			this.commands = commands;
			this.sleepTime = sleepTime;
			this.result = result;
			this.useRoot = useRoot;
		}
		public void run() 
		{
			Process process = null;
			DataOutputStream os = null;
			InputStreamReader osRes = null;
			InputStreamReader osErr = null;
		    try 
		    { 
		    	Runtime.getRuntime().gc();
				process = Runtime.getRuntime().exec(useRoot ? "su" : "sh");
				RootTools.log(useRoot ? "Using Root" : "Using sh");
				if (null != result) {
					result.setProcess(process);
				}

				os = new DataOutputStream(process.getOutputStream());
				osRes = new InputStreamReader(process.getInputStream());
				osErr = new InputStreamReader(process.getErrorStream());
				BufferedReader reader = new BufferedReader(osRes);
				BufferedReader reader_error = new BufferedReader(osErr);

				List<String> response = null;

				if (null == result) {
					response = new LinkedList<String>();
				}

				try {
					
					// Doing Stuff ;)
					for (String single : commands) {
						RootTools.log("Shell command: " + single);
						os.writeBytes(single + "\n");
						os.flush();
						Thread.sleep(sleepTime);
					}

					os.writeBytes("exit \n");
					os.flush();

					String line = reader.readLine();
					String line_error = reader_error.readLine();

					while (line != null) {
						if (null == result) {
							response.add(line);
						} else {
							result.process(line);
						}

						RootTools.log("input stream" + line);
						line = reader.readLine();
					}

					while (line_error != null) {
						if (null == result) {
							response.add(line_error);
						} else {
							result.processError(line_error);
						}

						RootTools.log("error stream: " + line_error);
						line_error = reader_error.readLine();
					}

				} 
				catch (Exception ex) 
				{
					if (null != result) 
					{
						result.onFailure(ex);
					}
				} finally {
					if (process != null) {
						finalResponse = response;
						exit = process.waitFor();
						RootTools.lastExitCode = exit;
						
						if (null != result) {
							result.onComplete(exit);
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

	        	if (process != null)
	        	{
	        		try {
	        			//if this fails, ignore it and dont crash.
	        			process.destroy();
	        		} catch (Exception e) {}
	        		process = null;
	        	}
                
	            try {
	                if (os != null) {
	                	os.flush();
	                    os.close();
	                }
	                if (osRes != null) {
	                    osRes.close();
	                }
	                if (osErr != null) {
	                    osErr.close();
	                }
	            } catch (Exception e) {
	                if (RootTools.debugMode) {
	                    RootTools.log("Error: " + e.getMessage());
	                }
	            }
	        }
		}
    }
}
