package edu.miamioh.cse283.htw;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;


/** Proxy client object. 
 * 
 */
public class ClientProxy {
	
	/** This socket is connected to a client. */
	protected Socket s;
	
	/** Used to read from the client's socket. */
	protected BufferedReader in;

	/** Used to write to the client's socket. */
	protected PrintWriter out;
	
	protected int arrows;
	protected int gold = 0;
	protected int points = 0;
	
	/** Constructor. */
	public ClientProxy(Socket s) throws IOException {
		this.s = s;
		try {
			this.out = new PrintWriter(s.getOutputStream(), true);                   
			this.in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch(IOException ex) {
			try { s.close(); } catch(IOException ex2) { }
			throw ex;
		}
		this.arrows = 5;
	}
	
	/** Close the connection to the client. */
	public void close() throws IOException {
		s.close();
	}
	
	/** Send a handoff message to this client. */
	public void handoff(InetAddress addr, int port) throws IOException {
		String msg = Protocol.HANDOFF + " " + addr.getHostName() + " " + port;
		out.println(msg);
	}
	
	/** Send a block message of notifications to the client. */
	public void sendNotifications(ArrayList<String> blockMsg) {
		out.println(Protocol.BEGIN_NOTIFICATION);
		for(String i : blockMsg) {
			out.println(i);
		}
		out.println(Protocol.END_NOTIFICATION);
	}
	
	/** Send a block message of sensory information to the client. */
	public void sendSenses(ArrayList<String> blockMsg) {
		out.println(Protocol.BEGIN_SENSES);
		for(String i : blockMsg) {
			out.println(i);
		}
		out.println("Gold: " + gold);
		out.println("Points: " + points);
		out.println(Protocol.END_SENSES);
	}
	
	/** Send a DIED message. */
	public void died() {
		out.println(Protocol.DIED);
	}
	
	/** Returns true if this client has data that can be read. */
	public boolean ready() throws IOException {
		return in.ready();
	}

	/** Get an action from the client. */
	public String nextLine() throws IOException {
		return in.readLine();
	}
}
