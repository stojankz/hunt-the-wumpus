package edu.miamioh.cse283.htw;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class CaveSystemServer {

	/** Port base. */
	protected int portBase;

	/** Socket for accepting connections from cave servers. */
	protected ServerSocket caveSocket;

	/** Socket for accepting connections from players. */
	protected ServerSocket clientSocket;

	/** Random number generator (used to pick caves for players). */
	protected Random rng;

	/** List of proxies to CaveServers. */
	protected ArrayList<CaveServerProxy> caves;

	/** Constructor. */
	public CaveSystemServer(int portBase) {
		this.portBase = portBase;
		this.rng = new Random();
		this.caves = new ArrayList<CaveServerProxy>();
	}

	/** Returns the port number to use for accepting client connections. */
	public int getClientPort() { return portBase; }

	/** Returns the port number to use for accepting cave server connections. */
	public int getCaveServerPort() { return portBase+1; }

	/** Adds a cave to this cave system. */
	public synchronized void addCave(CaveServerProxy c) {
		caves.add(c);
	}

	/** Removes a cave from this cave system. */
	public synchronized void removeCave(CaveServerProxy c) {
		caves.remove(c);
	}

	/** This thread manages a single CaveServer. */
	public class CaveServerThread implements Runnable {
		protected CaveServerProxy cave;
		
		/** Constructor. */
		public CaveServerThread(CaveServerProxy cave) {
			this.cave = cave;
		}
		
		/** Handle a single CaveServer. */
		public void run() {
			try {
				// we're expecting to receive a "REGISTER" message:
				String line = cave.nextLine();
				
				if(line.startsWith(Protocol.REGISTER)) {
					String[] words = line.split(" ");
					cave.setClientAddress(InetAddress.getByName(words[1]));
					cave.setClientPort(Integer.parseInt(words[2]));
					System.out.println(line);
					
					// if we reach here, address and port are good.  add this
					// cave server to the list:
					addCave(cave);
					
					// now put this thread to sleep until the connection is broken
					// (this throws an exception when the socket closes; there's a
					// bit of delay here, but acceptable for this project):
					while(true) { line = cave.nextLine(); }
				}

				// if the line didn't start with REGISTER, we can't really do
				// anything about it.  Fall through to the finally, and exit
				// the thread.
			} catch(Exception ex) {
			} finally {
				removeCave(cave);
			}
		}
	}
	
	/** This is the thread that accepts connections from CaveServers. 
	 *
	 * All it does is spawn new Threads for new connections from 
	 * CaveServers.
	 */
	public class CaveServerListenerThread implements Runnable {
		public void run() {
			try {
				while(true) {
					CaveServerProxy cave = new CaveServerProxy(caveSocket.accept());
					(new Thread(new CaveServerThread(cave))).start();
				}
			} catch(Exception ex) {
				// If an exception is thrown, we can't fix it here -- Crash,
				// because this is a pretty important feature.
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	/** Redirects a client to a random cave server. */
	public synchronized void handoff(ClientProxy client) throws IOException {
		CaveServerProxy c = caves.get(rng.nextInt(caves.size()));
		client.handoff(c.getClientAddress(), c.getClientPort());
	}

	/** This is the thread that handles a single client connection. 
	 */
	public class ClientThread implements Runnable {
		/** This is our "client" (actually, a proxy to the network-connected client). */
		protected ClientProxy client;

		/** Constructor. */
		public ClientThread(ClientProxy client) {
			this.client = client;
		}

		/** Handoff the player to a random CaveServer.
		 */
		public void run() {
			try {
				handoff(client);
			} catch(Exception ex) {
				// If an exception is thrown, we can't fix it here -- Crash.
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}

	/** Runs the CaveSystemServer.
	 * 
	 * @param args holds the port numbers to listen on.
	 */
	public void run() {
		try {
			// Construct both our sockets before we go multi-threaded:
			clientSocket = new ServerSocket(getClientPort());
			caveSocket = new ServerSocket(getCaveServerPort());

			// Start the thread listening for caves:
			(new Thread(new CaveServerListenerThread())).start();

			// and now loop forever, accepting client connections:
			while(true) {
				ClientProxy client = new ClientProxy(clientSocket.accept());
				(new Thread(new ClientThread(client))).start();
			}
		} catch(Exception ex) {
			// If an exception is caught, it's likely because of a network problem
			// somewhere.  We can't fix it here -- Crash.
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/** Main method (run the CaveSystemServer). */
	public static void main(String[] args) throws Exception {
		int cssPortBase=1234;
		
		if(args.length > 0) {
			cssPortBase = Integer.parseInt(args[0]);
		}

		CaveSystemServer css = new CaveSystemServer(cssPortBase);
		css.run();
	}
}
