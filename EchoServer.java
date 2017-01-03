package ie.gmit.sw;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
  boolean authenticated=false;
  /*
   * These are the user variables.
   * Started by using a Inner class however the varaible within were not thread safe
   * Local variables are thread safe however.
   */
  String name;
  String address;
  String accnum;
  String username;
  String password;
  

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
				
				//if (message.equals("bye"))
				while(authenticated == false){
					
						authenticated = loginMenu();
						
						if(message.equals("bye")){
							break;
						}
				
				}
				if (authenticated ==true){
					//presentCustomerMenu
					message = "bye";
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
  
  boolean loginMenu() throws ClassNotFoundException, IOException{
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
			if(login() == true){
				
				return true;
			}
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
			return false;
		}
		return false;
	}
  boolean login() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("Enter username:");
	  message = (String)in.readObject();
	  System.out.println(message);
	  username = message;
	  sendMessage("Password:");
	  message = (String)in.readObject();
	  System.out.println(message);
	  password = message;
	  
	  hashedPassword = getMD5(password);
	  
	  //open file 
	  
	  String line = "";
      String cvsSplitBy = ",";

      try (BufferedReader br = new BufferedReader(new FileReader("login.csv"))) {

          while ((line = br.readLine()) != null) {

              // use comma as separator
              String details[] = line.split(cvsSplitBy);
              if(details[0].equals(username)){
            	  System.out.println(details[1]);
            	  System.out.println(hashedPassword);
            	  if(details[1].equals(hashedPassword)){
            		  sendMessage("Authenticated");
            		  return true;
            	  }
              }

          }
          System.out.println("User not found");
          sendMessage("Login Failed");

      } catch (IOException e) {
          e.printStackTrace();
      }
	return false;
  }
  void register() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("First off what is your name?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  name = message;
 sendMessage("what is your address?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  address = message;
 sendMessage(" what is your account number?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  accnum = message;
	  sendMessage("Choose a username?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  username = message;
	  sendMessage("Choose password?");
	  
	  
	  message = (String)in.readObject();
	  System.out.println(message);
	  password = message;
	  
	  sendMessage("Account Created");
	  
	  FileWriter pw = new FileWriter("login.csv",true);
      StringBuilder sb = new StringBuilder();
      
      hashedPassword = getMD5(password);
      sb.append(username);
      sb.append(',');
      sb.append(hashedPassword);
      sb.append("\r\n");

      pw.write(sb.toString());
      pw.close();
	  
	  
  }
  private String getMD5(String str){
	  String generatedPassword = null;
	  try {
          // Create MessageDigest instance for MD5
          MessageDigest md = MessageDigest.getInstance("MD5");
          //Add password bytes to digest
          md.update(str.getBytes());
          //Get the hash's bytes 
          byte[] bytes = md.digest();
          //This bytes[] has bytes in decimal format;
          //Convert it to hexadecimal format
          StringBuilder sb2 = new StringBuilder();
          for(int i=0; i< bytes.length ;i++)
          {
              sb2.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
          }
          //Get complete hashed password in hex format
          generatedPassword = sb2.toString();
      } 
      catch (NoSuchAlgorithmException e) 
      {
          e.printStackTrace();
      }
	return generatedPassword;
  }
}
