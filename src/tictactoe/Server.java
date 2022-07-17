package tictactoe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class Server implements Runnable{
	private String configPath = "./app.config";

	private int port;
	private ServerSocket serverSocket = null;
	private boolean running = false;
	private boolean started = false;
	private Thread serverThread = null;
	
	private ArrayList<PairOfHandlers> waitingHandlers = new ArrayList<>(); //list of pairs where one of handlers is null, therefore the client is waiting on opponent
	private static int numberOfMatchesPlayed = 0;
	
	public Server() {
		start();
	}
	
	public void start() {
		if(!started) {
			started = true;
			
			Properties prop = new Properties();
			
			try {
				prop.load(new FileInputStream(configPath));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
				
			port = Integer.parseInt(prop.getProperty("SERVER_PORT"));
			
			try {
				serverSocket = new ServerSocket(port);
				running = true;
				
				serverThread = new Thread(this);
				serverThread.start();
				
				System.out.println("Successful server creation");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
			
	}
	
	public void stop(){
        running = false;
        started = false;

        if(serverThread != null)
            serverThread.interrupt();
        serverThread = null;
    }
 
	@Override
	public void run() {
		while(running) {
			try {
				Socket client = serverSocket.accept();
				System.out.println("Client accepted");
				
				ClientHandler handler = new ClientHandler(client);
				if(!waitingHandlers.isEmpty()) {
					PairOfHandlers p = waitingHandlers.get(0);
					p.setHandler(handler);
					
					if(!p.missingHandler()) {
						startNewMatch();
					}
				}else {
					PairOfHandlers p = new PairOfHandlers();
					p.setHandler(handler);
					
					waitingHandlers.add(p);
				}				
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void startNewMatch() {
		PairOfHandlers p = waitingHandlers.get(0);
		ClientHandler handler1 = p.getHandlerOne();
		ClientHandler handler2 = p.getHandlerTwo();
		
		handler1.setOpponent(handler2);
		handler2.setOpponent(handler1);
		
		// Send "Accepted" message to both clients so they can start the game
		Message message = new Message("Accepted"); 
		
		handler1.sendMessage(message, false);
		handler2.sendMessage(message, false);
		handler1.sendMessage(new Message("X"), false);
		handler2.sendMessage(new Message("O"), false);
		
		waitingHandlers.remove(0);
		numberOfMatchesPlayed++;
	}

	private class PairOfHandlers{
		private ClientHandler handler1 = null;
		private ClientHandler handler2 = null;
		
		public ClientHandler getHandlerOne(){
			return handler1;
		}

		public ClientHandler getHandlerTwo(){
			return handler2;
		}
		
		public void setHandlerOne(ClientHandler c){
			handler1 = c;
		}

		public void setHandlerTwo(ClientHandler c){
			handler2 = c;
		}
		
		public void setHandler(ClientHandler c) {
			if(handler1 == null)setHandlerOne(c);
			else if(handler2 == null)setHandlerTwo(c);
		}
		
		public boolean missingHandler() {
			return handler1 == null || handler2 == null;
		}
	}
	
	public static void main(String[] args) {
		Server server = new Server();
	}
	
}
