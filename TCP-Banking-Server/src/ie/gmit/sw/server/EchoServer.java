package ie.gmit.sw.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
/**
 * Inner class used for a customers account.
 * @author RyanGordon
 *
 * Contains functions for deposits and withdrawls
 */
  class Account {
 // Create a new lock
 private static Lock lock = new ReentrantLock();

 // Create a condition
 private static Condition newDeposit = lock.newCondition();

 private double balance = 0;

 public double getBalance() {
   return balance;
 }

 public boolean withdraw(double amount) {
   lock.lock(); //lock the lock
   System.out.println("Locked the account for "+Thread.currentThread().getName());
   try {
     if (balance < (amount-1000)){
    	 System.out.println("Insufficient funds even with credit limit");
    	 return false;
     }
     //if user has enough funds 
     else{
	   balance -= amount;
	   System.out.println("\t\t\tWithdraw " + amount +
	     "\t\t" + getBalance());
	   	return true;
	   
     }
   }
   finally {
     lock.unlock(); 
   }
 }//withdraw

 public boolean deposit(double amount) {
   lock.lock(); //lock the lock
   System.out.println("Locked the account for "+Thread.currentThread().getName());
   try {
     balance += amount;
     System.out.println("Deposit " + amount +
       "\t\t\t\t\t" + getBalance());
     
     // Signal thread 
     newDeposit.signalAll();
     return true;
   }
   finally {
	 System.out.println("Unlocked account for other threads");
     lock.unlock();
   }
 }//deposit

public void setBalance(double balance2) {
	this.balance = balance2;
}//used to update the user balance during transactions
}// end Account class

/**
 * Inner class Thread used to handle a client.
 * 
 * Each thread has its own set of variables as that is thread safe.
 * May abstract these vars into another class
 * @author college
 *
 */
class ClientServiceThread extends Thread {
  private Socket clientSocket;
  private String message;
  private int clientID = -1;
  private boolean running = true;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private boolean authenticated=false;
  /*
   * These are the user variables.
   * Started by using a Inner class however the varaible within were not thread safe
   * Local variables are thread safe however.
   * May try to change into a class again
   */
  private String name;
  private String address;
  private String accnum;
  private String username;
  private String password;
  private double balance;
  private Account account = new Account();
  

  ClientServiceThread(Socket s, int i) {
    clientSocket = s;
    clientID = i;
  }
//used to send a message string to the 'out' stream.
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
  /*
   * When running the thread we establish a handshake model for communication
   * Messages are sent, confirmed with a confirmation message and then performed.
   * The client has no access to some methods unless they are
   * 1. Authenticated with the server, presented with the option and confirmed that the operation can be performed
   * @see java.lang.Thread#run()
   */
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
				
				//show the login menu to the user until the successfully authenticate
				while(authenticated == false){
					authenticated = loginMenu();
					if(message.equals("bye")){
						break;
					}
				}
				//Check if the client has authenticated or not, if not they may have quit
				if (authenticated ==true){
					//presentCustomerMenu
					customerMenu();
				}
				
				
			}
			catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			
    	}while(!message.equals("bye"));
      
		System.out.println("Ending Client : ID - " + clientID + " : Address - "
		        + clientSocket.getInetAddress().getHostName());
		//close the client for good manners. Client will have already quit by now.
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /*
   * Customer menu is used after a client has authenticated with the server.
   * 
   * It presents a number of options for the user.
   * 1. Change the details of the logged in user on the server.
   * 2. View all transactions or just the last ten if there are more than that
   * 3. Attempt to deposit money into the users account and update the details file
   * 4. Attempt to withdraw money from the users account and update the details file
   * 5. Allow the client the option to quit
   */
  void customerMenu() throws ClassNotFoundException, IOException{
	  int choice;
	  String loginMenu = "\n Enter choice:  \n"
				+ "1. Change Customer Details  \n"
				+ "2. View Last 10 trasnactions  \n"
				+ "3. Deposit  \n"
				+ "4. Withdraw  \n"
				+ "5. Quit  \n";
		String starterMessage = " _____________________Acme ATM Company___________________\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|\tWelcome to Gordon Allied National Bank\t\t|\n"
				+ "|\t\t\t\t\t\t\t| \n"
				+ "|_______________________________________________________|"+loginMenu;
		sendMessage(starterMessage);
	    
		
		sendMessage("\n\n Your Balance is :"+String.format( "%.2f", account.getBalance() ));
		
		message = (String)in.readObject();
		choice = new Integer(message);
		
		switch(choice){
		case 1:
			//change customer details
			sendMessage("Change Customer Details");
			changeCustomerDetails(); // Prompt user for newdetails and change these on server side
			break;
		case 2: 
			sendMessage("Transactions");
			printTransactionLog(); // View all transactions or just the last ten if there are more than that
			
			break;
		case 3:
			sendMessage("Deposit");
			depositFunds(); //prompt for amount and attempt to do a deposit into users account
			
			break;
			
		case 4:
			sendMessage("Withdraw");
			withdrawFunds(); //prompt for amount and attempt to do a withdrawl from users account
			break;
		case 5:
			sendMessage("Quit!");
			message = (String)in.readObject(); //receive client confirmation of quit
			break;
			
		default:
			sendMessage("Invalid command");
			customerMenu(); //show menu again if invalid input
		}
  }
