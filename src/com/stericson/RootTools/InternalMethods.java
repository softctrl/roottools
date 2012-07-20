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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import com.stericson.RootTools.RootTools.Result;

//no modifier, this is package-private which means that no one but the library can access it.
class InternalMethods
{

	// --------------------
	// # Internal methods #
	// --------------------

	protected boolean returnPath() throws TimeoutException
	{

		InternalCommand command = null;

		File tmpDir = new File("/data/local/tmp");
		try
		{
			if(!tmpDir.exists())
			{
				command = new InternalCommand(0, "mkdir /data/local/tmp");
				Shell.startRootShell().add(command);
				command.exitCode();
			}

			InternalVariables.path = new HashSet<String>();
			// Try to read from the file.
			LineNumberReader lnr = null;

			String mountedas = RootTools.getMountedAs("/");
			RootTools.remount("/", "rw");

			command = new InternalCommand(0, "chmod 0777 /init.rc");
			Shell.startRootShell().add(command);
			command = new InternalCommand(0,
					"dd if=/init.rc of=/data/local/tmp/init.rc");
			Shell.startRootShell().add(command);
			command = new InternalCommand(0,
					"chmod 0777 /data/local/tmp/init.rc");
			Shell.startRootShell().add(command);
			command.exitCode();

			RootTools.remount("/", mountedas);

			lnr = new LineNumberReader(
					new FileReader("/data/local/tmp/init.rc"));
			String line;
			while ((line = lnr.readLine()) != null)
			{
				RootTools.log(line);
				if(line.contains("export PATH"))
				{
					int tmp = line.indexOf("/");
					InternalVariables.path = new HashSet<String>(
							Arrays.asList(line.substring(tmp).split(":")));
					return true;
				}
			}
			return false;
		}
		catch (Exception e)
		{
			if(RootTools.debugMode)
			{
				RootTools.log("Error: " + e.getMessage());
				e.printStackTrace();
			}
			return false;
		}
	}

