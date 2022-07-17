package tictactoe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	
	private Thread runningThread = null;
	private boolean running = false;
	
	private ClientHandler opponent = null;
	
	public ClientHandler(Socket socket) {
		this.socket = socket;
		
		try {
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			
			runningThread = new Thread(this);
			runningThread.start();
			running = true;
		} catch (IOException e) {
			e.printStackTrace();
			disconnect();
		}
	}
	
	private void disconnect() {
		running = false;
		if(runningThread != null)runningThread.interrupt();
		
		try {
			reader.close();
		}catch(Exception e) {}
		reader = null;
		
		try {
			writer.close();
		}catch(Exception e) {}
		writer = null;
		
		try {
			socket.close();
		}catch(Exception e) {}
		socket = null;
	}
	
	public void sendMessage(Message message, boolean propagateToOpponent) {
		if(running) {
			try {
				outputStream.writeObject(message);
				outputStream.flush();
				if(propagateToOpponent)opponent.sendMessage(message, false);
			}
			catch(Exception e) {
				e.printStackTrace();
				disconnect();
			}
		}
	}
	
	public void sendMessageToOpponent(Message message) {
		if(running) {
			try {
				opponent.sendMessage(message, false);
			}
			catch(Exception e) {
				e.printStackTrace();
				disconnect();
			}
		}
	}
	
	public void run() {
		while(running) {
			try {
				String message;
				while((message = ((Message)inputStream.readObject()).getMessage()) != null && running) {
					System.out.println("Server: Recieved message : " + message);
					sendMessageToOpponent(new Message(message));
				}
			}catch(Exception e) {}
		}
	}
	
	public void setOpponent(ClientHandler c) {
		this.opponent = c;
	}
	
}