/**
 * Attempts to find and display all last transactions from the user.
 * 
 * Opens the transaction file, attempts to find transactions from the user using username as a query
 * Adds all transactions from the user to an ArrayList
 * 
 * If the array list ends up being larger than 10 we will change the arraylist
 * We change the arraylist into a 10 long subset of itself
 * The ten we take come from the end of the Array for the most recent ones 
 * @throws FileNotFoundException
 * @throws IOException
 */
private void printTransactionLog() throws FileNotFoundException, IOException {
	List<String> transactionList= new ArrayList<String>();
	
	  String line = "";
	  String cvsSplitBy = ",";
	  BufferedReader br = new BufferedReader(new FileReader("userTransactions.csv")); //the file containing all transaxtions 

	  try  {
		  
	      while ((line = br.readLine()) != null) {

	          // use comma as separator
	          String loginDetails[] = line.split(cvsSplitBy);
	          if(loginDetails[0].equals(username)){
	        		  transactionList.add("Transaction Type:"+loginDetails[5]+ " Transaction Amount :" +loginDetails[2]+"\t Previous Balance: "+ loginDetails[3]+"\t New Balance: "+ loginDetails[4] + "\n") ;
	          //Add the transactions to the list as a string describing each of the parameters
	          }
	          if(transactionList.size()>10){
	        	  System.out.print("Arraylist bigger than 10");
	        	  transactionList = transactionList.subList(transactionList.size()-10, transactionList.size()); //change the array into a subset of itself 
	          }
	          System.out.println(transactionList.toString());
	          //show the list to server, not really needed
	      }

	  } catch (IOException e) {
	      e.printStackTrace();
	  }
	  finally{
		  br.close();
		  sendMessage(transactionList.toString()); //send the list to the client
		  transactionList.clear(); //clear the list for next request incase there are more recent transactions 
	  }
}

/**
 * Allows the logged in user to change the details for their account.
 * 
 * Using this the client can change their name, address or account number.
 * The username is the unique identifier for the account and that is what cant be changed.
 * @throws IOException
 * @throws ClassNotFoundException
 * @throws FileNotFoundException
 */