	protected ArrayList<Mount> getMounts() throws FileNotFoundException,
			IOException
	{
		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader("/proc/mounts"));
			String line;
			ArrayList<Mount> mounts = new ArrayList<Mount>();
			while ((line = lnr.readLine()) != null)
			{

				RootTools.log(line);

				String[] fields = line.split(" ");
				mounts.add(new Mount(new File(fields[0]), // device
						new File(fields[1]), // mountPoint
						fields[2], // fstype
						fields[3] // flags
				));
			}
			return mounts;
		}
		finally
		{
			// no need to do anything here.
		}
	}

	protected ArrayList<Symlink> getSymLinks() throws FileNotFoundException,
			IOException
	{
		LineNumberReader lnr = null;
		try
		{
			lnr = new LineNumberReader(new FileReader(
					"/data/local/symlinks.txt"));
			String line;
			ArrayList<Symlink> symlink = new ArrayList<Symlink>();
			while ((line = lnr.readLine()) != null)
			{

				RootTools.log(line);

				String[] fields = line.split(" ");
				symlink.add(new Symlink(new File(fields[fields.length - 3]), // file
						new File(fields[fields.length - 1]) // SymlinkPath
				));
			}
			return symlink;
		}
		finally
		{
			// no need to do anything here.
		}
	}

	protected Permissions getPermissions(String line)
	{

		String[] lineArray = line.split(" ");
		String rawPermissions = lineArray[0];

		if(rawPermissions.length() == 10
				&& (rawPermissions.charAt(0) == '-'
						|| rawPermissions.charAt(0) == 'd' || rawPermissions
						.charAt(0) == 'l')
				&& (rawPermissions.charAt(1) == '-' || rawPermissions.charAt(1) == 'r')
				&& (rawPermissions.charAt(2) == '-' || rawPermissions.charAt(2) == 'w'))
		{
			RootTools.log(rawPermissions);

			Permissions permissions = new Permissions();

			permissions.setType(rawPermissions.substring(0, 1));

			RootTools.log(permissions.getType());

			permissions.setUserPermissions(rawPermissions.substring(1, 4));

			RootTools.log(permissions.getUserPermissions());

			permissions.setGroupPermissions(rawPermissions.substring(4, 7));

			RootTools.log(permissions.getGroupPermissions());

			permissions.setOtherPermissions(rawPermissions.substring(7, 10));

			RootTools.log(permissions.getOtherPermissions());

			StringBuilder finalPermissions = new StringBuilder();
			finalPermissions.append(parseSpecialPermissions(rawPermissions));
			finalPermissions.append(parsePermissions(permissions.getUserPermissions()));
			finalPermissions.append(parsePermissions(permissions.getGroupPermissions()));
			finalPermissions.append(parsePermissions(permissions.getOtherPermissions()));

			permissions.setPermissions(Integer.parseInt(finalPermissions.toString()));

			return permissions;
		}

		return null;
	}

	protected int parsePermissions(String permission)
	{
		int tmp;
		if(permission.charAt(0) == 'r')
			tmp = 4;
		else
			tmp = 0;

		RootTools.log("permission " + tmp);
		RootTools.log("character " + permission.charAt(0));

		if(permission.charAt(1) == 'w')
			tmp += 2;
		else
			tmp += 0;

		RootTools.log("permission " + tmp);
		RootTools.log("character " + permission.charAt(1));

		if(permission.charAt(2) == 'x')
			tmp += 1;
		else
			tmp += 0;

		RootTools.log("permission " + tmp);
		RootTools.log("character " + permission.charAt(2));

		return tmp;
	}
	
	protected int parseSpecialPermissions(String permission)
	{
		int tmp = 0;
		if(permission.charAt(2) == 's')
			tmp += 4;

		if(permission.charAt(5) == 's')
			tmp += 2;

		if(permission.charAt(8) == 't')
			tmp += 1;

		RootTools.log("special permissions " + tmp);

		return tmp;
	}

	/*
	 * @return long Size, converted to kilobytes (from xxx or xxxm or xxxk etc.)
	 */
	protected long getConvertedSpace(String spaceStr)
	{
		try
		{
			double multiplier = 1.0;
			char c;
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i < spaceStr.length(); i++)
			{
				c = spaceStr.charAt(i);
				if(!Character.isDigit(c) && c != '.')
				{
					if(c == 'm' || c == 'M')
					{
						multiplier = 1024.0;
					}
					else if(c == 'g' || c == 'G')
					{
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

	/**
	 * This will check a given binary, determine if it exists and determine that
	 * it has either the permissions 755, 775, or 777.
	 * 
	 * 
	 * @param String
	 *            Name of the utility to check.
	 * 
	 * @return boolean to indicate whether the binary is installed and has
	 *         appropriate permissions.
	 */
	static boolean checkUtil(String util)
	{
		if(RootTools.findBinary(util))
		{

			List<String> binaryPaths = new ArrayList<String>();
			binaryPaths.addAll(RootTools.lastFoundBinaryPaths);

			for(String path : binaryPaths)
			{
				Permissions permissions = RootTools
						.getFilePermissionsSymlinks(path + "/" + util);

				if(permissions != null)
				{
					String permission = Integer.toString(permissions.getPermissions()).substring(1);

					if(permission.equals(755) || permission.equals(777)
							|| permission.equals(775))
					{
						RootTools.utilPath = path + "/" + util;
						return true;
					}
				}
			}
		}

		return false;

	}

	/**
	 * 
	 * @param file
	 *            String that represent the file, including the full path to the
	 *            file and its name.
	 * 
	 * @return An instance of the class permissions from which you can get the
	 *         permissions of the file or if the file could not be found or
	 *         permissions couldn't be determined then permissions will be null.
	 * 
	 */
	static Permissions getFilePermissionsSymlinks(String file)
	{
		RootTools.log("Checking permissions for " + file);
		File f = new File(file);
		if(f.exists())
		{
			RootTools.log(file + " was found.");
			try
			{

				InternalCommand command = new InternalCommand(
						InternalVariables.FPS, "ls -l " + file,
						"busybox ls -l " + file,
						"/system/bin/failsafe/toolbox ls -l " + file,
						"toolbox ls -l " + file);
				Shell.startRootShell().add(command);
				command.waitForFinish();

				return command.permissions;

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
	 * This method can be used to kill a running process
	 * 
	 * @param processName
	 *            name of process to kill
	 * @return <code>true</code> if process was found and killed successfully
	 */
	static boolean killProcess(final String processName)
	{
		RootTools.log("Killing process " + processName);

		boolean processKilled = false;
		try
		{
			Result result = new Result()
			{
				@Override
				public void process(String line) throws Exception
				{
					if(line.contains(processName))
					{
						Matcher psMatcher = InternalVariables.psPattern.matcher(line);

						try
						{
							if(psMatcher.find())
							{
								String pid = psMatcher.group(1);
								// concatenate to existing pids, to use later in
								// kill
								if(getData() != null)
								{
									setData(getData() + " " + pid);
								}
								else
								{
									setData(pid);
								}
								RootTools.log("Found pid: " + pid);
							}
							else
							{
								RootTools.log("Matching in ps command failed!");
							}
						}
						catch (Exception e)
						{
							RootTools.log("Error with regex!");
							e.printStackTrace();
						}
					}
				}

				@Override
				public void onFailure(Exception ex)
				{
					setError(1);
				}

				@Override
				public void onComplete(int diag)
				{
				}

				@Override
				public void processError(String arg0) throws Exception
				{
				}

			};
			RootTools.sendShell(new String[] { "ps" }, 1, result, -1);

			if(result.getError() == 0)
			{
				// get all pids in one string, created in process method
				String pids = (String) result.getData();

				// kill processes
				if(pids != null)
				{
					try
					{
						// example: kill -9 1234 1222 5343
						RootTools.sendShell(new String[] { "kill -9 " + pids }, 1, -1);
						processKilled = true;
					}
					catch (Exception e)
					{
						RootTools.log(e.getMessage());
					}
				}
			}
		}
		catch (Exception e)
		{
			RootTools.log(e.getMessage());
		}

		return processKilled;
	}

	static class InternalCommand extends CommandLog
	{
		Permissions permissions;

		public InternalCommand(int id, String... command)
		{
			super(id, command);
		}

		@Override
		public void output(int id, String line)
		{
			super.output(id, line);

			// getFilePermissionsSymlinks
			if(id == InternalVariables.FPS)
			{
				String symlink_final = "";

				String[] lineArray = line.split(" ");
				if(lineArray[0].length() != 10)
				{
					return;
				}

				RootTools.log("Line " + line);

				try
				{
					String[] symlink = line.split(" ");
					if(symlink[symlink.length - 2].equals("->"))
					{
						RootTools.log("Symlink found.");
						symlink_final = symlink[symlink.length - 1];
					}
				}
				catch (Exception e)
				{
				}

				try
				{
					permissions = new InternalMethods().getPermissions(line);
					if(permissions != null)
					{
						permissions.setSymlink(symlink_final);
					}
				}
				catch (Exception e)
				{
					RootTools.log(e.getMessage());
				}
			}
		}
	}
}
