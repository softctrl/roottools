package com.stericson.RootTools;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.stericson.RootTools.RootTools.Result;

class ShellManager {

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
	
	public void executeCommand(String command) throws InterruptedException
	{
		try {
			BufferedReader reader = new BufferedReader(osRes);
			BufferedReader reader_error = new BufferedReader(osErr);
	
			RootTools.log("Shell command: " + command);
			os.writeBytes(command + "\n");
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
		catch (Exception ex) 
		{
			if (null != this.result) 
			{
				this.result.onFailure(ex);
			}
		} finally {
			if (this.process != null) {
				int exit = this.process.waitFor();
				RootTools.lastExitCode = exit;
				
				if (null != this.result) {
					this.result.onComplete(exit);
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
	}
}