private void changeCustomerDetails() throws IOException, ClassNotFoundException, FileNotFoundException {
	//Prompt for new name and save this to var
	sendMessage("Current Name: "+ name+"\n Enter new name: ");
	readMessage();
	name = message;
	
	//Prompt for new address and save this to var
	sendMessage("Current Address: "+ address+"\n Enter new address: ");
	readMessage();
	address = message;
	//Prompt for new accnum and save this to var
	sendMessage("Current Account Number: "+ accnum+"\n Enter new account number (We find your customer record via username): ");
	readMessage();
	accnum = message;

	try {
		File file = new File("userDetails.csv");
		// Creates a random access file stream to read from, and optionally to write to
		
	    FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
		FileLock lock = channel.tryLock();
		
	  	String line = "";
	    String cvsSplitBy = ",";
		BufferedReader detailsReader = new BufferedReader(new FileReader(file));
		
		  while ((line = detailsReader.readLine()) != null) {
			  String userDetails[] = line.split(cvsSplitBy);
			  if(userDetails[0].equals(username)){
				  removeLineFromFile("userDetails.csv", line); //a method used to find the previous details from the file and delete it
				  FileWriter detailsWriter = new FileWriter(file,true); //append means we just add the line to the end
				  //we have found the user in the system after login
				  System.out.println("Found user in system changing details");
				  userDetails[1]= name ;
				  userDetails[2] = address;
				  userDetails[3] = accnum;
				  
				  System.out.println("Changing details now");
				  StringBuilder sb = new StringBuilder(); //string builder used to prepare the line
			      
				  
				  //mtehod used to find the previous customer details and remove them 
				  
				  //We append the string to what a csv should look like
			      
			      sb.append(userDetails[0]);
			      sb.append(',');
			      sb.append(name);
			      sb.append(',');
			      sb.append(address);
			      sb.append(',');
			      sb.append(accnum);
			      sb.append(',');
			      sb.append(userDetails[4]);
			      sb.append("\r\n");
				  System.out.println(sb.toString());
				  
				  //print this string to the file and close the writer
				  detailsWriter.write(sb.toString());
				  detailsWriter.close();
				  break;
			  }
	}//while
		  detailsReader.close();
		  
		  lock.release(); //unlock the lock
		  channel.close();
	}//end try
	catch (OverlappingFileLockException e) {
	    // thrown when an attempt is made to acquire a lock on a a file that overlaps
	    // a region already locked by the sme JVM or when another thread is already
	    // waiting to lock an overlapping region of the same file
	    System.out.println("Overlapping File Lock Error: " + e.getMessage());
	}
}

/**
 * A function used to attempt to deposit to a ThreadSafe account object.
 * 
 * Prompts to user for deposit amount.
 * Server then changes the user balance and updates the details file to reflect this.
 * User is prompted with a status messages and a new balance.
 * 
 * @throws IOException
 * @throws ClassNotFoundException
 */
private void depositFunds() throws IOException, ClassNotFoundException {
	sendMessage("How much do you want to deposit?");
	
	double prevBalance = account.getBalance();
	boolean transactionSuccessful;
	
	readMessage();
	transactionSuccessful =  account.deposit(Double.parseDouble(message));
	
	//if the transaction is successful we want to log the transaction details to the file
	if(transactionSuccessful){
		logTransaction(prevBalance ,"userDetails.csv", "userTransactions.csv", "Deposit");
		  
		  
	}
}

/**
 * A function used to attempt to withdraw from a ThreadSafe account object.
 * 
 * Prompts to user for withdrawal amount.
 * Server then changes the user balance and updates the details file to reflect this.
 * User is prompted with a status messages and a new balance.
 * 
 * @throws IOException
 * @throws ClassNotFoundException
 */
private void withdrawFunds() throws IOException, ClassNotFoundException {
	sendMessage("How much do you want to Withdraw?");
	//the balance before any transaction occurs
	double prevBalance = account.getBalance();
	boolean transactionSuccessful;
	
	sendMessage("You have "+prevBalance+" balance and a 1000 credit limit");
	readMessage();
	// Method which returns 
	transactionSuccessful = account.withdraw(Double.parseDouble(message));
	
	//if the transaction is successful we want to log the transaction details to the file
	if(transactionSuccessful){
		logTransaction(prevBalance ,"userDetails.csv", "userTransactions.csv", "Withdrawl");
		  
	}
	
}

/**
 * Used to log a transaction (Deposit, withdraw) to a transaction file.
 *
 * 
 * @param prevBalance
 * @param userDetailsFile
 * @param transactionFile
 * @param transactionType
 * @throws IOException
 * @throws FileNotFoundException
 */
