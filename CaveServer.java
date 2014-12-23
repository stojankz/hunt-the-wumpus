package edu.miamioh.cse283.htw;

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * The CaveServer class takes the following command-line parameters:
 * 
 * <Hostname of CaveSystemServer> <port number of CaveSystemServer> <port number
 * of this CaveServer>
 * 
 * E.g., "localhost 1234 2000"
 */
public class CaveServer {

	/** Port base for this cave server. */
	protected int portBase;

	/** Socket for accepting connections from players. */
	protected ServerSocket clientSocket;

	/** Proxy to the CaveSystemServer. */
	protected CaveSystemServerProxy caveSystem;

	/** Random number generator (used to pick caves for players). */
	protected Random rng;

	/** Rooms in this CaveServer. */
	protected ArrayList<Room> rooms;

	/** Constructor. */
	public CaveServer(CaveSystemServerProxy caveSystem, int portBase) {
		this.caveSystem = caveSystem;
		this.portBase = portBase;
		this.rng = new Random();

		// construct the rooms:
		rooms = new ArrayList<Room>();
		for (int i = 0; i < 20; ++i) {
			rooms.add(new Room());
		}

		// connect them to each other:
		for (int i = 0; i < 20; ++i) {
			rooms.get(i).connectRoom(rooms.get((i + 1) % 20));
			rooms.get(i).connectRoom(rooms.get((i + 2) % 20));
		}

		// and give them random ids:
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 0; i < 20; ++i) {
			// int r = rng.nextInt(100);
			// while(ids.contains(r)) { r = rng.nextInt(100); }
			rooms.get(i).setIdNumber(i);
			rooms.get(i).addGold();
		}

		int r = rng.nextInt(20);
		rooms.get(r).addWumpus();

		int l = rng.nextInt(20);
		rooms.get(l).addLadder();

		for (int i = 0; i < 3; i++) {
			r = rng.nextInt(20);
			rooms.get(r).addBats();
		}

