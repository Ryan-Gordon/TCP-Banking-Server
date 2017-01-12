package ie.gmit.sw.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;
public class Requester{
	private Socket requestSocket;
	private ObjectOutputStream out;
 	private ObjectInputStream in;
 	private String message;
 	private String ipaddress;
 	private Scanner user_input;
 	private boolean authenticated;
	Requester()
	{
		user_input = new Scanner(System.in);
		ipaddress = "54.213.87.187";
		authenticated = false;
	}
	void run()
	{
		try{
			//1. creating a socket to connect to the server
			
			requestSocket = new Socket("localhost", 2004);
			System.out.println("Connected to "+ipaddress+" in port 2004");
			//2. get Input and Output streams
			out = new ObjectOutputStream(requestSocket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(requestSocket.getInputStream());
			//3: Communicating with the server
			do{
				try{
					//take initial message from server with connection status
					message = (String)in.readObject();
					System.out.println(message);
					//authenticated is a boolean which is changed to true when the client has authenticated with the server
					while(authenticated ==false){
					/*
					 * the user will be given the option to login or register an account with the bank
					 * until the user authenticates a login with the server these are the only operations offered by the server
					 */
						
					authenticated = startMenu();
					}
					//once the client has authenticated we present the main menu
					while(authenticated == true){
							System.out.println("User is authenticated");
							//show menu
							message = (String)in.readObject();
							System.out.println(message);
							
							message = (String)in.readObject();
							System.out.println(message);
							//choose and option and send this option to server
							message = user_input.next();
							sendMessage(message);
							
							System.out.println("Listening for response");
							//the response callback from the server
							//a callback is done for every operation to ensure the client is not abusing the code
							message = (String)in.readObject();
							System.out.println(message);
							//depending on the callback received from the server which is our task basically we will do some function
							switch(message){
							case "Change Customer Details":
								//we need 2 next Lines here to flush the buffer
								message = (String)in.readObject();
								System.out.println(message);
								user_input.nextLine();
								message = user_input.nextLine();
								sendMessage(message); //new Name
								
								recieveNRespond(); //new address
								recieveNRespond(); //new accnum
								break;
							case "Transactions":
								message = (String)in.readObject();
								System.out.println(message); //print last 10 transactions to the screen
								break;
							case "Withdraw":
								message = (String)in.readObject();
								System.out.println(message);
								message = (String)in.readObject();
								System.out.println(message);
								message = user_input.next();
								
								
								sendMessage(message); //attempt to withdraw from the server or be notified of insufficient funds
								
								break;
								
							case "Deposit":
								message = (String)in.readObject();
								System.out.println(message); //server prompts for amount to deposit
								message = user_input.next();
								sendMessage(message); //send deposit amount to server to attempt a deposit
								
								
								break;
								
							case "Quit!":
								System.out.println("Now closing connection");
								sendMessage("bye"); //this message tells the server, the client would like to end and closes the socket between them
								System.exit(0);
								break;
							default :
								
							}
							
					}
					
					
				}
				catch(ClassNotFoundException classNot){
					System.err.println("data received in unknown format");
				}
			}while(!message.equals("Quit!"));
		}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//4: Closing connection for good manners
			try{
				System.out.println("Now closing connection");
				in.close();
				out.close();
				requestSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	
	/**
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void recieveNRespond() throws IOException, ClassNotFoundException {
		message = (String)in.readObject();
		System.out.println(message);
		message = user_input.nextLine();
		sendMessage(message);
	}
	public static void main(String args[])
	{
		Requester client = new Requester();
		client.run();
	}
	/**
	 * An abstraction of the code needed to add a message to the out stream.
	 * 
	 * @param msg
	 */
	void sendMessage(String msg)
	{
		try{
			out.writeObject(msg);
			out.flush();
			System.out.println("client>" + msg);
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	/**
	 * The startMenu is the intial Log in menu.
	 * 
	 * The client is required to authenticate themselves with the server in order to see anything further.
	 * Clients have the option to register a new account however they must also login to this account after creation
	 * 
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	boolean startMenu() throws ClassNotFoundException, IOException{
		message = (String)in.readObject();
		System.out.println(message);
		message = user_input.next();
		sendMessage(message);
		
		System.out.println("Listening for response");
		
		message = (String)in.readObject();
		System.out.println(message);
		
		switch(message){
		case "Login":
			System.out.println("In Login");
			message = (String)in.readObject();
			System.out.println(message);
			user_input.nextLine();
			message = user_input.nextLine();
			sendMessage(message);
			recieveNRespond();
			
			
			message = (String)in.readObject();
			if(message.equals("Authenticated")){
				authenticated = true;
				System.out.println("Authentication = "+authenticated);
				return true;
			}
			else{
				System.out.println("Failed to authenticate");
			}
			
			break;
			
		case "Register":
			System.out.println("In register \n");
			registerUser();
			break;
			
		case "Quit!":
			System.out.println("Now closing connection");
			sendMessage("bye");
			System.exit(0);
			break;
		default :
		}
		return authenticated = false;
	}
	/**
	 * Register a new user with the service.
	 * 
	 * The user will choose details such as name, address ,uname and we will then print this to 2 files
	 * 2 Files : login.csv and userDetails.csv
	 * On a remote computer the client has no access to these files they are all server side.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void registerUser() throws IOException, ClassNotFoundException {
		//name
		message = (String)in.readObject();
		System.out.println(message);
		user_input.nextLine();
		message = user_input.nextLine();
		sendMessage(message);
		//address
		recieveNRespond();
		//accnum
		recieveNRespond();
		//username
		recieveNRespond();
		//password
		recieveNRespond();
		
		
		
		//confirmation message
		message = (String)in.readObject();
		System.out.println(message);
	}
	
}