private void logTransaction(double prevBalance, String userDetailsFile, String transactionFile, String transactionType) throws IOException, FileNotFoundException {
	//log the transaction into the transaction file
	 FileWriter transWriter = new FileWriter(transactionFile,true);
	 StringBuilder detailsSB = new StringBuilder();
	  
	  detailsSB.append(username);
	  detailsSB.append(',');
	  detailsSB.append(accnum);
	  detailsSB.append(',');
	  detailsSB.append(Double.parseDouble(message));
	  detailsSB.append(',');
	  detailsSB.append(prevBalance);
	  detailsSB.append(',');
	  detailsSB.append(account.getBalance());
	  detailsSB.append(',');
	  detailsSB.append(transactionType);
	  detailsSB.append("\r\n");

	  transWriter.write(detailsSB.toString());
	  transWriter.close();
	  
	  try {
	  	File file = new File(userDetailsFile);
		// Creates a random access file stream to read from, and optionally to write to
		
	    FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
	  	FileLock lock = channel.tryLock();
	  	//open file \
		  	String line = "";
	      String cvsSplitBy = ",";
	  	BufferedReader detailsReader = new BufferedReader(new FileReader(file));
	  	
		  while ((line = detailsReader.readLine()) != null) {
			  String userDetails[] = line.split(cvsSplitBy);
			  if(userDetails[0].equals(username)){
				  removeLineFromFile("userDetails.csv", line);
				  FileWriter detailsWriter = new FileWriter(file,true);
				  //we have found the user in the system after login
				  userDetails[1]= name ;
				  userDetails[2] = address;
				  userDetails[3] = accnum;
				  StringBuilder sb = new StringBuilder();
			      
				  
				  //mtehod used to find the previous customer details and remove them 
				  
				  //TODO add a lock
			      
			      sb.append(userDetails[0]);
			      sb.append(',');
			      sb.append(name);
			      sb.append(',');
			      sb.append(address);
			      sb.append(',');
			      sb.append(accnum);
			      sb.append(',');
			      sb.append(account.getBalance());
			      sb.append("\r\n");
				  System.out.println(sb.toString());
				  detailsWriter.write(sb.toString());
				  detailsWriter.close();
				  break;
			  }//end if
		  }//end while
			  detailsReader.close();
			  
		  lock.release();
		  channel.close();
	  }//end try
	  catch (OverlappingFileLockException e) {

	      // thrown when an attempt is made to acquire a lock on a a file that overlaps
	      // a region already locked by the same JVM or when another thread is already
	      // waiting to lock an overlapping region of the same file
	      System.out.println("Overlapping File Lock Error: " + e.getMessage());
	  }
}