		for (int i = 0; i < 3; i++) {
			r = rng.nextInt(20);
			while (r == l) {
				r = rng.nextInt(20);
			}
			rooms.get(r).addPit();
		}
	}

	/** Returns the port number to use for accepting client connections. */
	public int getClientPort() {
		return portBase;
	}

	/** Returns an initial room for a client. */
	public synchronized Room getInitialRoom() {
		return rooms.get(rng.nextInt(rooms.size()));
	}

	/** This is the thread that handles a single client connection. */
	public class ClientThread implements Runnable {
		/**
		 * This is our "client" (actually, a proxy to the network-connected
		 * client).
		 */
		protected ClientProxy client;

		/** Notification messages. */
		protected ArrayList<String> notifications;

		/** Whether this player is alive. */
		protected boolean isAlive;

		/** Constructor. */
		public ClientThread(ClientProxy client) {
			this.client = client;
			this.notifications = new ArrayList<String>();
			this.isAlive = true;
		}

		/**
		 * Returns true if there are notifications that should be sent to this
		 * client.
		 */
		public synchronized boolean hasNotifications() {
			return !notifications.isEmpty();
		}

		/** Adds a message to the notifications. */
		public synchronized void addNotification(String msg) {
			notifications.add(msg);
		}

		/** Returns and resets notification messages. */
		public synchronized ArrayList<String> getNotifications() {
			ArrayList<String> t = notifications;
			notifications = new ArrayList<String>();
			return t;
		}

		/** Returns true if the player is alive. */
		public synchronized boolean isAlive() {
			return isAlive;
		}

		/** Kills this player. */
		public synchronized void kill() {
			isAlive = false;
		}

		/**
		 * Play the game with this client.
		 */
		public void run() {
			try {
				// the first time a player connects, send a welcome message:
				ArrayList<String> welcome = new ArrayList<String>();
				welcome.add("Welcome!");
				client.sendNotifications(welcome);

				// Put the player in an initial room and send them their initial
				// sensory information:
				Room r = getInitialRoom();
				r.enterRoom(client);
				client.sendSenses(r.getSensed());

				if (r.hasWumpus) {
					kill();
					r.leaveRoom(client);
					client.died();
				}

				if (r.hasPit) {
					kill();
					r.leaveRoom(client);
					client.died();
				}

				if (r.hasBats) {
					int roomNum = (int) Math.random() * 20;
					r.leaveRoom(client);
					r = rooms.get(roomNum);
					r.enterRoom(client);
					client.sendSenses(r.getSensed());
				}

				// while the player is alive, listen for commands from the
				// player
				// and for activities elsewhere in the cave:
				try {
					while (isAlive) {
						// poll, waiting for input from client or other
						// notifications:
						while (!client.ready() && !hasNotifications()
								&& isAlive()) {
							try {
								Thread.sleep(50);
							} catch (InterruptedException ex) {
							}
						}

						// if there are notifications, send them:
						if (hasNotifications()) {
							client.sendNotifications(getNotifications());
						}

						// if the player is dead, send the DIED message and
						// break:
						if (!isAlive()) {
							client.died();
							break;
						}

						// if the player did something, respond to it:
						if (client.ready()) {
							String line = client.nextLine().trim();

							if (line.startsWith(Protocol.MOVE_ACTION)) {
								// move the player: split out the room number,
								// move the player, etc.
								int roomNum = Integer.parseInt(line
										.substring(Protocol.MOVE_ACTION
												.length() + 1));
								// client has to leave the room:
								// r.leaveRoom(client)
								if (r.isConnected(roomNum) == true) {
									r.leaveRoom(client);
									r = r.getRoom(roomNum);
									// and enter the new room:
									// newRoom.enterRoom(client)
									r.enterRoom(client);
									// send the client new senses here:
									// client.sendSenses(r.getSensed());

									client.sendSenses(r.getSensed());

									if (r.hasWumpus) {
										kill();
										r.leaveRoom(client);
										client.died();
									}

									if (r.hasPit) {
										kill();
										r.leaveRoom(client);
										client.died();
									}

									while (r.hasBats) {
										roomNum = (int) (Math.random() * 20);
										r.leaveRoom(client);
										r = rooms.get(roomNum);
										r.enterRoom(client);
										client.sendSenses(r.getSensed());

										if (r.hasWumpus) {
											kill();
											r.leaveRoom(client);
											client.died();
										}

										if (r.hasPit) {
											kill();
											r.leaveRoom(client);
											client.died();
										}
									}
								} else {
									ArrayList<String> msg = new ArrayList<String>();
									msg.add("Invalid imput. This cave is not connected to room " + r.getIdNumber());
									client.sendNotifications(msg);
									client.sendSenses(r.getSensed());
								}

							} else if (line.startsWith(Protocol.SHOOT_ACTION)) {
								// shoot an arrow: split out the room number
								// into which the arrow
								// is to be shot, and then send an arrow into
								// the right series of
								// rooms.
								int roomNum = Integer.parseInt(line
										.substring(Protocol.SHOOT_ACTION
												.length() + 1));
								ArrayList<String> notify = new ArrayList<String>();
								if (r.isConnected(roomNum) == true) {
								if (client.arrows > 0) {
									Room s = r.getRoom(roomNum);
									if (s.hasWumpus) {
										notify.add("You killed the wumpus!");
										client.points += 200;
										s.hasWumpus = false;
										s.addArrow();
										int newRoom = (int) Math.random() * 20;
										while (newRoom == s.getIdNumber()
												|| newRoom == r.getIdNumber()) {
											newRoom = (int) Math.random() * 20;
										}
										Room m = rooms.get(newRoom);
										m.addWumpus();
										notify.add("You have " + client.arrows
												+ " arrows left.");
										client.sendNotifications(notify);
										client.sendSenses(r.getSensed());
										if (m == r) {
											kill();
											client.died();
										}
									} else {
										notify.add("You did not hit anything.");
										s.addArrow();
										// client.sendSenses(r.getSensed());
										notify.add("You have " + client.arrows
												+ " arrows left.");
										client.sendNotifications(notify);
										client.sendSenses(r.getSensed());
									}
									client.arrows--;
									
									// client.sendNotifications(notify);
									// client.sendSenses(r.getSensed());
								} else {
									notify.add("You have no more arrows.");
									client.sendNotifications(notify);
									client.sendSenses(r.getSensed());
								}
								}else{
									notify.add("You are not connected to room " + roomNum);
									client.sendNotifications(notify);
									client.sendSenses(r.getSensed());
								}
								
								// Arrows a = client.arrows.get(0);
								// r = rooms.get(roomNum);
								// a.move(r, client);

							} else if (line.startsWith(Protocol.PICKUP_ACTION)) {
								// pickup gold / arrows.
								ArrayList<String> notify = new ArrayList<String>();
								if (!r.hasGold && !r.hasArrow) {
									notify.add("Nothing to pick up.");
									client.sendNotifications(notify);
									client.sendSenses(r.getSensed());
								}
								if (r.hasGold) {
									client.gold += 20;
									r.hasGold = false;
									notify.add("You picked up gold!");
								}
								if (r.hasArrow) {
									client.arrows++;
									r.hasArrow = false;
									notify.add("You picked up an arrow!");
								}
								client.sendNotifications(notify);
								client.sendSenses(r.getSensed());

							} else if (line.startsWith(Protocol.CLIMB_ACTION)) {
								// climb the ladder, if the player is in a room
								// with a ladder.
								// send a notification telling the player his
								// score
								// and some kind of congratulations, and then
								// kill
								// the player to end the game -- call kill(),
								// above.
								ArrayList<String> notify = new ArrayList<String>();
								if (r.hasLadder) {
									notify.add("Congratulations, you made it out of the cave alive!");
									notify.add("Your score was: "
											+ client.points);
									notify.add("You gathered " + client.gold
											+ " pounds of gold.");
									client.sendNotifications(notify);
									kill();
									client.died();
								} else {
									notify.add("There is no ladder in this room.");
									client.sendNotifications(notify);
									client.sendSenses(r.getSensed());
								}

							} else if (line.startsWith(Protocol.QUIT)) {
								// no response: drop gold and arrows, and break.
								break;

							} else {
								// invalid response; send the client some kind
								// of error message
								// (as a notificiation).
								ArrayList<String> error = new ArrayList<String>();
								error.add("invalid response");
								client.sendNotifications(error);
							}
						}
					}
				} finally {
					// make sure the client leaves whichever room they're in,
					// and close the client's socket:
					// r.leaveRoom(client);
					// client.close();
				}
			} catch (Exception ex) {
				// If an exception is thrown, we can't fix it here -- Crash.
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}

	/** Runs the CaveSystemServer. */
	public void run() {
		try {
			// first thing we need to do is register this CaveServer
			// with the CaveSystemServer:
			clientSocket = new ServerSocket(getClientPort());
			caveSystem.register(clientSocket);
			System.out.println("CaveServer registered");

			// then, loop forever accepting Client connections:
			while (true) {
				ClientProxy client = new ClientProxy(clientSocket.accept());
				System.out.println("Client connected");
				(new Thread(new ClientThread(client))).start();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	/** Main method (run the CaveServer). */
	public static void main(String[] args) {
		try {
			InetAddress addr = InetAddress.getByName("localhost");
			int cssPortBase = 1234;
			int cavePortBase = 2000;

			if (args.length > 0) {
				addr = InetAddress.getByName(args[0]);
				cssPortBase = Integer.parseInt(args[1]);
				cavePortBase = Integer.parseInt(args[2]);
			}

			// first, we need our proxy object to the CaveSystemServer:
			CaveSystemServerProxy caveSystem = new CaveSystemServerProxy(
					new Socket(addr, cssPortBase + 1));

			// now construct this cave server, and run it:
			CaveServer cs = new CaveServer(caveSystem, cavePortBase);
			cs.run();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
}
