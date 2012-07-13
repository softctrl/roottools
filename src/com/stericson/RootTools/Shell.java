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

/*
 *Special thanks to Jeremy Lakeman for the following code and for teaching me something new.
 *
 *Stephen
 */

package com.stericson.RootTools;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.SystemClock;

class Shell {
	private final Process proc;
	private final DataInputStream in;
	private final OutputStream out;
	private final List<Command> commands = new ArrayList<Command>();
	private boolean close = false;
	private static final String token = "F*D^W@#FGF";

	private static Shell rootShell;
	private static Shell shell;

	public static Shell startRootShell() throws IOException {
		if (rootShell == null) {
			String cmd = "/system/bin/su";
			if (!new File(cmd).exists()) {
				cmd = "/system/xbin/su";
				if (!new File(cmd).exists())
					throw new IOException("Unable to locate su binary");
			}
			// keep prompting the user until they accept, we hit 10 retries, or
			// the attempt fails quickly
			int retries = 0;
			while (rootShell == null) {
				long start = SystemClock.elapsedRealtime();
				try {
					rootShell = new Shell(cmd);
				} catch (IOException e) {
					long delay = SystemClock.elapsedRealtime() - start;
					if (delay < 500 || retries++ >= 10)
						throw e;
				}
			}
		}
		return rootShell;
	}

	public static Shell startShell() throws IOException {
		if (shell == null) {
			shell = new Shell("/system/bin/sh");
		}
		return shell;
	}

	public static void runRootCommand(Command command) throws IOException {
		startRootShell().add(command);
	}

	public static void runCommand(Command command) throws IOException {
		startShell().add(command);
	}

	public static void closeRootShell() throws IOException {
		if (rootShell == null)
			return;
		rootShell.close();
	}

	public static void closeShell() throws IOException {
		if (shell == null)
			return;
		shell.close();
	}

	public static void closeAll() throws IOException
	{
		closeShell();
		closeRootShell();
	}
	
	public Shell(String cmd) throws IOException {

		RootTools.log("Starting shell: " + cmd);

		proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		in = new DataInputStream(proc.getInputStream());
		out = proc.getOutputStream();

		out.write("echo Started\n".getBytes());
		out.flush();

		String line = in.readLine();
		if (!line.equals("Started")) {
			proc.destroy();
			throw new IOException("Unable to start shell " + line);
		}

		new Thread(input, "Shell Input").start();
		new Thread(output, "Shell Output").start();
	}

	private Runnable input = new Runnable() {
		public void run() {
			try {
				writeCommands();
			} catch (IOException e) {
				RootTools.log(e.getMessage(), 2, e);
			}
		}
	};

	private void writeCommands() throws IOException {
		try {
			int write = 0;
			while (true) {
				OutputStream out;
				synchronized (commands) {
					while (!close && write >= commands.size()) {
						commands.wait();
					}
					out = this.out;
				}
				if (write < commands.size()) {
					Command next = commands.get(write);
					next.writeCommand(out);
					String line = "\necho " + token + " " + write + " $?\n";
					out.write(line.getBytes());
					out.flush();
					write++;
				} else if (close) {
					out.write("\nexit 0\n".getBytes());
					out.flush();
					out.close();
					RootTools.log("Closing shell");
					return;
				}
			}
		} catch (InterruptedException e) {
			RootTools.log(e.getMessage(), 2, e);
		}
	}

	private Runnable output = new Runnable() {
		public void run() {
			try {
				readOutput();
			} catch (IOException e) {
				RootTools.log(e.getMessage(), 2, e);
			} catch (InterruptedException e) {
				RootTools.log(e.getMessage(), 2, e);
			}
		}
	};

	private void readOutput() throws IOException, InterruptedException {
		Command command = null;
		int read = 0;
		while (!close) {
			String line = in.readLine();
			if (line == null)
				break;

			// Log.v("Shell", "Out; \"" + line + "\"");
			if (command == null) {
				if (read >= commands.size())
					continue;
				command = commands.get(read);
			}

			int pos = line.indexOf(token);
			if (pos > 0)
				command.output(line.substring(0, pos));
			if (pos >= 0) {
				line = line.substring(pos);
				String fields[] = line.split(" ");
				int id = Integer.parseInt(fields[1]);
				if (id == read) {
					command.exitCode(Integer.parseInt(fields[2]));
					read++;
					command = null;
					continue;
				}
			}
			command.output(line);
		}
		RootTools.log("Read all output");
		proc.waitFor();
		proc.destroy();
		RootTools.log("Shell destroyed");
	}

	public void add(Command command) throws IOException {
		if (close)
			throw new IllegalStateException(
					"Unable to add commands to a closed shell");
		synchronized (commands) {
			commands.add(command);
			commands.notifyAll();
		}
	}

	public void close() throws IOException {
		if (this == rootShell)
			rootShell = null;
		if (this == shell)
			shell = null;
		synchronized (commands) {
			this.close = true;
			commands.notifyAll();
		}
	}

	public void waitFor() throws IOException, InterruptedException {
		close();
		if (commands.size() > 0)
			commands.get(commands.size() - 1).exitCode();
	}
}