package tictactoe;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Client implements Runnable{
	private String configPath = "./app.config";
	
	private static Server 	server;
	private int 			port;
	
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintWriter writer = null;
	
	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	
	private Thread runningThread = null;
	private boolean running = false;
	
	private boolean accepted = false;
	
	private BufferedImage 	board;
	private BufferedImage 	redX;
	private BufferedImage 	redCircle;
	private BufferedImage 	blueX;
	private BufferedImage 	blueCircle;
	
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 527;
	private int lengthOfSpace = 160;
	private int firstSpot = -1;
	private int secondSpot = -1;
	
	private boolean myTurn = false;
	private boolean won = false;
	private boolean enemyWon = false;
	private boolean tie = false;
	
	private boolean unableToCommunicateWithOpponent = false;
	private int errors = 0;
	
	private Font font  = new Font("Verdana", Font.BOLD, 32);
	private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
	private Font largerFont = new Font("Verdana", Font.BOLD, 50);
	
	private String waitingString = "Waiting for another player";
	private String unableToCommunicateWithOpponentString = "Unable to communicate with opponent";
	private String wonString = "You won!";
	private String enemyWonString = "Oppoennt won!";
	private String tieString = "Game ended in a tie.";

	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };
	private String[] spaces = new String[9];
	
	private String playerSymbol;
	private Painter painter;
	
	public Client(){
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
			socket = new Socket("localhost", port);
			inputStream = new ObjectInputStream(socket.getInputStream());
			outputStream = new ObjectOutputStream(socket.getOutputStream());
		}catch(IOException e) {
			e.printStackTrace();
			disconnect();
		}
		
		loadImages();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));
			
		frame = new JFrame();
		frame.setTitle("Tic-Tac-Toe");
		frame.setContentPane(painter);
		frame.setSize(WIDTH, HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
		
		runningThread = new Thread(this);
		runningThread.start();
		running = true;
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
	
	private void incrementErrors() {
		errors++;
		if(errors >= 10)unableToCommunicateWithOpponent = true;
		if(unableToCommunicateWithOpponent)System.out.println("------------------- UNABLE TO COMMUNICATE ----------------------");
	}
	
	private void sendMessage(String message) {
		if(running) {
			try {
				outputStream.writeObject(new Message(message));
				outputStream.flush();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		while(running) {
			try{
				
				String message = ((Message)inputStream.readObject()).getMessage();
				if(message != null && !unableToCommunicateWithOpponent) {
					if(message.equals("Accepted")) {
						accepted = true;
					}else if(message.equals("X") || message.equals("O")) {
						playerSymbol = message;
						if(message.equals("X"))myTurn = true;
					}else {
						int position = Integer.parseInt(message);
						if(playerSymbol.equals("X"))spaces[position] = "O";
						else spaces[position] = "X";
						
						checkForEnemyWin();
						checkForTie();
						myTurn = true;
					}
				}

				painter.repaint();
			}catch(Exception e) {
			}
		}
	}
	
	private void loadImages() {
		try {
			board = ImageIO.read(new FileInputStream("../res/board.png"));
			redX = ImageIO.read(new FileInputStream("../res/redX.png"));
			redCircle = ImageIO.read(new FileInputStream("../res/redCircle.png"));
			blueX = ImageIO.read(new FileInputStream("../res/blueX.png"));
			blueCircle = ImageIO.read(new FileInputStream("../res/blueCircle.png"));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if(unableToCommunicateWithOpponent) {
			g.setColor(Color.RED);
			g.setFont(smallerFont);
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateWithOpponentString);
			g.drawString(unableToCommunicateWithOpponentString, WIDTH/2 - stringWidth/2, HEIGHT/2);
			return;
		}
		if(accepted){
			for(int i=0;i<spaces.length;i++) {
				if(spaces[i] != null && spaces[i].equals("X")) {
					if(playerSymbol.equals("O")) {
						g.drawImage(redX, (i % 3)*lengthOfSpace + 10 * (i%3), (int)(i/3)*lengthOfSpace + 10 * (int)(i/3), null);
					}else{
						g.drawImage(blueX, (i % 3)*lengthOfSpace + 10 * (i%3), (int)(i/3)*lengthOfSpace + 10 * (int)(i/3), null);
					}
				}else if(spaces[i] != null && spaces[i].equals("O")) {
					if(playerSymbol.equals("O")) {
						g.drawImage(blueCircle, (i % 3)*lengthOfSpace + 10 * (i%3), (int)(i/3)*lengthOfSpace + 10 * (int)(i/3), null);
					}else {
						g.drawImage(redCircle, (i % 3)*lengthOfSpace + 10 * (i%3), (int)(i/3)*lengthOfSpace + 10 * (int)(i/3), null);
					}
				}
			}
			
			if(won || enemyWon) {
				Graphics2D g2 = (Graphics2D)g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.BLACK);
				g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);
				
				g.setColor(Color.RED);
				g.setFont(largerFont);
				if(won) {
					int stringWidth = g2.getFontMetrics().stringWidth(wonString);
					g.drawString(wonString, WIDTH/2 - stringWidth/2, HEIGHT/2);
				}else {
					int stringWidth = g2.getFontMetrics().stringWidth(enemyWonString);
					g.drawString(enemyWonString, WIDTH/2 - stringWidth/2, HEIGHT/2); 
				}
			}
			else if (tie) {
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(largerFont);
				int stringWidth = g2.getFontMetrics().stringWidth(tieString);
				g.drawString(tieString, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
			}
		}else {
			g.setColor(Color.RED);
			g.setFont(font);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(waitingString);
			g.drawString(waitingString, WIDTH/2 - stringWidth/2, HEIGHT/2); 
		}
		
	}
	
	private void checkForWin() {
		for (int i = 0; i < wins.length; i++) {
			if (playerSymbol.equals("O")) {
				if (spaces[wins[i][0]] != null &&
					spaces[wins[i][1]] != null &&
					spaces[wins[i][2]] != null &&
					spaces[wins[i][0]].equals("O") && spaces[wins[i][1]].equals("O") && spaces[wins[i][2]].equals("O")) {
					
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			} else {
				if (spaces[wins[i][0]] != null &&
					spaces[wins[i][1]] != null &&
					spaces[wins[i][2]] != null &&
					spaces[wins[i][0]].equals("X") && spaces[wins[i][1]].equals("X") && spaces[wins[i][2]].equals("X")) {
					
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			}
		}
	}

	private void checkForEnemyWin() {
		for (int i = 0; i < wins.length; i++) {
			if (playerSymbol.equals("O")) {
				if (spaces[wins[i][0]] != null &&
					spaces[wins[i][1]] != null &&
					spaces[wins[i][2]] != null &&	
					spaces[wins[i][0]].equals("X") && spaces[wins[i][1]].equals("X") && spaces[wins[i][2]].equals("X")) {
					
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			} else {
				if (spaces[wins[i][0]] != null &&
					spaces[wins[i][1]] != null &&
					spaces[wins[i][2]] != null &&
					spaces[wins[i][0]].equals("O") && spaces[wins[i][1]].equals("O") && spaces[wins[i][2]].equals("O")) {
					
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			}
		}
		if(enemyWon) {
			System.out.println("Enemy won");
		}
	}
	
	private void checkForTie() {
		for (int i = 0; i < spaces.length; i++) {
			if (spaces[i] == null) {
				return;
			}
		}
		tie = true;
	}
	
	private class Painter extends JPanel implements MouseListener{
		private static final long serialVersionUID = 1L;
		
		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(accepted) {
				if(myTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
					int x = e.getX()/lengthOfSpace;
					int y = e.getY()/lengthOfSpace;
					y *= 3;
					int position = x+y;
					
					if(spaces[position] == null) {
						if(playerSymbol.equals("X"))spaces[position] = "X";
						else spaces[position] = "O";
						myTurn = false;
						
						repaint();
						
						
						
						
						Toolkit.getDefaultToolkit().sync();
						
						sendMessage(Integer.toString(position));
						
						
						System.out.println("DATA WAS SENT");
						checkForWin();
						checkForTie();
					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
		
	}
	
	
	
	public static void main(String[] args) {
		Client client = new Client();	
	}
}
