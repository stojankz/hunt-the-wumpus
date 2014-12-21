package edu.miamioh.cse283.htw;

import java.io.*;

public class InputScanner {
	protected BufferedReader in; //!< Reader for System.in.
	
	//! Constructor.
	public InputScanner() {
		in = new BufferedReader(new InputStreamReader(System.in));
	}

	//! Returns true if there is data waiting to be read.
	public boolean ready() throws IOException {
		return in.ready();
	}
	
	//! Returns the next line that can be read.
	public String nextLine() throws IOException {
		return in.readLine().trim().toLowerCase();
	}
}
