package edu.miamioh.cse283.htw;

import java.util.*;

public class Room {
	
	/** Players currently in this room. */
	protected ArrayList<ClientProxy> players;

	/** Rooms that this room is connected to. */
	protected HashSet<Room> connected;

	/** ID number of this room. */
	protected int roomId;
	
	/** Whether this room has bats. */
	protected boolean hasBats;
	
	/** Whether this room has a Wumpus. */
	protected boolean hasWumpus;
	
	/** Whether this room has a pit. */
	protected boolean hasPit;
	
	/** Whether this room hasGold. */
	protected boolean hasGold;
	
	/**Whether this room has a ladder. */
	protected boolean hasLadder;
	
	protected boolean hasArrow;
	
	/** Constructor. */
	public Room() {
		players = new ArrayList<ClientProxy>();
		connected = new HashSet<Room>();
		hasBats = false;
		hasWumpus = false;
		hasPit = false;
		hasGold = false;
		hasLadder = false;
		hasArrow = false;
	}
	
	/** Set this room's id number. */
	public void setIdNumber(int n) {
		roomId = n;
	}

	/** Get this room's id number. */
	public int getIdNumber() {
		return roomId;
	}
	
	/** Connect room r to this room (bidirectional). */
	public void connectRoom(Room r) {
		connected.add(r);
		r.connected.add(this);
	}
	
	/** Called when a player enters this room. */
	public synchronized void enterRoom(ClientProxy c) {
		players.add(c);
	}
	
	/** Called when a player leaves this room. */
	public synchronized void leaveRoom(ClientProxy c) {
		players.remove(c);
	}

	/** Returns a connected Room (if room is valid), otherwise returns null. */
	public Room getRoom(int room) {
		for(Room r: connected) {
			if(r.getIdNumber() == room) {
				return r;
			}
		}
		return null;
	}
	
	public boolean isConnected(int room){
		for(Room r: connected){
			if(r.getIdNumber() == room){
				return true;
			}
		}
		return false;
	}
	
	/**Adds bats to this room. */
	public void addBats(){
		this.hasBats = true;
	}
	
	/**Adds a Wumpus to this room. */
	public void addWumpus(){
		this.hasWumpus = true;
	}
	
	/**Adds a pit to this room. */
	public void addPit(){
		this.hasPit = true;
	}
	
	/**Adds gold to this room. */
	public void addGold(){
		this.hasGold = true;
	}
	
	/**Adds a ladder to this room. */
	public void addLadder(){
		this.hasLadder = true;
	}
	
	public void addArrow(){
		this.hasArrow = true;
	}
	
//	public void shoot(Arrows arrow){
//		while(arrow.getHopsRemaining()> 0){
//			
//		}
//	}
	
	/** Returns a string describing what a player sees in this room. */
	public synchronized ArrayList<String> getSensed() {
		ArrayList<String> msg = new ArrayList<String>();
		
		if(hasBats){
			msg.add("You have encountered bats. The bats move you to another room.");
			return msg;
		}
		
		msg.add("You are in room " + this.getIdNumber());
		
		if(hasWumpus){
			msg.add("You are in the same room as a wumpus. The wumpus has killed you.");
			return msg;
		}
		if(hasPit){
			msg.add("You have fallen down a pit. You are dead.");
			return msg;
		}
		
		if(hasArrow){
			msg.add("You see an arrow.");
		}
		if(hasGold){
			msg.add("You see gold");
		}
		if(hasLadder){
			msg.add("You see a ladder.");
		}
		
		String t = "You see tunnels to rooms ";
		int c = 0;
		for(Room r : connected) {
			++c;
			if(c == connected.size()) {
				t = t.concat("and " + r.getIdNumber() + ".");
 			} else {
 				t = t.concat("" + r.getIdNumber() + ", ");
 			}
		}
		msg.add(t);
		
		for(Room r : connected){
			if(r.hasWumpus){
				msg.add("You smell something terrible.");
			}
		}
		
		for(Room r: connected){
			if(r.hasBats){
				msg.add("You hear a fluttering of wings.");
				break;
			}
		}
		
		for(Room r: connected){
			if(r.hasPit){
				msg.add("You feel a draft.");
				break;
			}
		}
		
		for(Room r: connected){
			if(r.players.size() > 0){
				msg.add("You hear another person in the caves.");
				break;
			}
		}
		return msg;
	}
}
