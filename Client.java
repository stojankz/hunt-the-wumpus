package edu.miamioh.cse283.htw;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client (player) for the Hunt the Wumpus game.
 * 
 * The Client class takes the following command-line parameters:
 * 
 * <Hostname of CaveSystemServer> <port number of CaveSystemServer>
 * 
 * E.g., "localhost 1234" 
 *
 */
public class Client {
	
	/** Proxy object that connects the client to its current cave. */
	protected CaveServerProxy cave;
	
	/** Boolean representing whether the player is alive or not. */
	protected boolean alive;
	

	/** Constructor. */
	public Client(CaveServerProxy cave) {
		this.cave = cave;
		this.alive = true;
		
	}
	
	/** Returns an array filled with lines read from the cave server until end is reached. 
	 * 
	 * WARNING: This is a blocking call (no user input available until this method returns).
	 * */
	public ArrayList<String> readUntil(String end) throws IOException {
		ArrayList<String> block = new ArrayList<String>();
		String line = cave.nextLine();
		while(!line.startsWith(end)) {
			block.add(line);
			line = cave.nextLine();
		}
		return block;
	}

	/** Plays the game.
	 * 
	 * @param args holds address and port number for the 
	 * CaveSystemServer this client will connect to.
	 */
	public void run() {
		try {
			InputScanner inputFromPlayer = new InputScanner();
			ArrayList<String> lastSenses = new ArrayList<String>();
			ArrayList<String> lastNotification = new ArrayList<String>();

			Pattern movePattern = Pattern.compile("^m(?:ove)?\\s+(\\w+)$");
			Pattern shootPattern = Pattern.compile("^s(?:hoot)?\\s+(\\w+)$");
			Pattern connectPattern = Pattern.compile("^co(?:nnect)?\\s+(\\w+)$");
			Pattern pickupPattern = Pattern.compile("^p(?:ickup)?$");
			Pattern quitPattern = Pattern.compile("^q(?:uit)?$");
			Pattern climbPattern = Pattern.compile("^cl(?:imb)?$");

			// loop while the player is still alive:
			while(alive) {
				// print the senses each time through this loop:
				// (this amounts to printing the senses everything something happens)
				for(String i : lastSenses) {
					System.out.println(i);
				}
				
				// however, notifications are printed only once; do that here:
				for(String i : lastNotification) {
					System.out.println(i);
				}
				lastNotification.clear();
				
				// poll for input from the player or cave:
				while (!cave.ready() && !inputFromPlayer.ready()) {
					try { Thread.sleep(50);	} catch (InterruptedException ex) {	}
				}

				// input from the player
				// parse that input, and send it to the server:
				if(inputFromPlayer.ready()) {
					String line = inputFromPlayer.nextLine().trim().toLowerCase();
					
					// process the player's input; usually we'll just send it to the 
					// cave server, except in the case of a quit message.
					// you're free to change how these commands work,
					// but be sure that the right messages get sent to the server!
					Matcher connect = connectPattern.matcher(line);
					Matcher move = movePattern.matcher(line);
					Matcher shoot = shootPattern.matcher(line);
					Matcher pickup = pickupPattern.matcher(line);
					Matcher quit = quitPattern.matcher(line);
					Matcher climb = climbPattern.matcher(line);

					if(connect.matches()) {
						cave.connect(connect.group(1));
						
					} else if(move.matches()) {
						cave.move(move.group(1));
						lastSenses.clear();
						
					} else if(shoot.matches()) {
						cave.shoot(shoot.group(1));
						lastSenses.clear();
						
					} else if(pickup.matches()) {
						cave.pickup();
						lastSenses.clear();
						
					} else if(quit.matches()) {
						alive = false;
						lastSenses.clear();
						
					} else if(climb.matches()) {
						cave.climb();
						lastSenses.clear();
						
					} else {
						// invalid input
						System.out.println("Invalid input");
						continue;
					}
				}
						
				// input from the server; parse it, figure out what to do:
				if(cave.ready()) {
					String line = cave.nextLine().trim();
					
					if(line.startsWith(Protocol.HANDOFF)) {
						// handoff; replace existing cave server by a new one:
						String[] words = line.split(" ");
						InetAddress addr = InetAddress.getByName(words[1]);
						int port = Integer.parseInt(words[2]);
						cave = cave.handoff(addr, port);
						lastSenses.clear();
						lastNotification.clear();
						
					} else if(line.startsWith(Protocol.BEGIN_NOTIFICATION)) {
						// read all available notification lines into the last notifications list:
						lastNotification = readUntil(Protocol.END_NOTIFICATION);

					} else if(line.startsWith(Protocol.DIED)) {
						// quit:
						alive = false;
						return;

					} else if(line.startsWith(Protocol.BEGIN_SENSES)) {
						// read all available sensory info into the last senses list:
						lastSenses = readUntil(Protocol.END_SENSES);
					}
				}
			} 
		} catch(IOException ex) {
			ex.printStackTrace();
		} finally {
			try { cave.close(); } catch(Exception ex) { }
		}
	}
	
	/** Main method for clients.
	 * 
	 * @param args contains the hostname and port number of the server that 
	 * this client should connect to.
	 */
	public static void main(String[] args) {
		try {
			InetAddress addr=InetAddress.getByName("localhost");
			int cavePortBase=1234;
		
			if(args.length > 0) {
				addr = InetAddress.getByName(args[0]);
				cavePortBase = Integer.parseInt(args[1]);
			}

			CaveServerProxy cave = new CaveServerProxy(new Socket(addr, cavePortBase));
			Client c = new Client(cave);
			c.run();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
