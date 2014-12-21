package edu.miamioh.cse283.htw;

import java.io.*;
import java.net.*;

public class CaveSystemServerProxy {
	/** This socket is connected to the cave system server. */
	protected Socket s;

	/** Used to write to the cave's socket. */
	protected PrintWriter out;

	/** Constructor. */
	public CaveSystemServerProxy(Socket s) throws IOException {
		this.s = s;
		try {
			this.out = new PrintWriter(s.getOutputStream(), true);                   
		} catch(IOException ex) {
			try { s.close(); } catch(Exception ex2) { }
			throw ex;
		}
	}
	
	/** Register a cave server. */
	public void register(ServerSocket s) throws IOException {
		String msg = Protocol.REGISTER + " " + InetAddress.getLocalHost().getHostAddress() + " " + s.getLocalPort();
		out.println(msg);
	}
}
