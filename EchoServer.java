package ie.gmit.sw;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
  public static void main(String[] args) throws Exception {
    ServerSocket m_ServerSocket = new ServerSocket(2004);
    int id = 0;
    while (true) {
      Socket clientSocket = m_ServerSocket.accept();
      ClientServiceThread cliThread = new ClientServiceThread(clientSocket, id++);
      cliThread.start();
    }
    
  }
}

class ClientServiceThread extends Thread {
  Socket clientSocket;
  String message;
  int clientID = -1;
  boolean running = true;
  ObjectOutputStream out;
  ObjectInputStream in;
  
  static class ClientDetails {
	   static String name;
	   static String address;
	   static String accnum;
	   static String username;
	   static String password;
  }

  ClientServiceThread(Socket s, int i) {
    clientSocket = s;
    clientID = i;
  }

  void sendMessage(String msg)
	{
		try{
			out.writeObject(msg);
			out.flush();
			System.out.println("server> " + msg);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
  public void run() {
    System.out.println("Accepted Client : ID - " + clientID + " : Address - "
        + clientSocket.getInetAddress().getHostName());
    try 
    {
    	out = new ObjectOutputStream(clientSocket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(clientSocket.getInputStream());
		System.out.println("Accepted Client : ID - " + clientID + " : Address - "
		        + clientSocket.getInetAddress().getHostName());
		
		sendMessage("Connection successful");
		do{
			try
			{
				
				System.out.println("client>"+clientID+"  "+ message);
				boolean authenticated =false;
				//if (message.equals("bye"))
				while(authenticated == false){
					
						loginMenu();
						
						if(message.equals("bye")){
							break;
						}
				
				}
				
				
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			
    	}while(!message.equals("bye"));
      
		System.out.println("Ending Client : ID - " + clientID + " : Address - "
		        + clientSocket.getInetAddress().getHostName());
		
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  void loginMenu() throws ClassNotFoundException, IOException{
	  int choice;
		String loginMenu = "\n Enter choice:  \n"
				+ "1. Register with the system  \n"
				+ "2. Log-into the banking system  \n"
				+ "3. Quit  \n";
		String starterMessage = " _____________________Acme ATM Company___________________\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|\tWelcome to Gordon Allied National Bank\t\t|\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|_______________________________________________________|"+loginMenu;
		sendMessage(starterMessage);
		
		
		
		message = (String)in.readObject();
		choice = new Integer(message);
		
		switch(choice){
		case 1:
			//do register
			sendMessage("Login");
			break;
		case 2: 
			//do login
			sendMessage("Register");
			register();
			break;
		case 3:
			sendMessage("Quit!");
			message = (String)in.readObject();
			break;
			
		default:
			sendMessage("Invalid command");
			loginMenu();
			
		}
	}
  void register() throws ClassNotFoundException, IOException{
	  sendMessage("First off what is your name?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  ClientDetails.name = message;
 sendMessage("what is your address?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  ClientDetails.address = message;
 sendMessage(" what is your account number?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  ClientDetails.accnum = message;
	  sendMessage("Choose a username?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  ClientDetails.username = message;
	  sendMessage("Choose password?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  ClientDetails.password = message;
	  
	  sendMessage("Account Created");
	  
	  
  }
}