/**
 * The initial message menu we provide to the client.
 * 
 * Until the client authenticated with the server
 * We simply show them this menu to register or login.
 * 
 * @return
 * @throws ClassNotFoundException
 * @throws IOException
 */
  boolean loginMenu() throws ClassNotFoundException, IOException{
	  int choice;
		String loginMenu = "\n Enter choice:  \n"
				+ "1. Log-into the banking system  \n"
				+ "2. Register with the system  \n"
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
  
  
  
  /**
   * Used to check the login file and attempt to verify the login info provided.
   * 
   * Method opens the login file and searches for the provided username.
   * If the provided username is found we then check this line for the password.
   * 
   * The plain text password is never saved on the server, instead we save the MD5 hash of the password.
   * Using the provided password we generate a MD5 hash of this and check it with the hash on file.
   * 
   * @return
   * @throws ClassNotFoundException
   * @throws IOException
   */
  boolean login() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("Enter username:");
	  readMessage();
	  username = message;
	  sendMessage("Password:");
	  readMessage();
	  password = message;
	  
	  hashedPassword = getMD5(password);
	  
	  //open file 
	  
	  String line = "";
      String cvsSplitBy = ",";
      BufferedReader br = new BufferedReader(new FileReader("login.csv"));

      try  {
    	  
          while ((line = br.readLine()) != null) {

              // use comma as separator
              String loginDetails[] = line.split(cvsSplitBy);
              if(loginDetails[0].equals(username)){
            	  System.out.println(loginDetails[1]);
            	  System.out.println(hashedPassword);
            	  if(loginDetails[1].equals(hashedPassword)){
            		  BufferedReader detailsReader = new BufferedReader(new FileReader("userDetails.csv"));
            		  while ((line = detailsReader.readLine()) != null) {
            			  String userDetails[] = line.split(cvsSplitBy);
            			  if(userDetails[0].equals(username)){
            				  //we have found the user in the system after login
            				  
            				  name = userDetails[1];
            				  address = userDetails[2];
            				  accnum = userDetails[3];
            				  balance = Double.parseDouble(userDetails[4]);
            				  
            				  account.setBalance(balance);
            				  detailsReader.close();
            				  br.close();
            				  break;
            				  
            			  }
            		  }
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
      finally{
    	  br.close();
      }
	return false;
  }
  /**
   * Used to take details and register a new user.
   * 
   * Method prompts the user for account info
   * Once all info is provided the system logs this into the login file and returns the user to the customer menu
   * 
   * The plain text password is never saved on the server, instead we save the MD5 hash of the password.
   * 
   * 
   * @return
   * @throws ClassNotFoundException
   * @throws IOException
   */
  void register() throws ClassNotFoundException, IOException{
	  String hashedPassword = null;
	  sendMessage("First off what is your name?");
	  
	  
	  readMessage();
	  name = message;
	  sendMessage("what is your address?");
	  
	  readMessage();
	  address = message;
	  sendMessage(" what is your account number?");
	  
	  
	  readMessage();
	  accnum = message;
	  sendMessage("Choose a username?");
	  
	  
	  readMessage();
	  username = message;
	  sendMessage("Choose password?");
	  
	  
	  readMessage();
	  password = message;
	  
	  balance = (double)(Math.random() * 10) + 1;
	  
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
      
      /*
       * Now we will print the details of the customer to a seperate file
       * This is because I consider it unsafe to have all details in one file
       * 
       */
      FileWriter detailsWriter = new FileWriter("userDetails.csv",true);
      StringBuilder detailsSB = new StringBuilder();
      
      hashedPassword = getMD5(password);
      detailsSB.append(username);
      detailsSB.append(',');
      detailsSB.append(name);
      detailsSB.append(',');
      detailsSB.append(address);
      detailsSB.append(',');
      detailsSB.append(accnum);
      detailsSB.append(',');
      detailsSB.append(balance);
      detailsSB.append("\r\n");

      detailsWriter.write(detailsSB.toString());
      detailsWriter.close();
	  
  }
/**
 * @throws IOException
 * @throws ClassNotFoundException
 */
private void readMessage() throws IOException, ClassNotFoundException {
	message = (String)in.readObject();
	  System.out.println(message);
}
  
 
  /*
   * A utility method used to get the MD5 hash string of a string
   * 
   * getMD5()
   * Takes a @param of a string
   * Then gets the bytes for this string 
   * Generates an MD5 using the bytes and returns this as a string
   */
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
  /**
   * Used to query a file for a line and remove it.
   * 
   * @param file
   * @param lineToRemove
   * @throws IOException
   */
  private void removeLineFromFile(String file, String lineToRemove) throws IOException {

	    try {

	      File inFile = new File(file);

	      if (!inFile.isFile()) {
	        System.out.println("Parameter is not an existing file");
	        return;
	      }
	      //TODO tidy and comment the code
	      //Construct the new file that will later be renamed to the original filename.
	      File tempFile = new File(inFile.getAbsolutePath() + ".tmp");

	      BufferedReader br = new BufferedReader(new FileReader(file));
	      PrintWriter pw = new PrintWriter(new FileWriter(tempFile));

	      String line = null;

	      //Read from the original file and write to the new
	      //unless content matches data to be removed.
	      while ((line = br.readLine()) != null) {

	        if (!line.trim().equals(lineToRemove)) {
	        	System.out.println("Writing line to new file");
	          pw.println(line);
	          pw.flush();
	        }
	        else{
	        	System.out.println("Found line");
	        }
	      }
	      pw.close();
	      br.close();

	      //Delete the original file
	      if (!inFile.delete()) {
	        System.out.println("Could not delete file");
	        System.out.println(inFile.getAbsolutePath());
	        br.close();
	        inFile.delete();
	        return;
	      }
	    //Rename the new file to the filename the original file had.
	      if (!tempFile.renameTo(inFile))
	        System.out.println("Could not rename file");

	    }
	    catch (FileNotFoundException ex) {
	      ex.printStackTrace();
	    }
	    catch (IOException ex) {
	      ex.printStackTrace();
	    }
	    
	    finally{
	    	System.out.println("Removed entry from file");
	    }
  }
}// end ClientServiceThread class 
