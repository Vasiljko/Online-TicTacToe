package tictactoe;

import java.io.Serializable;

public class Message implements Serializable{
	private String s;
	
	public Message(String str) {
		s = str;
	}

	public String getMessage() {
		return s;
	}
}

